package com.example.data.network

import com.example.BuildConfig
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
    val releaseNotes: String? = null,
    val totalUsersCount: Int = 0,
    val checkSucceeded: Boolean = false,
    val errorMessage: String? = null
)

class AppUpdateAndStatsFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Official update & stats backend endpoints (kept internal, never exposed as GitHub text in UI)
    private val releasesApiUrl = "https://api.github.com/repos/moshraheem-sudo/Mtproxy/releases"
    private val repoApiUrl = "https://api.github.com/repos/moshraheem-sudo/Mtproxy"
    private val fallbackReleasesUrl = "https://github.com/moshraheem-sudo/Mtproxy/releases/latest"
    private val rawVersionJsonUrl = "https://raw.githubusercontent.com/moshraheem-sudo/Mtproxy/main/version.json"

    suspend fun fetchUpdateAndUserStats(
        currentVersion: String = BuildConfig.VERSION_NAME
    ): AppRemoteStatus = withContext(Dispatchers.IO) {
        var totalDownloads = 0
        var highestVersion: String? = null
        var bestDownloadUrl: String? = null
        var bestNotes: String? = null
        var releasesChecked = false
        var lastError: String? = null

        // 1. Query Releases API for strictly newer versions + total download/user count
        try {
            val request = Request.Builder()
                .url(releasesApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TeleProxy-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    releasesChecked = true
                    val body = response.body?.string().orEmpty()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val release = array.optJSONObject(i) ?: continue
                        if (release.optBoolean("draft", false)) continue

                        val assets = release.optJSONArray("assets")
                        var apkAssetUrl: String? = null
                        if (assets != null) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.optJSONObject(j) ?: continue
                                totalDownloads += asset.optInt("download_count", 0)
                                val name = asset.optString("name", "")
                                if (apkAssetUrl == null && name.endsWith(".apk", ignoreCase = true)) {
                                    apkAssetUrl = asset.optString("browser_download_url", "").ifBlank { null }
                                }
                            }
                        }

                        if (!release.optBoolean("prerelease", false)) {
                            val rawTag = release.optString("tag_name", "")
                            val cleanTag = normalizeVersion(rawTag)
                            if (cleanTag.isNotEmpty()) {
                                val isHigherThanCurrent = compareSemVer(cleanTag, currentVersion) > 0
                                val isHigherThanBest = highestVersion == null || compareSemVer(cleanTag, highestVersion!!) > 0
                                if (isHigherThanCurrent && isHigherThanBest) {
                                    highestVersion = cleanTag
                                    bestDownloadUrl = apkAssetUrl
                                        ?: release.optString("html_url", "").ifBlank { fallbackReleasesUrl }
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

        // 2. Fallback: check optional version.json if no release found yet
        if (highestVersion == null) {
            try {
                val req = Request.Builder()
                    .url(rawVersionJsonUrl)
                    .header("User-Agent", "TeleProxy-Android/${BuildConfig.VERSION_NAME}")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        val remoteVer = normalizeVersion(json.optString("version", ""))
                        if (remoteVer.isNotEmpty() && compareSemVer(remoteVer, currentVersion) > 0) {
                            highestVersion = remoteVer
                            bestDownloadUrl = json.optString("apkUrl", "")
                                .ifBlank { json.optString("url", fallbackReleasesUrl) }
                            bestNotes = json.optString("notes", "").trim().ifBlank { null }
                            releasesChecked = true
                        }
                        val jsonUsers = json.optInt("usersCount", 0)
                        if (jsonUsers > totalDownloads) {
                            totalDownloads = jsonUsers
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        // 3. Also inspect repository metadata if releases had 0 downloads (e.g., repository stars/watchers fallback check)
        var repoMetaSucceeded = false
        try {
            val repoReq = Request.Builder()
                .url(repoApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TeleProxy-Android/${BuildConfig.VERSION_NAME}")
                .build()
            client.newCall(repoReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    repoMetaSucceeded = true
                }
            }
        } catch (_: Throwable) {
        }

        val succeeded = releasesChecked || repoMetaSucceeded
        AppRemoteStatus(
            currentVersion = currentVersion,
            latestVersion = highestVersion,
            isUpdateAvailable = highestVersion != null && compareSemVer(highestVersion!!, currentVersion) > 0,
            downloadUrl = bestDownloadUrl,
            releaseNotes = bestNotes,
            totalUsersCount = totalDownloads,
            checkSucceeded = succeeded,
            errorMessage = if (succeeded) null else (lastError ?: "تعذر الوصول إلى خادم التحديثات حالياً")
        )
    }

    companion object {
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
