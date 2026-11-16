package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.TeleProxyApp
import com.example.data.local.ProxySettings
import com.example.data.model.ProxyEntity
import com.example.service.ProxyStabilityService
import com.example.util.QrCodeGenerator
import com.example.util.TelegramAppHelper
import com.example.util.TelegramClientInfo
import com.example.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProxyFilter {
    ALL, REACHABLE_ONLY, FAVORITES
}

data class VerificationReport(
    val isChecking: Boolean = false,
    val isVerifiedWorking: Boolean? = null,
    val latencyMs: Int? = null,
    val summaryText: String = "",
    val installedClients: List<TelegramClientInfo> = emptyList()
)

data class MainUiState(
    val proxies: List<ProxyEntity> = emptyList(),
    val filteredProxies: List<ProxyEntity> = emptyList(),
    val activeProxy: ProxyEntity? = null,
    val settings: ProxySettings = ProxySettings(),
    val searchQuery: String = "",
    val activeFilter: ProxyFilter = ProxyFilter.ALL,
    val isSyncing: Boolean = false,
    val isPingingAll: Boolean = false,
    val isCheckingAppUpdate: Boolean = false,
    val pingProgressText: String = "",
    val verificationReport: VerificationReport = VerificationReport(),
    val qrTargetProxy: ProxyEntity? = null,
    val qrPreviewBitmap: Bitmap? = null,
    val currentAppVersion: String = BuildConfig.VERSION_NAME
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class LaunchSafSave(val fileName: String) : UiEvent
    data object TelegramNotInstalledDialog : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TeleProxyApp
    private val repository = app.repository

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(ProxyFilter.ALL)
    private val _isSyncing = MutableStateFlow(false)
    private val _isPingingAll = MutableStateFlow(false)
    private val _isCheckingAppUpdate = MutableStateFlow(false)
    private val _pingProgressText = MutableStateFlow("")
    private val _verificationReport = MutableStateFlow(VerificationReport())
    private val _qrTargetProxy = MutableStateFlow<ProxyEntity?>(null)
    private val _qrPreviewBitmap = MutableStateFlow<Bitmap?>(null)

    private var pendingSafBitmap: Bitmap? = null

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.proxiesFlow,
        repository.settingsFlow,
        _searchQuery,
        _activeFilter,
        _isSyncing,
        _isPingingAll,
        _isCheckingAppUpdate,
        _pingProgressText,
        _verificationReport,
        _qrTargetProxy,
        _qrPreviewBitmap
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val proxies = values[0] as List<ProxyEntity>
        val settings = values[1] as ProxySettings
        val query = values[2] as String
        val filter = values[3] as ProxyFilter
        val isSyncing = values[4] as Boolean
        val isPinging = values[5] as Boolean
        val isCheckingUpdate = values[6] as Boolean
        val progressText = values[7] as String
        val verification = values[8] as VerificationReport
        val qrProxy = values[9] as ProxyEntity?
        val qrBitmap = values[10] as Bitmap?

        val activeProxy = proxies.firstOrNull { it.id == settings.activeProxyId }
            ?: proxies.firstOrNull { it.isReachable }
            ?: proxies.firstOrNull()

        val filtered = proxies.filter { proxy ->
            val matchesQuery = query.isBlank() ||
                proxy.server.contains(query, ignoreCase = true) ||
                proxy.port.toString().contains(query) ||
                proxy.secretType.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                ProxyFilter.ALL -> true
                ProxyFilter.REACHABLE_ONLY -> proxy.isReachable
                ProxyFilter.FAVORITES -> proxy.isFavorite
            }
            matchesQuery && matchesFilter
        }

        MainUiState(
            proxies = proxies,
            filteredProxies = filtered,
            activeProxy = activeProxy,
            settings = settings,
            searchQuery = query,
            activeFilter = filter,
            isSyncing = isSyncing,
            isPingingAll = isPinging,
            isCheckingAppUpdate = isCheckingUpdate,
            pingProgressText = progressText,
            verificationReport = verification,
            qrTargetProxy = qrProxy,
            qrPreviewBitmap = qrBitmap,
            currentAppVersion = BuildConfig.VERSION_NAME
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        refreshInstalledClients()
        autoSyncOnStartup()
    }

    private fun refreshInstalledClients() {
        val clients = TelegramAppHelper.detectInstalledTelegramClients(app)
        _verificationReport.value = _verificationReport.value.copy(installedClients = clients)
    }

    private fun autoSyncOnStartup() {
        viewModelScope.launch {
            checkAppUpdatesAndStats(manual = false)
            val settings = repository.getSettingsSnapshot()
            val current = uiState.value.proxies
            if (current.isEmpty() || System.currentTimeMillis() - settings.lastSyncTimestamp > 30 * 60 * 1000L) {
                syncNow()
            }
        }
    }

    fun checkAppUpdatesAndStats(manual: Boolean = true) {
        if (_isCheckingAppUpdate.value) return
        viewModelScope.launch {
            _isCheckingAppUpdate.value = true
            val status = repository.refreshAppUpdateAndUserStats()
            _isCheckingAppUpdate.value = false

            if (manual) {
                if (!status.checkSucceeded) {
                    _events.emit(
                        UiEvent.ShowMessage(status.errorMessage ?: "تعذر الاتصال بخادم التحديثات")
                    )
                } else if (status.isUpdateAvailable && status.latestVersion != null) {
                    _events.emit(
                        UiEvent.ShowMessage("يتوفر تحديث جديد للتطبيق: الإصدار ${status.latestVersion}")
                    )
                } else {
                    _events.emit(
                        UiEvent.ShowMessage("التطبيق محدث على آخر إصدار (${BuildConfig.VERSION_NAME})")
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ProxyFilter) {
        _activeFilter.value = filter
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _pingProgressText.value = "جارٍ جلب البروكسيات من الخادم السحابي..."
            val summary = repository.syncAndVerify(maxTestCount = 75) { checked, total ->
                _pingProgressText.value = "فحص السرعة ($checked / $total)"
            }
            _isSyncing.value = false
            _pingProgressText.value = ""

            if (summary.errorMessage != null) {
                _events.emit(UiEvent.ShowMessage("تعذر التحديث: ${summary.errorMessage}"))
            } else {
                _events.emit(
                    UiEvent.ShowMessage("تم التحديث: ${summary.workingCount} بروكسي شغال من أصل ${summary.fetchedCount}")
                )
            }
            refreshInstalledClients()
        }
    }

    fun pingAllProxies() {
        if (_isPingingAll.value || _isSyncing.value) return
        viewModelScope.launch {
            _isPingingAll.value = true
            val working = repository.pingAllExisting(maxTestCount = 90) { checked, total ->
                _pingProgressText.value = "فحص السرعة ($checked / $total)"
            }
            _isPingingAll.value = false
            _pingProgressText.value = ""
            _events.emit(UiEvent.ShowMessage("انتهى الفحص: $working بروكسي يعمل بنجاح"))
        }
    }

    fun verifyActiveConnection() {
        viewModelScope.launch {
            refreshInstalledClients()
            _verificationReport.value = _verificationReport.value.copy(
                isChecking = true,
                summaryText = "جارٍ التحقق الفعلي من السيرفر وتطبيق تلغرام..."
            )

            if (!TelegramAppHelper.hasNetworkConnection(app)) {
                _verificationReport.value = _verificationReport.value.copy(
                    isChecking = false,
                    isVerifiedWorking = false,
                    latencyMs = null,
                    summaryText = "لا يوجد اتصال بالإنترنت على الجهاز حالياً"
                )
                return@launch
            }

            val (proxy, switched) = repository.verifyActiveOrAutoSwitch()
            val clients = TelegramAppHelper.detectInstalledTelegramClients(app)

            if (proxy == null || !proxy.isReachable) {
                _verificationReport.value = VerificationReport(
                    isChecking = false,
                    isVerifiedWorking = false,
                    latencyMs = null,
                    summaryText = "البروكسي الحالي لا يستجيب. اضغط تحديث لجلب بروكسيات جديدة.",
                    installedClients = clients
                )
            } else {
                val clientNote = if (clients.isNotEmpty()) {
                    "تطبيق ${clients.first().displayName} جاهز"
                } else {
                    "لم يتم العثور على تطبيق تلغرام مثبّت"
                }
                val switchNote = if (switched) " (تم التبديل تلقائياً للأسرع)" else ""
                _verificationReport.value = VerificationReport(
                    isChecking = false,
                    isVerifiedWorking = true,
                    latencyMs = proxy.latencyMs,
                    summaryText = "البروكسي يستجيب فعلياً (${proxy.latencyMs} ms)$switchNote • $clientNote",
                    installedClients = clients
                )
            }
        }
    }

    fun connectTelegramWithProxy(proxy: ProxyEntity? = uiState.value.activeProxy) {
        viewModelScope.launch {
            if (proxy == null) {
                _events.emit(UiEvent.ShowMessage("لا يوجد بروكسي متاح حالياً، جارٍ التحديث..."))
                syncNow()
                return@launch
            }

            repository.setActiveProxy(proxy.id)
            _verificationReport.value = _verificationReport.value.copy(isChecking = true)
            val (verifiedProxy, switched) = repository.verifyActiveOrAutoSwitch()
            _verificationReport.value = _verificationReport.value.copy(isChecking = false)

            val target = verifiedProxy ?: proxy
            if (switched) {
                _events.emit(UiEvent.ShowMessage("تم التحويل لبروكسي أسرع تلقائياً (${target.server})"))
            }

            val opened = TelegramAppHelper.openProxyInTelegram(app, target)
            if (!opened) {
                _events.emit(UiEvent.TelegramNotInstalledDialog)
            }
        }
    }

    fun selectProxy(proxy: ProxyEntity) {
        viewModelScope.launch {
            repository.setActiveProxy(proxy.id)
            _events.emit(UiEvent.ShowMessage("تم تعيين ${proxy.server} كبروكسي نشط"))
        }
    }

    fun toggleFavorite(proxy: ProxyEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(proxy)
        }
    }

    fun copyProxyLink(proxy: ProxyEntity) {
        TelegramAppHelper.copyProxyLink(app, proxy)
        viewModelScope.launch {
            _events.emit(UiEvent.ShowMessage("تم نسخ رابط البروكسي إلى الحافظة"))
        }
    }

    fun shareProxyText(proxy: ProxyEntity) {
        TelegramAppHelper.shareProxyText(app, proxy)
    }

    fun openQrDialog(proxy: ProxyEntity) {
        _qrTargetProxy.value = proxy
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = QrCodeGenerator.generateQrBitmap(proxy.tmeLink, 720)
            _qrPreviewBitmap.value = bitmap
        }
    }

    fun closeQrDialog() {
        _qrTargetProxy.value = null
        _qrPreviewBitmap.value = null
    }

    fun downloadQrPoster(proxy: ProxyEntity) {
        viewModelScope.launch {
            val poster = withContext(Dispatchers.Default) {
                QrCodeGenerator.createShareablePosterBitmap(proxy)
            }
            val safeHost = proxy.server.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileName = "TeleProxy_${safeHost}_${proxy.port}.png"
            val result = QrCodeGenerator.saveQrToMediaStore(app, poster, fileName)
            if (result.isSuccess) {
                _events.emit(UiEvent.ShowMessage("تم تنزيل صورة الباركود في المعرض (Pictures/TeleProxy)"))
            } else if (result.exceptionOrNull() is UnsupportedOperationException) {
                pendingSafBitmap = poster
                _events.emit(UiEvent.LaunchSafSave(fileName))
            } else {
                _events.emit(UiEvent.ShowMessage("تعذر حفظ الصورة: ${result.exceptionOrNull()?.localizedMessage}"))
            }
        }
    }

    fun completeSafSave(uri: Uri?) {
        val bitmap = pendingSafBitmap ?: return
        pendingSafBitmap = null
        if (uri == null) return
        viewModelScope.launch {
            val res = QrCodeGenerator.writeBitmapToUri(app, uri, bitmap)
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage("تم حفظ صورة الباركود بنجاح"))
            } else {
                _events.emit(UiEvent.ShowMessage("فشل حفظ الصورة"))
            }
        }
    }

    fun shareQrPoster(proxy: ProxyEntity) {
        viewModelScope.launch {
            val poster = withContext(Dispatchers.Default) {
                QrCodeGenerator.createShareablePosterBitmap(proxy)
            }
            val safeHost = proxy.server.replace(Regex("[^A-Za-z0-9._-]"), "_")
            QrCodeGenerator.shareQrImage(app, poster, "TeleProxy_QR_${safeHost}.png")
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
            val interval = uiState.value.settings.syncIntervalMinutes
            WorkScheduler.schedulePeriodicSync(app, interval, enabled)
            _events.emit(
                UiEvent.ShowMessage(
                    if (enabled) "تم تفعيل التحديث التلقائي بالخلفية" else "تم إيقاف التحديث التلقائي"
                )
            )
        }
    }

    fun setSyncIntervalMinutes(minutes: Long) {
        viewModelScope.launch {
            repository.setSyncIntervalMinutes(minutes)
            val enabled = uiState.value.settings.autoSyncEnabled
            WorkScheduler.schedulePeriodicSync(app, minutes, enabled)
            _events.emit(UiEvent.ShowMessage("تم ضبط التحديث التلقائي كل $minutes دقيقة"))
        }
    }

    fun setAutoFailoverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoFailoverEnabled(enabled)
        }
    }

    fun setStabilityServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setStabilityServiceEnabled(enabled)
            if (enabled) {
                ProxyStabilityService.start(app)
                _events.emit(UiEvent.ShowMessage("تم تشغيل خدمة المراقبة المستمرة بالخلفية"))
            } else {
                ProxyStabilityService.stop(app)
                _events.emit(UiEvent.ShowMessage("تم إيقاف خدمة المراقبة بالخلفية"))
            }
        }
    }
}
