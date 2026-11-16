package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.data.network.AppUpdateAndStatsFetcher
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAdaptiveSpecs
import com.example.util.AppDownloadState
import com.example.viewmodel.MainUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: MainUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onCheckAppUpdate: () -> Unit,
    onStartInAppDownload: () -> Unit,
    onCancelInAppDownload: () -> Unit,
    onInstallDownloadedApk: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onSelectInterval: (Long) -> Unit,
    onToggleAutoFailover: (Boolean) -> Unit,
    onToggleStabilityService: (Boolean) -> Unit,
    onOpenTrustedUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val specs = LocalAdaptiveSpecs.current
    val scrollState = rememberScrollState()
    val settings = state.settings
    val hasUpdate = !settings.latestAvailableVersion.isNullOrBlank()
    val downloadState = state.appDownloadState

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = specs.horizontalPadding, vertical = specs.verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = specs.contentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(specs.sectionSpacing)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = strings.settingsHeader,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = strings.settingsSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 1. Language & Theme Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.languageSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LanguageOptionButton(
                                language = AppLanguage.ARABIC,
                                selected = settings.appLanguage == AppLanguage.ARABIC,
                                onClick = { onSelectLanguage(AppLanguage.ARABIC) },
                                modifier = Modifier.weight(1f)
                            )
                            LanguageOptionButton(
                                language = AppLanguage.ENGLISH,
                                selected = settings.appLanguage == AppLanguage.ENGLISH,
                                onClick = { onSelectLanguage(AppLanguage.ENGLISH) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LanguageOptionButton(
                                language = AppLanguage.KURDISH,
                                selected = settings.appLanguage == AppLanguage.KURDISH,
                                onClick = { onSelectLanguage(AppLanguage.KURDISH) },
                                modifier = Modifier.weight(1f)
                            )
                            LanguageOptionButton(
                                language = AppLanguage.PERSIAN,
                                selected = settings.appLanguage == AppLanguage.PERSIAN,
                                onClick = { onSelectLanguage(AppLanguage.PERSIAN) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.themeSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            icon = Icons.Default.BrightnessAuto,
                            label = strings.themeSystem,
                            selected = settings.themeMode == ThemeMode.SYSTEM,
                            onClick = { onSelectThemeMode(ThemeMode.SYSTEM) },
                            testTag = "theme_system_option",
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            icon = Icons.Default.LightMode,
                            label = strings.themeLight,
                            selected = settings.themeMode == ThemeMode.LIGHT,
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                            testTag = "theme_light_option",
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            icon = Icons.Default.DarkMode,
                            label = strings.themeDark,
                            selected = settings.themeMode == ThemeMode.DARK,
                            onClick = { onSelectThemeMode(ThemeMode.DARK) },
                            testTag = "theme_dark_option",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Connection Stability Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.stabilitySectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SettingSwitchRow(
                        icon = Icons.Default.Autorenew,
                        title = strings.autoSyncTitle,
                        subtitle = strings.autoSyncSubtitle,
                        checked = settings.autoSyncEnabled,
                        onCheckedChange = onToggleAutoSync,
                        testTag = "auto_sync_switch"
                    )

                    if (settings.autoSyncEnabled) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.syncIntervalTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val intervals = listOf(
                                15L to strings.interval15m,
                                30L to strings.interval30m,
                                60L to strings.interval1h,
                                360L to strings.interval6h,
                                720L to strings.interval12h
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                intervals.forEach { (mins, label) ->
                                    FilterChip(
                                        selected = settings.syncIntervalMinutes == mins,
                                        onClick = { onSelectInterval(mins) },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("interval_chip_$mins")
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingSwitchRow(
                        icon = Icons.Default.Security,
                        title = strings.autoFailoverTitle,
                        subtitle = strings.autoFailoverSubtitle,
                        checked = settings.autoFailoverEnabled,
                        onCheckedChange = onToggleAutoFailover,
                        testTag = "auto_failover_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    SettingSwitchRow(
                        icon = Icons.Default.NotificationsActive,
                        title = strings.stabilityServiceTitle,
                        subtitle = strings.stabilityServiceSubtitle,
                        checked = settings.stabilityServiceEnabled,
                        onCheckedChange = onToggleStabilityService,
                        testTag = "stability_service_switch"
                    )
                }
            }

            // 3. Installed Telegram Clients & Compatibility Card (100% Localized)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.compatibilityTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val clients = state.verificationReport.installedClients
                    if (clients.isNotEmpty()) {
                        val localizedClientNames = clients.joinToString(" • ") { info ->
                            if (info.isOfficial && info.packageName == "org.telegram.messenger") {
                                strings.officialTelegramName
                            } else {
                                info.displayName
                            }
                        }
                        Text(
                            text = strings.detectedClientsText(localizedClientNames),
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldVerified,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = strings.compatibilityFallbackText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val localizedSyncState = strings.formatSyncState(
                        code = settings.lastSyncStateCode,
                        working = settings.lastSyncWorkingCount,
                        total = settings.lastSyncCount,
                        errorDetail = settings.lastSyncErrorDetail
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.lastSyncLabel(localizedSyncState),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
            }

            // 4. Security Shield & Open-Source Attribution Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_opensource_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldVerified
                        )
                        Text(
                            text = strings.securityOpenSourceTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Active Security Shield Summary
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldVerified.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(11.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = strings.securityShieldTitle,
                                tint = EmeraldVerified,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = strings.securityShieldTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldVerified,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.securityShieldDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // App Open Source Repository
                    OpenSourceRepoCard(
                        title = strings.appOpenSourceLabel,
                        repoName = "github.com/moshraheem-sudo/Mtproxy",
                        hint = strings.tapToOpenLinkHint,
                        onClick = { onOpenTrustedUrl(AppUpdateAndStatsFetcher.APP_REPO_URL) },
                        testTag = "app_repo_link_card"
                    )

                    // Proxy Provider Open Source Repository
                    OpenSourceRepoCard(
                        title = strings.proxyProviderOpenSourceLabel,
                        repoName = "github.com/SoliSpirit/mtproto",
                        hint = strings.tapToOpenLinkHint,
                        onClick = { onOpenTrustedUrl(AppUpdateAndStatsFetcher.PROXY_PROVIDER_REPO_URL) },
                        testTag = "proxy_repo_link_card"
                    )
                }
            }

            // 5. Developer Attribution Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding + 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = strings.developerName,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.developerTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = strings.developerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = strings.developerBio(state.currentAppVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 6. Compact App Info & Updates Card (At bottom after Developer Card)
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_updates_bottom_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.appUpdatesCardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = strings.appNameLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = strings.currentVersionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "v${state.currentAppVersion}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldVerified
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = hasUpdate) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = EmeraldVerified.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = EmeraldVerified
                                    )
                                    Text(
                                        text = strings.nextVersionAvailable(settings.latestAvailableVersion.orEmpty()),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = EmeraldVerified,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!settings.latestReleaseNotes.isNullOrBlank()) {
                                    Text(
                                        text = settings.latestReleaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                when (downloadState) {
                                    is AppDownloadState.Downloading -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (downloadState.totalBytes > 0L) {
                                                LinearProgressIndicator(
                                                    progress = { downloadState.progress },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = EmeraldVerified
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = EmeraldVerified
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${downloadState.downloadedMb} / ${downloadState.totalMb}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = JetBrainsMonoFamily
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (downloadState.totalBytes > 0L) {
                                                    Text(
                                                        text = "${(downloadState.progress * 100).toInt()}%",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontFamily = JetBrainsMonoFamily
                                                        ),
                                                        color = EmeraldVerified,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                IconButton(
                                                    onClick = onCancelInAppDownload,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = strings.cancelBtn,
                                                        tint = CoralError
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    is AppDownloadState.ReadyToInstall -> {
                                        Text(
                                            text = strings.downloadCompleteMsg(downloadState.totalMb),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldVerified,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Button(
                                            onClick = onInstallDownloadedApk,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldVerified,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("install_downloaded_apk_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.InstallMobile,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.installUpdateNowBtn,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    is AppDownloadState.Error -> {
                                        Text(
                                            text = strings.formatDownloadError(downloadState.reasonKey, downloadState.detail),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CoralError
                                        )
                                        Button(
                                            onClick = onStartInAppDownload,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("retry_inapp_download_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(strings.retryDownloadBtn, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }

                                    AppDownloadState.Idle -> {
                                        Button(
                                            onClick = onStartInAppDownload,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldVerified,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("download_app_update_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = strings.downloadUpdateBtn(settings.latestAvailableVersion.orEmpty()),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onCheckAppUpdate,
                        enabled = !state.isCheckingAppUpdate,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("check_app_update_button")
                    ) {
                        if (state.isCheckingAppUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.checkUpdatesBtn,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OpenSourceRepoCard(
    title: String,
    repoName: String,
    hint: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = repoName,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = hint,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LanguageOptionButton(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val border = if (selected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = border,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("lang_option_${language.code}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ThemeOptionCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val border = if (selected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = border,
        modifier = modifier
            .heightIn(min = 60.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
