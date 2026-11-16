package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProxyEntity
import com.example.ui.theme.AmberModerate
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.viewmodel.MainUiState
import com.example.viewmodel.ProxyFilter

@Composable
fun ProxiesScreen(
    state: MainUiState,
    onSearchChange: (String) -> Unit,
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "قائمة البروكسيات",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.filteredProxies.size} بروكسي متاح",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPingAll,
                        enabled = !state.isPingingAll && !state.isSyncing && state.proxies.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("ping_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "فحص السرعة للكل",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص الكل", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !state.isSyncing,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("proxies_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث القائمة",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحديث", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(visible = state.isSyncing || state.isPingingAll) {
                Column(
                    modifier = Modifier.padding(bottom = 10.dp),
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

            // Search Field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("ابحث بالعنوان أو المنفذ...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proxy_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.activeFilter == ProxyFilter.ALL,
                    onClick = { onFilterChange(ProxyFilter.ALL) },
                    label = { Text("الكل (${state.proxies.size})") },
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = state.activeFilter == ProxyFilter.REACHABLE_ONLY,
                    onClick = { onFilterChange(ProxyFilter.REACHABLE_ONLY) },
                    label = { Text("الشغالة (${state.proxies.count { it.isReachable }})") },
                    modifier = Modifier.testTag("filter_working_chip")
                )
                FilterChip(
                    selected = state.activeFilter == ProxyFilter.FAVORITES,
                    onClick = { onFilterChange(ProxyFilter.FAVORITES) },
                    label = { Text("المفضلة") },
                    modifier = Modifier.testTag("filter_favorites_chip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.filteredProxies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
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
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (state.proxies.isEmpty()) {
                                "لا توجد بروكسيات محفوظة بعد.\nاضغط تحديث لجلب أحدث القائمة من SoliSpirit/mtproto."
                            } else {
                                "لا توجد نتائج مطابقة للبحث أو الفلتر الحالي."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (state.proxies.isEmpty()) {
                            Button(
                                onClick = onSyncNow,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("جلب البروكسيات الآن")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                                contentDescription = "البروكسي النشط",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = proxy.server,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily
                            ),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "المنفذ: ${proxy.port}  •  ${proxy.secretType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LatencyBadge(proxy = proxy)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onConnect,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال بتلغرام", style = MaterialTheme.typography.labelLarge)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onOpenQr,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "عرض باركود QR",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة البروكسي"
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (proxy.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "إضافة للمفضلة",
                            tint = if (proxy.isFavorite) AmberModerate else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatencyBadge(proxy: ProxyEntity) {
    val (bgColor, textColor, label) = when {
        proxy.latencyMs == ProxyEntity.LATENCY_UNTESTED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "غير مفحوص"
        )
        !proxy.isReachable || proxy.latencyMs >= ProxyEntity.LATENCY_FAILED -> Triple(
            CoralError.copy(alpha = 0.14f),
            CoralError,
            "لا يستجيب"
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
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = if (proxy.isReachable) JetBrainsMonoFamily else MaterialTheme.typography.labelMedium.fontFamily
            ),
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
