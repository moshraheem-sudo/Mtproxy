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

data class TelegramClientInfo(
    val packageName: String,
    val displayName: String
)

object TelegramAppHelper {

    private val knownClients = listOf(
        "org.telegram.messenger" to "Telegram الرسمي",
        "org.telegram.messenger.web" to "Telegram Direct",
        "org.thunderdog.challegram" to "Telegram X",
        "org.telegram.plus" to "Plus Messenger",
        "tw.nekomimi.nekogram" to "Nekogram",
        "xyz.nextalone.nagram" to "Nagram",
        "org.aka.messenger" to "AKA Messenger"
    )

    fun detectInstalledTelegramClients(context: Context): List<TelegramClientInfo> {
        val pm = context.packageManager
        return knownClients.mapNotNull { (pkg, name) ->
            if (isPackageInstalled(pm, pkg)) {
                TelegramClientInfo(packageName = pkg, displayName = name)
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

    fun shareProxyText(context: Context, proxy: ProxyEntity) {
        val latencyInfo = if (proxy.isReachable && proxy.latencyMs in 1 until ProxyEntity.LATENCY_FAILED) {
            "السرعة المفحوصة: ${proxy.latencyMs} ms"
        } else {
            "بروكسي MTProto مباشر"
        }
        val text = buildString {
            appendLine("اتصال تلغرام سريع ومستقر عبر بروكسي MTProto:")
            appendLine("السيرفر: ${proxy.server}:${proxy.port}")
            appendLine(latencyInfo)
            appendLine()
            appendLine("اضغط للربط المباشر في تلغرام:")
            appendLine(proxy.tmeLink)
            appendLine()
            append("تطبيق تلي بروكسي • تطوير: محمد شهيد الاعاجيبي")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "بروكسي تلغرام سريع")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة البروكسي مع الأصدقاء").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
