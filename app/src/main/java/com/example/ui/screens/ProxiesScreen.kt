package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProxyEntity
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AmberModerate
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalAdaptiveSpecs
import com.example.viewmodel.MainUiState
import com.example.viewmodel.ProxyFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProxiesScreen(
    state: MainUiState,
    onFilterChange: (ProxyFilter) -> Unit,
    onSyncNow: () -> Unit,
    onPingAll: () -> Unit,
    onSelectProxy: (ProxyEntity) -> Unit,
    onConnectProxy: (ProxyEntity) -> Unit,
    onToggleFavorite: (ProxyEntity) -> Unit,
    onOpenQr: (ProxyEntity) -> Unit,
    onShareProxy: (ProxyEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val specs = LocalAdaptiveSpecs.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = specs.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = specs.contentMaxWidth)
        ) {
            Spacer(modifier = Modifier.height(specs.verticalPadding))

            // Responsive Header + Action Buttons (wraps gracefully on small phones so counts never truncate)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = strings.connectionsHeader,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.availableCountText(state.filteredProxies.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onPingAll,
                        enabled = !state.isPingingAll && !state.isSyncing && state.proxies.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("ping_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = strings.checkAllBtn,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.checkAllBtn, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !state.isSyncing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("proxies_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = strings.refreshBtn,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.refreshBtn, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = state.isSyncing || state.isPingingAll) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    if (state.pingProgressText.isNotBlank()) {
                        Text(
                            text = state.pingProgressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = state.activeFilter == ProxyFilter.ALL,
                    onClick = { onFilterChange(ProxyFilter.ALL) },
                    label = {
                        Text(
                            strings.filterAll(state.proxies.size),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = state.activeFilter == ProxyFilter.REACHABLE_ONLY,
                    onClick = { onFilterChange(ProxyFilter.REACHABLE_ONLY) },
                    label = {
                        Text(
                            strings.filterWorking(state.proxies.count { it.isReachable }),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("filter_working_chip")
                )
                FilterChip(
                    selected = state.activeFilter == ProxyFilter.FAVORITES,
                    onClick = { onFilterChange(ProxyFilter.FAVORITES) },
                    label = {
                        Text(
                            strings.filterFavorites,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("filter_favorites_chip")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (state.filteredProxies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = if (state.proxies.isEmpty()) {
                                strings.emptyConnectionsList
                            } else {
                                strings.emptyFilteredConnections
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (state.proxies.isEmpty()) {
                            Button(
                                onClick = onSyncNow,
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(strings.fetchNowBtn, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(specs.itemSpacing)
                ) {
                    items(
                        items = state.filteredProxies,
                        key = { it.id }
                    ) { proxy ->
                        val isActive = state.activeProxy?.id == proxy.id
                        ProxyItemCard(
                            proxy = proxy,
                            isActive = isActive,
                            onSelect = { onSelectProxy(proxy) },
                            onConnect = { onConnectProxy(proxy) },
                            onToggleFavorite = { onToggleFavorite(proxy) },
                            onOpenQr = { onOpenQr(proxy) },
                            onShare = { onShareProxy(proxy) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyItemCard(
    proxy: ProxyEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onConnect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQr: () -> Unit,
    onShare: () -> Unit
) {
    val strings = LocalStrings.current
    val specs = LocalAdaptiveSpecs.current
    val border = if (isActive) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(18.dp),
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("proxy_item_card_${proxy.port}")
    ) {
        Column(
            modifier = Modifier.padding(specs.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = proxy.server,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = strings.portLabel(proxy.port, proxy.secretType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LatencyBadge(proxy = proxy)
            }

            if (specs.useStackedHeaders) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onConnect,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.connectTelegramShort,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProxyQuickActionIcons(
                            proxy = proxy,
                            onOpenQr = onOpenQr,
                            onShare = onShare,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onConnect,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = strings.connectTelegramShort,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        ProxyQuickActionIcons(
                            proxy = proxy,
                            onOpenQr = onOpenQr,
                            onShare = onShare,
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProxyQuickActionIcons(
    proxy: ProxyEntity,
    onOpenQr: () -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val strings = LocalStrings.current
    IconButton(
        onClick = onOpenQr,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = strings.actionQr,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
    IconButton(
        onClick = onShare,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = strings.actionShare,
            modifier = Modifier.size(20.dp)
        )
    }
    IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = if (proxy.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
            contentDescription = strings.filterFavorites,
            tint = if (proxy.isFavorite) AmberModerate else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LatencyBadge(proxy: ProxyEntity) {
    val strings = LocalStrings.current
    val (bgColor, textColor, label) = when {
        proxy.latencyMs == ProxyEntity.LATENCY_UNTESTED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            strings.statusUntested
        )
        !proxy.isReachable || proxy.latencyMs >= ProxyEntity.LATENCY_FAILED -> Triple(
            CoralError.copy(alpha = 0.14f),
            CoralError,
            strings.statusUnreachable
        )
        proxy.latencyMs <= 180 -> Triple(
            EmeraldVerified.copy(alpha = 0.16f),
            EmeraldVerified,
            "${proxy.latencyMs} ms"
        )
        else -> Triple(
            AmberModerate.copy(alpha = 0.16f),
            AmberModerate,
            "${proxy.latencyMs} ms"
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = if (proxy.isReachable) JetBrainsMonoFamily else MaterialTheme.typography.labelSmall.fontFamily
            ),
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
