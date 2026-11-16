package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ThemeMode
import com.example.security.SecurityGuard
import com.example.ui.components.QrCodeDialog
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProxiesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalAdaptiveSpecs
import com.example.ui.theme.TeleProxyTheme
import com.example.ui.theme.computeAdaptiveUiSpecs
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiEvent

const val ROUTE_HOME = "home"
const val ROUTE_CONNECTIONS = "connections"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeleProxyRootApp()
        }
    }
}

@Composable
fun TeleProxyRootApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val currentStrings = remember(state.settings.appLanguage) {
        stringsFor(state.settings.appLanguage)
    }
    val layoutDirection = if (state.settings.appLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val isDarkTheme = when (state.settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val adaptiveSpecs = remember(maxWidth, density.fontScale) {
            computeAdaptiveUiSpecs(maxWidth = maxWidth, systemFontScale = density.fontScale)
        }

        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalStrings provides currentStrings,
            LocalAdaptiveSpecs provides adaptiveSpecs
        ) {
            TeleProxyTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                var showTelegramMissingDialog by remember { mutableStateOf(false) }
                var showInstallPermissionDialog by remember { mutableStateOf(false) }
                var pendingNotifAction by remember { mutableStateOf<(() -> Unit)?>(null) }

            val safCreateDocLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("image/png")
            ) { uri ->
                viewModel.completeSafSave(uri)
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    pendingNotifAction?.invoke()
                } else {
                    viewModel.setStabilityServiceEnabled(false)
                }
                pendingNotifAction = null
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.retryPendingInstallOnResume()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                        is UiEvent.LaunchSafSave -> safCreateDocLauncher.launch(event.fileName)
                        is UiEvent.TelegramNotInstalledDialog -> showTelegramMissingDialog = true
                        is UiEvent.RequestInstallPermissionDialog -> showInstallPermissionDialog = true
                    }
                }
            }

            val destinations = remember(currentStrings) {
                listOf(
                    NavDestinationItem(
                        route = ROUTE_HOME,
                        label = currentStrings.navConnect,
                        selectedIcon = Icons.Filled.Bolt,
                        unselectedIcon = Icons.Outlined.Bolt,
                        testTag = "nav_home"
                    ),
                    NavDestinationItem(
                        route = ROUTE_CONNECTIONS,
                        label = currentStrings.navConnections,
                        selectedIcon = Icons.AutoMirrored.Filled.List,
                        unselectedIcon = Icons.AutoMirrored.Outlined.List,
                        testTag = "nav_connections"
                    ),
                    NavDestinationItem(
                        route = ROUTE_SETTINGS,
                        label = currentStrings.navSettings,
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings,
                        testTag = "nav_settings"
                    )
                )
            }

            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

            val navigateTo: (String) -> Unit = { route ->
                if (currentRoute != route) {
                    navController.navigate(route) {
                        popUpTo(ROUTE_HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            val ensureNotificationPermissionThen: (() -> Unit) -> Unit = { action ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        action()
                    } else {
                        pendingNotifAction = action
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    action()
                }
            }

            val useRail = adaptiveSpecs.useNavigationRail

            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            destinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(ROUTE_HOME) {
                                HomeScreen(
                                    state = state,
                                    onConnectTelegram = { viewModel.connectTelegramWithProxy() },
                                    onVerifyConnection = { viewModel.verifyActiveConnection() },
                                    onSyncNow = { viewModel.syncNow() },
                                    onOpenQr = { viewModel.openQrDialog(it) },
                                    onCopyLink = { viewModel.copyProxyLink(it) },
                                    onShareProxy = { viewModel.shareProxyText(it) },
                                    onNavigateToConnections = { navigateTo(ROUTE_CONNECTIONS) },
                                    onNavigateToSettings = { navigateTo(ROUTE_SETTINGS) }
                                )
                            }

                            composable(ROUTE_CONNECTIONS) {
                                ProxiesScreen(
                                    state = state,
                                    onFilterChange = viewModel::setFilter,
                                    onSyncNow = viewModel::syncNow,
                                    onPingAll = viewModel::pingAllProxies,
                                    onSelectProxy = viewModel::selectProxy,
                                    onConnectProxy = viewModel::connectTelegramWithProxy,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onOpenQr = viewModel::openQrDialog,
                                    onShareProxy = viewModel::shareProxyText
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                LaunchedEffect(Unit) {
                                    viewModel.checkForAppUpdates(manual = false)
                                }
                                SettingsScreen(
                                    state = state,
                                    onSelectLanguage = viewModel::setAppLanguage,
                                    onSelectThemeMode = viewModel::setThemeMode,
                                    onCheckAppUpdate = { viewModel.checkForAppUpdates(manual = true) },
                                    onStartInAppDownload = viewModel::startInAppUpdateDownload,
                                    onCancelInAppDownload = viewModel::cancelInAppDownload,
                                    onInstallDownloadedApk = viewModel::installDownloadedUpdate,
                                    onToggleAutoSync = viewModel::setAutoSyncEnabled,
                                    onSelectInterval = viewModel::setSyncIntervalMinutes,
                                    onToggleAutoFailover = viewModel::setAutoFailoverEnabled,
                                    onToggleStabilityService = { enable ->
                                        if (enable) {
                                            ensureNotificationPermissionThen {
                                                viewModel.setStabilityServiceEnabled(true)
                                            }
                                        } else {
                                            viewModel.setStabilityServiceEnabled(false)
                                        }
                                    },
                                    onOpenTrustedUrl = { url ->
                                        if (SecurityGuard.isTrustedHttpsUrl(url)) {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

            val qrTarget = state.qrTargetProxy
            if (qrTarget != null) {
                QrCodeDialog(
                    proxy = qrTarget,
                    qrBitmap = state.qrPreviewBitmap,
                    onDismiss = viewModel::closeQrDialog,
                    onDownloadImage = { viewModel.downloadQrPoster(qrTarget) },
                    onShareImage = { viewModel.shareQrPoster(qrTarget) },
                    onCopyLink = { viewModel.copyProxyLink(qrTarget) }
                )
            }

            if (showInstallPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showInstallPermissionDialog = false },
                    title = { Text(currentStrings.installPermTitle) },
                    text = { Text(currentStrings.installPermBody) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showInstallPermissionDialog = false
                                viewModel.openSystemInstallPermission()
                            }
                        ) {
                            Text(currentStrings.grantPermBtn)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showInstallPermissionDialog = false }) {
                            Text(currentStrings.cancelBtn)
                        }
                    }
                )
            }

            if (showTelegramMissingDialog) {
                AlertDialog(
                    onDismissRequest = { showTelegramMissingDialog = false },
                    title = { Text(currentStrings.telegramMissingTitle) },
                    text = { Text(currentStrings.telegramMissingBody) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showTelegramMissingDialog = false
                                val playIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger")
                                )
                                context.startActivity(playIntent)
                            }
                        ) {
                            Text(currentStrings.installTelegramBtn)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showTelegramMissingDialog = false
                                state.activeProxy?.let { viewModel.copyProxyLink(it) }
                            }
                        ) {
                            Text(currentStrings.actionCopy)
                        }
                    }
                )
            }
        }
    }
}
}
