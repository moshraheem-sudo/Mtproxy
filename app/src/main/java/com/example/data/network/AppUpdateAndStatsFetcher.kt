package com.example.data.network

import com.example.BuildConfig
import com.example.security.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppRemoteStatus(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val latestVersion: String? = null,
    val isUpdateAvailable: Boolean = false,
    val downloadUrl: String? = null,
    val apkAssetUrl: String? = null,
    val releaseNotes: String? = null,
    val checkSucceeded: Boolean = false,
    val errorMessage: String? = null
)

class AppUpdateAndStatsFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val releasesApiUrl = "https://api.github.com/repos/moshraheem-sudo/Mtproxy/releases"
    private val rawVersionJsonUrl = "https://raw.githubusercontent.com/moshraheem-sudo/Mtproxy/main/version.json"
    private val fallbackReleasesUrl = "https://github.com/moshraheem-sudo/Mtproxy/releases/latest"

    suspend fun fetchAppUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): AppRemoteStatus = withContext(Dispatchers.IO) {
        var highestVersion: String? = null
        var bestHtmlUrl: String? = null
        var bestApkUrl: String? = null
        var bestNotes: String? = null
        var anyNetworkSuccess = false
        var lastError: String? = null

        // 1. Check Releases for strictly higher SemVer (> 1.0.0) & APK assets
        try {
            val request = Request.Builder()
                .url(releasesApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TeleProxy-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    anyNetworkSuccess = true
                    val body = response.body?.string().orEmpty()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val release = array.optJSONObject(i) ?: continue
                        if (release.optBoolean("draft", false)) continue

                        val assets = release.optJSONArray("assets")
                        var apkUrlInRelease: String? = null
                        if (assets != null) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.optJSONObject(j) ?: continue
                                val name = asset.optString("name", "")
                                if (apkUrlInRelease == null && name.endsWith(".apk", ignoreCase = true)) {
                                    val candidate = asset.optString("browser_download_url", "").ifBlank { null }
                                    if (SecurityGuard.isTrustedHttpsUrl(candidate)) {
                                        apkUrlInRelease = candidate
                                    }
                                }
                            }
                        }

                        if (!release.optBoolean("prerelease", false)) {
                            val cleanTag = normalizeVersion(release.optString("tag_name", ""))
                            if (cleanTag.isNotEmpty()) {
                                val higherThanCurrent = compareSemVer(cleanTag, currentVersion) > 0
                                val higherThanBest = highestVersion == null || compareSemVer(cleanTag, highestVersion!!) > 0
                                if (higherThanCurrent && higherThanBest) {
                                    highestVersion = cleanTag
                                    bestApkUrl = apkUrlInRelease
                                    bestHtmlUrl = release.optString("html_url", "").ifBlank { fallbackReleasesUrl }
                                    bestNotes = release.optString("body", "").trim().ifBlank { null }
                                }
                            }
                        }
                    }
                } else {
                    lastError = "HTTP ${response.code}"
                }
            }
        } catch (t: Throwable) {
            lastError = t.localizedMessage
        }

        // 2. Fallback check for version.json in repository if no release found
        if (highestVersion == null) {
            try {
                val req = Request.Builder()
                    .url(rawVersionJsonUrl)
                    .header("User-Agent", "TeleProxy-Android/${BuildConfig.VERSION_NAME}")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        anyNetworkSuccess = true
                        val json = JSONObject(resp.body?.string().orEmpty())
                        val remoteVer = normalizeVersion(json.optString("version", ""))
                        if (remoteVer.isNotEmpty() && compareSemVer(remoteVer, currentVersion) > 0) {
                            highestVersion = remoteVer
                            val rawApk = json.optString("apkUrl", "").ifBlank { null }
                            val trustedApk = if (SecurityGuard.isTrustedHttpsUrl(rawApk)) rawApk else null
                            val rawUrl = json.optString("url", fallbackReleasesUrl)
                            val trustedUrl = if (SecurityGuard.isTrustedHttpsUrl(rawUrl)) rawUrl else fallbackReleasesUrl
                            bestApkUrl = trustedApk
                            bestHtmlUrl = trustedApk ?: trustedUrl
                            bestNotes = json.optString("notes", "").trim().ifBlank { null }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        AppRemoteStatus(
            currentVersion = currentVersion,
            latestVersion = highestVersion,
            isUpdateAvailable = highestVersion != null && compareSemVer(highestVersion!!, currentVersion) > 0,
            downloadUrl = bestApkUrl ?: bestHtmlUrl,
            apkAssetUrl = bestApkUrl,
            releaseNotes = bestNotes,
            checkSucceeded = anyNetworkSuccess,
            errorMessage = if (anyNetworkSuccess) null else (lastError ?: "تعذر الوصول إلى خادم التحديثات حالياً")
        )
    }

    companion object {
        const val APP_REPO_URL = "https://github.com/moshraheem-sudo/Mtproxy"
        const val PROXY_PROVIDER_REPO_URL = "https://github.com/SoliSpirit/mtproto"

        fun normalizeVersion(raw: String): String {
            return raw.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore("-")
                .substringBefore("+")
                .trim()
        }

        fun compareSemVer(remote: String, current: String): Int {
            val rParts = normalizeVersion(remote).split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = normalizeVersion(current).split(".").map { it.toIntOrNull() ?: 0 }
            val maxLen = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r != c) return r.compareTo(c)
            }
            return 0
        }
    }
}
