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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.QrCodeDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProxiesScreen
import com.example.ui.screens.StabilityScreen
import com.example.ui.theme.TeleProxyTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.UiEvent

const val ROUTE_HOME = "home"
const val ROUTE_PROXIES = "proxies"
const val ROUTE_STABILITY = "stability"

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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TeleProxyTheme {
                    TeleProxyRootApp()
                }
            }
        }
    }
}

@Composable
fun TeleProxyRootApp(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTelegramMissingDialog by remember { mutableStateOf(false) }

    val safCreateDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        viewModel.completeSafSave(uri)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setStabilityServiceEnabled(true)
        } else {
            viewModel.setStabilityServiceEnabled(false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.LaunchSafSave -> safCreateDocLauncher.launch(event.fileName)
                is UiEvent.TelegramNotInstalledDialog -> showTelegramMissingDialog = true
            }
        }
    }

    val destinations = remember {
        listOf(
            NavDestinationItem(
                route = ROUTE_HOME,
                label = "الاتصال",
                selectedIcon = Icons.Filled.Bolt,
                unselectedIcon = Icons.Outlined.Bolt,
                testTag = "nav_home"
            ),
            NavDestinationItem(
                route = ROUTE_PROXIES,
                label = "البروكسيات",
                selectedIcon = Icons.Filled.List,
                unselectedIcon = Icons.Outlined.List,
                testTag = "nav_proxies"
            ),
            NavDestinationItem(
                route = ROUTE_STABILITY,
                label = "الاستقرار",
                selectedIcon = Icons.Filled.Shield,
                unselectedIcon = Icons.Outlined.Shield,
                testTag = "nav_stability"
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

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
                                label = { Text(dest.label) },
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
                                label = { Text(dest.label) },
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
                            onNavigateToProxies = { navigateTo(ROUTE_PROXIES) },
                            onNavigateToStability = { navigateTo(ROUTE_STABILITY) }
                        )
                    }

                    composable(ROUTE_PROXIES) {
                        ProxiesScreen(
                            state = state,
                            onSearchChange = viewModel::updateSearchQuery,
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

                    composable(ROUTE_STABILITY) {
                        StabilityScreen(
                            state = state,
                            onToggleAutoSync = viewModel::setAutoSyncEnabled,
                            onSelectInterval = viewModel::setSyncIntervalMinutes,
                            onToggleAutoFailover = viewModel::setAutoFailoverEnabled,
                            onToggleStabilityService = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        viewModel.setStabilityServiceEnabled(true)
                                    } else {
                                        notificationPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                } else {
                                    viewModel.setStabilityServiceEnabled(enable)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // QR Code Modal Dialog
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

    // Friendly dialog if Telegram isn't installed
    if (showTelegramMissingDialog) {
        AlertDialog(
            onDismissRequest = { showTelegramMissingDialog = false },
            title = { Text("تطبيق تلغرام غير مثبت") },
            text = {
                Text("لم يتم العثور على تطبيق تلغرام على هذا الجهاز. يمكنك تثبيته من متجر Google Play أو نسخ رابط البروكسي ومشاركته.")
            },
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
                    Text("تثبيت تلغرام")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTelegramMissingDialog = false
                        state.activeProxy?.let { viewModel.copyProxyLink(it) }
                    }
                ) {
                    Text("نسخ الرابط")
                }
            }
        )
    }
}
