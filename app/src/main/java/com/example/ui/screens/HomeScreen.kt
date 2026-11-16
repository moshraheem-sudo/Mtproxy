package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProxyEntity
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AmberModerate
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAdaptiveSpecs
import com.example.viewmodel.MainUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    onConnectTelegram: () -> Unit,
    onVerifyConnection: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenQr: (ProxyEntity) -> Unit,
    onCopyLink: (ProxyEntity) -> Unit,
    onShareProxy: (ProxyEntity) -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val specs = LocalAdaptiveSpecs.current
    val activeProxy = state.activeProxy
    val workingCount = state.proxies.count { it.isReachable }
    val scrollState = rememberScrollState()

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
            // App Top Header with Responsive Developer Name Pill
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(
                    modifier = Modifier.widthIn(min = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.appSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .clip(RoundedCornerShape(50))
                        .clickable { onNavigateToSettings() }
                        .testTag("developer_header_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = strings.developerName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = strings.developerName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Hero Connection Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_proxy_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xD908182E),
                                    Color(0xF00B1D38)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(specs.cardPadding + 2.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isWorking = activeProxy?.isReachable == true
                        val statusColor = when {
                            state.isSyncing -> MaterialTheme.colorScheme.primary
                            isWorking -> EmeraldVerified
                            activeProxy == null -> AmberModerate
                            else -> CoralError
                        }
                        val statusLabel = when {
                            state.isSyncing -> strings.statusSyncing
                            isWorking -> strings.statusReady
                            activeProxy == null -> strings.statusWaiting
                            else -> strings.statusNeedsRefresh
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (activeProxy != null && activeProxy.isReachable && activeProxy.latencyMs < ProxyEntity.LATENCY_FAILED) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = EmeraldVerified,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${activeProxy.latencyMs} ms",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFamily
                                        ),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (activeProxy != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = strings.activeServerLabel(activeProxy.secretType),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${activeProxy.server}:${activeProxy.port}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = strings.noServersHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }

                    AnimatedVisibility(visible = state.isSyncing || state.isPingingAll) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (state.pingProgressText.isNotBlank()) {
                                Text(
                                    text = state.pingProgressText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onConnectTelegram,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .testTag("connect_telegram_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF229ED9),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.connectTelegramBtn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedButton(
                        onClick = onVerifyConnection,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("verify_connection_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        if (state.verificationReport.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.verifyConnectionBtn,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }

                    AnimatedVisibility(visible = state.verificationReport.summaryText.isNotBlank()) {
                        val verified = state.verificationReport.isVerifiedWorking == true
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (verified) {
                                EmeraldVerified.copy(alpha = 0.16f)
                            } else {
                                AmberModerate.copy(alpha = 0.16f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (verified) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (verified) EmeraldVerified else AmberModerate,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.verificationReport.summaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (activeProxy != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(specs.itemSpacing)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.QrCode2,
                        label = strings.actionQr,
                        testTag = "home_qr_button",
                        onClick = { onOpenQr(activeProxy) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.ContentCopy,
                        label = strings.actionCopy,
                        testTag = "home_copy_button",
                        onClick = { onCopyLink(activeProxy) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.Share,
                        label = strings.actionShare,
                        testTag = "home_share_button",
                        onClick = { onShareProxy(activeProxy) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sync Status Card (Clean Footer Bar with Zero Repeated Words)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(specs.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.syncStatusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = onSyncNow,
                            enabled = !state.isSyncing,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("sync_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.syncNowBtn, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(specs.itemSpacing)
                    ) {
                        StatMiniCard(
                            title = strings.workingServersCountLabel,
                            value = "$workingCount",
                            icon = Icons.Default.Bolt,
                            accent = EmeraldVerified,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToConnections() }
                        )
                        StatMiniCard(
                            title = strings.totalSavedCountLabel,
                            value = "${state.proxies.size}",
                            icon = Icons.Default.Speed,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToConnections() }
                        )
                    }

                    val formattedTime = if (state.settings.lastSyncTimestamp > 0L) {
                        SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault())
                            .format(Date(state.settings.lastSyncTimestamp))
                    } else {
                        strings.notSyncedYet
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_sync_footer_bar")
                    ) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.lastUpdateCloudText(formattedTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val autoSyncOn = state.settings.autoSyncEnabled
                            val badgeColor = if (autoSyncOn) {
                                EmeraldVerified
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = badgeColor.copy(alpha = if (autoSyncOn) 0.14f else 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (autoSyncOn) strings.autoSyncOnBadge else strings.autoSyncOffBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColor,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .heightIn(min = 68.dp)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
