package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.AppLanguage
import com.example.data.local.ProxyDao
import com.example.data.local.ProxySettings
import com.example.data.local.SettingsDataStore
import com.example.data.local.SyncStateCode
import com.example.data.local.ThemeMode
import com.example.data.model.ProxyEntity
import com.example.data.network.AppRemoteStatus
import com.example.data.network.AppUpdateAndStatsFetcher
import com.example.data.network.MtprotoFetcher
import com.example.data.network.ProxyConnectivityTester
import kotlinx.coroutines.flow.Flow

data class SyncSummary(
    val fetchedCount: Int,
    val testedCount: Int,
    val workingCount: Int,
    val prunedDeadCount: Int,
    val activeProxy: ProxyEntity?,
    val switchedAutomatically: Boolean,
    val errorMessage: String? = null
)

class ProxyRepository(
    private val proxyDao: ProxyDao,
    private val settingsStore: SettingsDataStore,
    private val fetcher: MtprotoFetcher = MtprotoFetcher(),
    private val tester: ProxyConnectivityTester = ProxyConnectivityTester(),
    private val appRemoteFetcher: AppUpdateAndStatsFetcher = AppUpdateAndStatsFetcher()
) {
    val proxiesFlow: Flow<List<ProxyEntity>> = proxyDao.observeAllProxies()
    val settingsFlow: Flow<ProxySettings> = settingsStore.settingsFlow

    suspend fun syncAndVerify(
        maxTestCount: Int = 350,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): SyncSummary {
        val fetchResult = fetcher.fetchFromSoliSpirit()
        if (fetchResult.isFailure) {
            val message = fetchResult.exceptionOrNull()?.localizedMessage.orEmpty()
            val now = System.currentTimeMillis()
            val cached = proxyDao.getAllProxiesOnce()
            settingsStore.updateSyncMetadata(
                timestamp = now,
                stateCode = SyncStateCode.ERROR,
                workingCount = cached.count { it.isReachable },
                totalCount = cached.size,
                errorDetail = message
            )
            val active = resolveOrFailoverActiveProxy()
            return SyncSummary(
                fetchedCount = 0,
                testedCount = 0,
                workingCount = cached.count { it.isReachable },
                prunedDeadCount = 0,
                activeProxy = active,
                switchedAutomatically = false,
                errorMessage = message.ifBlank { "Network error" }
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

        // Automatically remove every unreachable/broken port & secret on every update
        val workingCount = results.count { it.isReachable }
        val prunedCount = proxyDao.deleteUnreachableProxies()

        val settings = settingsStore.getSnapshot()
        val previousActiveId = settings.activeProxyId
        val activeProxy = resolveOrFailoverActiveProxy()
        val switched = activeProxy != null && activeProxy.id != previousActiveId

        val stateCode = if (workingCount > 0) SyncStateCode.WORKING else SyncStateCode.NONE_WORKING
        settingsStore.updateSyncMetadata(
            timestamp = now,
            stateCode = stateCode,
            workingCount = workingCount,
            totalCount = freshProxies.size
        )

        checkForAppUpdate()

        return SyncSummary(
            fetchedCount = freshProxies.size,
            testedCount = results.size,
            workingCount = workingCount,
            prunedDeadCount = prunedCount,
            activeProxy = activeProxy,
            switchedAutomatically = switched
        )
    }

    suspend fun checkForAppUpdate(): AppRemoteStatus {
        val status = appRemoteFetcher.fetchAppUpdate(currentVersion = BuildConfig.VERSION_NAME)

        if (status.checkSucceeded) {
            settingsStore.updateAppRemoteInfo(
                latestVersion = if (status.isUpdateAvailable) status.latestVersion else null,
                updateUrl = if (status.isUpdateAvailable) status.downloadUrl else null,
                apkAssetUrl = if (status.isUpdateAvailable) status.apkAssetUrl else null,
                releaseNotes = if (status.isUpdateAvailable) status.releaseNotes else null,
                checkedAt = System.currentTimeMillis()
            )
        }
        return status
    }

    suspend fun pingAllExisting(
        maxTestCount: Int = 350,
        onProgress: (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<Int, Int> {
        val all = proxyDao.getAllProxiesOnce().take(maxTestCount)
        val results = tester.testBatch(all, onProgress = onProgress)
        val now = System.currentTimeMillis()
        results.forEach { m ->
            proxyDao.updateLatency(m.proxyId, m.latencyMs, m.isReachable, now)
        }
        // Automatically prune non-working ports/secrets
        val pruned = proxyDao.deleteUnreachableProxies()
        resolveOrFailoverActiveProxy()
        return results.count { it.isReachable } to pruned
    }

    suspend fun verifyActiveOrAutoSwitch(): Pair<ProxyEntity?, Boolean> {
        val settings = settingsStore.getSnapshot()
        val currentId = settings.activeProxyId
        val current = currentId?.let { proxyDao.getProxyById(it) }

        if (current != null) {
            val check = tester.testSingleProxy(current, timeoutMs = 3200)
            proxyDao.updateLatency(
                id = current.id,
                latencyMs = check.latencyMs,
                isReachable = check.isReachable,
                checkedAt = System.currentTimeMillis()
            )
            if (check.isReachable) {
                return proxyDao.getProxyById(current.id) to false
            } else {
                // Remove dead proxy automatically
                proxyDao.deleteUnreachableProxies()
            }
        }

        if (settings.autoFailoverEnabled || current == null) {
            val fastest = findBestVerifiedProxy()
            if (fastest != null) {
                settingsStore.setActiveProxyId(fastest.id)
                return fastest to (fastest.id != currentId)
            }
        }
        return null to false
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
        val candidates = proxyDao.getAllProxiesOnce().take(25)
        for (candidate in candidates) {
            val check = tester.testSingleProxy(candidate, timeoutMs = 2600)
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
        proxyDao.deleteUnreachableProxies()
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
    suspend fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)
    suspend fun setAppLanguage(language: AppLanguage) = settingsStore.setAppLanguage(language)
    suspend fun getSettingsSnapshot(): ProxySettings = settingsStore.getSnapshot()
}
