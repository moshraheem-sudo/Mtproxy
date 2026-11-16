package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import com.example.data.model.ProxyEntity
import com.example.security.SecurityGuard
import com.example.ui.i18n.AppStrings

data class TelegramClientInfo(
    val packageName: String,
    val displayName: String,
    val isOfficial: Boolean = false
)

object TelegramAppHelper {

    private val knownClients = listOf(
        Triple("org.telegram.messenger", "Telegram", true),
        Triple("org.telegram.messenger.web", "Telegram Direct", true),
        Triple("org.thunderdog.challegram", "Telegram X", false),
        Triple("org.telegram.plus", "Plus Messenger", false),
        Triple("tw.nekomimi.nekogram", "Nekogram", false),
        Triple("xyz.nextalone.nagram", "Nagram", false),
        Triple("org.aka.messenger", "AKA Messenger", false)
    )

    fun detectInstalledTelegramClients(context: Context): List<TelegramClientInfo> {
        val pm = context.packageManager
        return knownClients.mapNotNull { (pkg, name, official) ->
            if (isPackageInstalled(pm, pkg)) {
                TelegramClientInfo(packageName = pkg, displayName = name, isOfficial = official)
            } else {
                null
            }
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun hasNetworkConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun openProxyInTelegram(context: Context, proxy: ProxyEntity): Boolean {
        if (!SecurityGuard.sanitizeProxyFields(proxy.server, proxy.port, proxy.secret)) {
            return false
        }
        val installed = detectInstalledTelegramClients(context)
        val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse(proxy.tgLink)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (installed.isNotEmpty()) {
                setPackage(installed.first().packageName)
            }
        }

        return try {
            context.startActivity(tgIntent)
            true
        } catch (_: Throwable) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(proxy.tmeLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun copyProxyLink(context: Context, proxy: ProxyEntity) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Telegram Proxy", proxy.tmeLink))
    }

    fun shareProxyText(context: Context, proxy: ProxyEntity, strings: AppStrings) {
        val latencyInfo = if (proxy.isReachable && proxy.latencyMs in 1 until ProxyEntity.LATENCY_FAILED) {
            strings.shareLatencyLine(proxy.latencyMs)
        } else {
            strings.shareDirectLine
        }
        val text = buildString {
            appendLine(strings.shareHeaderLine)
            appendLine("${proxy.server}:${proxy.port}")
            appendLine(latencyInfo)
            appendLine()
            appendLine(strings.shareTapLine)
            appendLine(proxy.tmeLink)
            appendLine()
            append("${strings.appName} • ${strings.developerName}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, strings.appName)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, strings.actionShare).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
