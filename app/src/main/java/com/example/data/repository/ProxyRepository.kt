package com.example.data.repository

import com.example.data.local.ProxyDao
import com.example.data.local.ProxySettings
import com.example.data.local.SettingsDataStore
import com.example.data.model.ProxyEntity
import com.example.data.network.MtprotoFetcher
import com.example.data.network.ProxyConnectivityTester
import kotlinx.coroutines.flow.Flow

data class SyncSummary(
    val fetchedCount: Int,
    val testedCount: Int,
    val workingCount: Int,
    val activeProxy: ProxyEntity?,
    val switchedAutomatically: Boolean,
    val errorMessage: String? = null
)

class ProxyRepository(
    private val proxyDao: ProxyDao,
    private val settingsStore: SettingsDataStore,
    private val fetcher: MtprotoFetcher = MtprotoFetcher(),
    private val tester: ProxyConnectivityTester = ProxyConnectivityTester()
) {
    val proxiesFlow: Flow<List<ProxyEntity>> = proxyDao.observeAllProxies()
    val settingsFlow: Flow<ProxySettings> = settingsStore.settingsFlow

    suspend fun syncAndVerify(
        maxTestCount: Int = 80,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): SyncSummary {
        val fetchResult = fetcher.fetchFromSoliSpirit()
        if (fetchResult.isFailure) {
            val message = fetchResult.exceptionOrNull()?.localizedMessage ?: "تعذر الاتصال بمصدر SoliSpirit"
            val now = System.currentTimeMillis()
            val cached = proxyDao.getAllProxiesOnce()
            settingsStore.updateSyncMetadata(now, "فشل التحديث: $message", cached.size)
            val active = resolveOrFailoverActiveProxy()
            return SyncSummary(
                fetchedCount = 0,
                testedCount = 0,
                workingCount = cached.count { it.isReachable },
                activeProxy = active,
                switchedAutomatically = false,
                errorMessage = message
            )
        }

        val freshProxies = fetchResult.getOrThrow().proxies
        proxyDao.mergeSyncedProxies(freshProxies)

        val allCurrent = proxyDao.getAllProxiesOnce()
        val toTest = allCurrent.take(maxTestCount)
        val results = tester.testBatch(toTest, onProgress = onProgress)
        val now = System.currentTimeMillis()

        results.forEach { measurement ->
            proxyDao.updateLatency(
                id = measurement.proxyId,
                latencyMs = measurement.latencyMs,
                isReachable = measurement.isReachable,
                checkedAt = now
            )
        }

        val workingCount = results.count { it.isReachable }
        val settings = settingsStore.getSnapshot()
        val previousActiveId = settings.activeProxyId
        val activeProxy = resolveOrFailoverActiveProxy()
        val switched = activeProxy != null && activeProxy.id != previousActiveId

        val statusMsg = if (workingCount > 0) {
            "تم التحديث بنجاح • $workingCount بروكسي شغال"
        } else {
            "تم جلب ${freshProxies.size} بروكسي (تحقق من اتصال الإنترنت)"
        }
        settingsStore.updateSyncMetadata(now, statusMsg, freshProxies.size)

        return SyncSummary(
            fetchedCount = freshProxies.size,
            testedCount = results.size,
            workingCount = workingCount,
            activeProxy = activeProxy,
            switchedAutomatically = switched
        )
    }

    suspend fun pingAllExisting(
        maxTestCount: Int = 100,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): Int {
        val all = proxyDao.getAllProxiesOnce().take(maxTestCount)
        val results = tester.testBatch(all, onProgress = onProgress)
        val now = System.currentTimeMillis()
        results.forEach { m ->
            proxyDao.updateLatency(m.proxyId, m.latencyMs, m.isReachable, now)
        }
        resolveOrFailoverActiveProxy()
        return results.count { it.isReachable }
    }

    suspend fun verifyActiveOrAutoSwitch(): Pair<ProxyEntity?, Boolean> {
        val settings = settingsStore.getSnapshot()
        val currentId = settings.activeProxyId
        val current = currentId?.let { proxyDao.getProxyById(it) }

        if (current != null) {
            val check = tester.testSingleProxy(current, timeoutMs = 3500)
            proxyDao.updateLatency(
                id = current.id,
                latencyMs = check.latencyMs,
                isReachable = check.isReachable,
                checkedAt = System.currentTimeMillis()
            )
            if (check.isReachable) {
                return proxyDao.getProxyById(current.id) to false
            }
        }

        if (settings.autoFailoverEnabled || current == null) {
            val fastest = findBestVerifiedProxy()
            if (fastest != null) {
                settingsStore.setActiveProxyId(fastest.id)
                return fastest to (fastest.id != currentId)
            }
        }
        return current to false
    }

    private suspend fun resolveOrFailoverActiveProxy(): ProxyEntity? {
        val settings = settingsStore.getSnapshot()
        val current = settings.activeProxyId?.let { proxyDao.getProxyById(it) }
        if (current != null && current.isReachable) {
            return current
        }
        if (settings.autoFailoverEnabled || current == null) {
            val fastest = proxyDao.getFastestReachableProxy()
            if (fastest != null) {
                settingsStore.setActiveProxyId(fastest.id)
                return fastest
            }
        }
        return current ?: proxyDao.getAllProxiesOnce().firstOrNull()
    }

    private suspend fun findBestVerifiedProxy(): ProxyEntity? {
        val candidates = proxyDao.getAllProxiesOnce().take(20)
        for (candidate in candidates) {
            val check = tester.testSingleProxy(candidate, timeoutMs = 2800)
            proxyDao.updateLatency(
                id = candidate.id,
                latencyMs = check.latencyMs,
                isReachable = check.isReachable,
                checkedAt = System.currentTimeMillis()
            )
            if (check.isReachable) {
                return proxyDao.getProxyById(candidate.id)
            }
        }
        return null
    }

    suspend fun setActiveProxy(proxyId: String) {
        settingsStore.setActiveProxyId(proxyId)
    }

    suspend fun toggleFavorite(proxy: ProxyEntity) {
        proxyDao.setFavorite(proxy.id, !proxy.isFavorite)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) = settingsStore.setAutoSyncEnabled(enabled)
    suspend fun setSyncIntervalMinutes(minutes: Long) = settingsStore.setSyncIntervalMinutes(minutes)
    suspend fun setAutoFailoverEnabled(enabled: Boolean) = settingsStore.setAutoFailoverEnabled(enabled)
    suspend fun setStabilityServiceEnabled(enabled: Boolean) = settingsStore.setStabilityServiceEnabled(enabled)
    suspend fun getSettingsSnapshot(): ProxySettings = settingsStore.getSnapshot()
}
