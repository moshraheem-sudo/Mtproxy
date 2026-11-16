package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.TeleProxyApp
import com.example.data.local.AppLanguage
import com.example.data.local.ProxySettings
import com.example.data.local.ThemeMode
import com.example.data.model.ProxyEntity
import com.example.security.SecurityGuard
import com.example.security.SecurityStatus
import com.example.service.ProxyStabilityService
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.stringsFor
import com.example.util.AppDownloadState
import com.example.util.AppUpdateDownloader
import com.example.util.QrCodeGenerator
import com.example.util.TelegramAppHelper
import com.example.util.TelegramClientInfo
import com.example.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.io.File

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
    val activeFilter: ProxyFilter = ProxyFilter.ALL,
    val isSyncing: Boolean = false,
    val isPingingAll: Boolean = false,
    val isCheckingAppUpdate: Boolean = false,
    val pingProgressText: String = "",
    val verificationReport: VerificationReport = VerificationReport(),
    val qrTargetProxy: ProxyEntity? = null,
    val qrPreviewBitmap: Bitmap? = null,
    val currentAppVersion: String = BuildConfig.VERSION_NAME,
    val appDownloadState: AppDownloadState = AppDownloadState.Idle,
    val securityStatus: SecurityStatus = SecurityStatus()
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class LaunchSafSave(val fileName: String) : UiEvent
    data object TelegramNotInstalledDialog : UiEvent
    data object RequestInstallPermissionDialog : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TeleProxyApp
    private val repository = app.repository

    private val _activeFilter = MutableStateFlow(ProxyFilter.ALL)
    private val _isSyncing = MutableStateFlow(false)
    private val _isPingingAll = MutableStateFlow(false)
    private val _isCheckingAppUpdate = MutableStateFlow(false)
    private val _pingProgressText = MutableStateFlow("")
    private val _verificationReport = MutableStateFlow(VerificationReport())
    private val _qrTargetProxy = MutableStateFlow<ProxyEntity?>(null)
    private val _qrPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    private val _appDownloadState = MutableStateFlow<AppDownloadState>(AppDownloadState.Idle)
    private val _securityStatus = MutableStateFlow(SecurityGuard.inspectAppIntegrity(app))

    private var pendingSafBitmap: Bitmap? = null
    private var pendingInstallFile: File? = null
    private var downloadJob: Job? = null

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.proxiesFlow,
        repository.settingsFlow,
        _activeFilter,
        _isSyncing,
        _isPingingAll,
        _isCheckingAppUpdate,
        _pingProgressText,
        _verificationReport,
        _qrTargetProxy,
        _qrPreviewBitmap,
        _appDownloadState,
        _securityStatus
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val proxies = values[0] as List<ProxyEntity>
        val settings = values[1] as ProxySettings
        val filter = values[2] as ProxyFilter
        val isSyncing = values[3] as Boolean
        val isPinging = values[4] as Boolean
        val isCheckingUpdate = values[5] as Boolean
        val progressText = values[6] as String
        val verification = values[7] as VerificationReport
        val qrProxy = values[8] as ProxyEntity?
        val qrBitmap = values[9] as Bitmap?
        val downloadState = values[10] as AppDownloadState
        val secStatus = values[11] as SecurityStatus

        val activeProxy = proxies.firstOrNull { it.id == settings.activeProxyId }
            ?: proxies.firstOrNull { it.isReachable }
            ?: proxies.firstOrNull()

        val filtered = proxies.filter { proxy ->
            when (filter) {
                ProxyFilter.ALL -> true
                ProxyFilter.REACHABLE_ONLY -> proxy.isReachable
                ProxyFilter.FAVORITES -> proxy.isFavorite
            }
        }

        MainUiState(
            proxies = proxies,
            filteredProxies = filtered,
            activeProxy = activeProxy,
            settings = settings,
            activeFilter = filter,
            isSyncing = isSyncing,
            isPingingAll = isPinging,
            isCheckingAppUpdate = isCheckingUpdate,
            pingProgressText = progressText,
            verificationReport = verification,
            qrTargetProxy = qrProxy,
            qrPreviewBitmap = qrBitmap,
            currentAppVersion = BuildConfig.VERSION_NAME,
            appDownloadState = downloadState,
            securityStatus = secStatus
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

    private suspend fun activeStrings(): AppStrings {
        val lang = repository.getSettingsSnapshot().appLanguage
        return stringsFor(lang)
    }

    private fun refreshInstalledClients() {
        val clients = TelegramAppHelper.detectInstalledTelegramClients(app)
        _verificationReport.value = _verificationReport.value.copy(installedClients = clients)
    }

    private fun autoSyncOnStartup() {
        viewModelScope.launch {
            checkForAppUpdates(manual = false)
            val settings = repository.getSettingsSnapshot()
            val current = uiState.value.proxies
            if (current.isEmpty() || System.currentTimeMillis() - settings.lastSyncTimestamp > 30 * 60 * 1000L) {
                syncNow()
            }
        }
    }

    fun checkForAppUpdates(manual: Boolean = true) {
        if (_isCheckingAppUpdate.value) return
        viewModelScope.launch {
            _isCheckingAppUpdate.value = true
            val status = repository.checkForAppUpdate()
            _isCheckingAppUpdate.value = false

            if (manual) {
                val strings = activeStrings()
                if (!status.checkSucceeded) {
                    _events.emit(
                        UiEvent.ShowMessage(status.errorMessage ?: strings.updateServerErrorToast)
                    )
                } else if (status.isUpdateAvailable && status.latestVersion != null) {
                    _events.emit(
                        UiEvent.ShowMessage(strings.nextVersionAvailable(status.latestVersion))
                    )
                } else {
                    _events.emit(
                        UiEvent.ShowMessage("${strings.currentVersionLabel}: v${BuildConfig.VERSION_NAME}")
                    )
                }
            }
        }
    }

    fun startInAppUpdateDownload() {
        val settings = uiState.value.settings
        val version = settings.latestAvailableVersion ?: return
        val apkUrl = settings.latestApkAssetUrl
        val currentLangStrings = stringsFor(settings.appLanguage)

        if (apkUrl.isNullOrBlank()) {
            val fallback = settings.latestUpdateUrl
            if (!fallback.isNullOrBlank() && SecurityGuard.isTrustedHttpsUrl(fallback)) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallback)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
            } else {
                viewModelScope.launch {
                    _events.emit(UiEvent.ShowMessage(currentLangStrings.noDirectApkToast))
                }
            }
            return
        }

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val strings = activeStrings()
            try {
                AppUpdateDownloader.downloadApk(app, apkUrl, version).collect { state ->
                    _appDownloadState.value = state
                    if (state is AppDownloadState.ReadyToInstall) {
                        _events.emit(UiEvent.ShowMessage(strings.downloadReadyToast(state.totalMb)))
                    } else if (state is AppDownloadState.Error) {
                        _events.emit(
                            UiEvent.ShowMessage(strings.formatDownloadError(state.reasonKey, state.detail))
                        )
                    }
                }
            } catch (t: Throwable) {
                _appDownloadState.value = AppDownloadState.Error("generic", t.localizedMessage.orEmpty())
            }
        }
    }

    fun cancelInAppDownload() {
        downloadJob?.cancel()
        _appDownloadState.value = AppDownloadState.Idle
    }

    fun installDownloadedUpdate() {
        val strings = stringsFor(uiState.value.settings.appLanguage)
        val ready = _appDownloadState.value as? AppDownloadState.ReadyToInstall ?: return
        if (!ready.apkFile.exists()) {
            _appDownloadState.value = AppDownloadState.Idle
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage(strings.redownloadUpdateToast))
            }
            return
        }

        if (!AppUpdateDownloader.canInstallPackages(app)) {
            pendingInstallFile = ready.apkFile
            viewModelScope.launch {
                _events.emit(UiEvent.RequestInstallPermissionDialog)
            }
            return
        }

        val launched = AppUpdateDownloader.launchApkInstaller(app, ready.apkFile)
        if (!launched) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage(strings.installerErrorToast))
            }
        }
    }

    fun openSystemInstallPermission() {
        AppUpdateDownloader.openInstallPermissionSettings(app)
    }

    fun retryPendingInstallOnResume() {
        val file = pendingInstallFile ?: return
        if (AppUpdateDownloader.canInstallPackages(app) && file.exists()) {
            pendingInstallFile = null
            AppUpdateDownloader.launchApkInstaller(app, file)
        }
    }

    fun setFilter(filter: ProxyFilter) {
        _activeFilter.value = filter
    }

    fun syncNow() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            val strings = activeStrings()
            _isSyncing.value = true
            _pingProgressText.value = strings.fetchingEndpointsMsg
            val summary = repository.syncAndVerify(maxTestCount = 350) { checked, total ->
                _pingProgressText.value = strings.pingingProgressMsg(checked, total)
            }
            _isSyncing.value = false
            _pingProgressText.value = ""

            if (summary.errorMessage != null) {
                _events.emit(UiEvent.ShowMessage(summary.errorMessage))
            } else {
                _events.emit(
                    UiEvent.ShowMessage(
                        strings.syncCompleteToast(summary.workingCount, summary.prunedDeadCount)
                    )
                )
            }
            refreshInstalledClients()
        }
    }

    fun pingAllProxies() {
        if (_isPingingAll.value || _isSyncing.value) return
        viewModelScope.launch {
            val strings = activeStrings()
            _isPingingAll.value = true
            val (working, pruned) = repository.pingAllExisting(maxTestCount = 350) { checked, total ->
                _pingProgressText.value = strings.pingingProgressMsg(checked, total)
            }
            _isPingingAll.value = false
            _pingProgressText.value = ""
            _events.emit(UiEvent.ShowMessage(strings.syncCompleteToast(working, pruned)))
        }
    }

    fun verifyActiveConnection() {
        viewModelScope.launch {
            val strings = activeStrings()
            refreshInstalledClients()
            _verificationReport.value = _verificationReport.value.copy(
                isChecking = true,
                summaryText = strings.verifyCheckingMsg
            )

            if (!TelegramAppHelper.hasNetworkConnection(app)) {
                _verificationReport.value = _verificationReport.value.copy(
                    isChecking = false,
                    isVerifiedWorking = false,
                    latencyMs = null,
                    summaryText = strings.verifyNoInternetMsg
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
                    summaryText = strings.verifyDeadMsg,
                    installedClients = clients
                )
            } else {
                val clientNote = if (clients.isNotEmpty()) {
                    val first = clients.first()
                    val clientLabel = if (first.isOfficial && first.packageName == "org.telegram.messenger") {
                        strings.officialTelegramName
                    } else {
                        first.displayName
                    }
                    strings.clientReadyText(clientLabel)
                } else {
                    strings.clientMissingText
                }
                _verificationReport.value = VerificationReport(
                    isChecking = false,
                    isVerifiedWorking = true,
                    latencyMs = proxy.latencyMs,
                    summaryText = strings.verifyWorkingMsg(proxy.latencyMs, switched, clientNote),
                    installedClients = clients
                )
            }
        }
    }

    fun connectTelegramWithProxy(proxy: ProxyEntity? = uiState.value.activeProxy) {
        viewModelScope.launch {
            val strings = activeStrings()
            if (proxy == null) {
                _events.emit(UiEvent.ShowMessage(strings.noProxyRefreshToast))
                syncNow()
                return@launch
            }

            repository.setActiveProxy(proxy.id)
            _verificationReport.value = _verificationReport.value.copy(isChecking = true)
            val (verifiedProxy, switched) = repository.verifyActiveOrAutoSwitch()
            _verificationReport.value = _verificationReport.value.copy(isChecking = false)

            val target = verifiedProxy ?: proxy
            if (switched) {
                _events.emit(UiEvent.ShowMessage(strings.switchedFastestToast(target.server)))
            }

            val opened = TelegramAppHelper.openProxyInTelegram(app, target)
            if (!opened) {
                _events.emit(UiEvent.TelegramNotInstalledDialog)
            }
        }
    }

    fun selectProxy(proxy: ProxyEntity) {
        viewModelScope.launch {
            val strings = activeStrings()
            repository.setActiveProxy(proxy.id)
            _events.emit(UiEvent.ShowMessage(strings.selectedEndpointToast(proxy.server)))
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
            val strings = activeStrings()
            _events.emit(UiEvent.ShowMessage(strings.copiedLinkToast))
        }
    }

    fun shareProxyText(proxy: ProxyEntity) {
        val strings = stringsFor(uiState.value.settings.appLanguage)
        TelegramAppHelper.shareProxyText(app, proxy, strings)
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
            val strings = activeStrings()
            val poster = withContext(Dispatchers.Default) {
                QrCodeGenerator.createShareablePosterBitmap(proxy)
            }
            val safeHost = proxy.server.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileName = "TeleProxy_${safeHost}_${proxy.port}.png"
            val result = QrCodeGenerator.saveQrToMediaStore(app, poster, fileName)
            if (result.isSuccess) {
                _events.emit(UiEvent.ShowMessage(strings.qrSavedGalleryToast))
            } else if (result.exceptionOrNull() is UnsupportedOperationException) {
                pendingSafBitmap = poster
                _events.emit(UiEvent.LaunchSafSave(fileName))
            } else {
                _events.emit(UiEvent.ShowMessage(strings.qrSaveFailedToast))
            }
        }
    }

    fun completeSafSave(uri: Uri?) {
        val bitmap = pendingSafBitmap ?: return
        pendingSafBitmap = null
        if (uri == null) return
        viewModelScope.launch {
            val strings = activeStrings()
            val res = QrCodeGenerator.writeBitmapToUri(app, uri, bitmap)
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage(strings.qrSavedGalleryToast))
            } else {
                _events.emit(UiEvent.ShowMessage(strings.qrSaveFailedToast))
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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setAppLanguage(language)
            // Clear transient verification message so no old-language text remains visible
            _verificationReport.value = _verificationReport.value.copy(summaryText = "")
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val strings = activeStrings()
            repository.setAutoSyncEnabled(enabled)
            val interval = uiState.value.settings.syncIntervalMinutes
            WorkScheduler.schedulePeriodicSync(app, interval, enabled)
            _events.emit(
                UiEvent.ShowMessage(
                    if (enabled) strings.autoSyncEnabledToast else strings.autoSyncDisabledToast
                )
            )
        }
    }

    fun setSyncIntervalMinutes(minutes: Long) {
        viewModelScope.launch {
            val strings = activeStrings()
            repository.setSyncIntervalMinutes(minutes)
            val enabled = uiState.value.settings.autoSyncEnabled
            WorkScheduler.schedulePeriodicSync(app, minutes, enabled)
            _events.emit(UiEvent.ShowMessage(strings.intervalUpdatedToast(minutes)))
        }
    }

    fun setAutoFailoverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoFailoverEnabled(enabled)
        }
    }

    fun setStabilityServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val strings = activeStrings()
            repository.setStabilityServiceEnabled(enabled)
            if (enabled) {
                ProxyStabilityService.start(app)
                _events.emit(UiEvent.ShowMessage(strings.monitorStartedToast))
            } else {
                ProxyStabilityService.stop(app)
                _events.emit(UiEvent.ShowMessage(strings.monitorStoppedToast))
            }
        }
    }
}
