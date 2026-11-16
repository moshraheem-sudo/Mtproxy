package com.example.data.network

import android.net.Uri
import com.example.data.model.ProxyEntity
import com.example.security.SecurityGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class FetchResult(
    val proxies: List<ProxyEntity>,
    val sourceUsed: String
)

class MtprotoFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val endpoints = listOf(
        "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
        "https://raw.githubusercontent.com/SoliSpirit/mtproto/main/all_proxies.txt",
        "https://cdn.jsdelivr.net/gh/SoliSpirit/mtproto@master/all_proxies.txt",
        "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/README.md"
    )

    suspend fun fetchFromSoliSpirit(): Result<FetchResult> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (url in endpoints) {
            if (!SecurityGuard.isTrustedHttpsUrl(url)) continue
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "TeleProxy-Android/1.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string().orEmpty()
                        val parsed = parseProxies(body)
                        if (parsed.isNotEmpty()) {
                            return@withContext Result.success(
                                FetchResult(
                                    proxies = parsed,
                                    sourceUsed = url
                                )
                            )
                        }
                    } else {
                        lastError = IllegalStateException("HTTP ${response.code}")
                    }
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        Result.failure(lastError ?: IllegalStateException("تعذر جلب البروكسيات من SoliSpirit/mtproto"))
    }

    fun parseProxies(rawText: String): List<ProxyEntity> {
        val resultMap = LinkedHashMap<String, ProxyEntity>()
        val urlRegex = Regex("""(tg://proxy\?[^\s)"'<>]+|https?://t\.me/proxy\?[^\s)"'<>]+)""")

        urlRegex.findAll(rawText).forEach { match ->
            parseProxyUri(match.value)?.let { proxy ->
                resultMap.putIfAbsent(proxy.id, proxy)
            }
        }

        if (resultMap.isEmpty()) {
            // Fallback parser for server:port:secret lines
            rawText.lineSequence().forEach { line ->
                val clean = line.trim()
                if (clean.isNotEmpty() && !clean.startsWith("#")) {
                    val parts = clean.split(":", "|", "\t", ",").map { it.trim() }
                    if (parts.size >= 3) {
                        val server = parts[0]
                        val port = parts[1].toIntOrNull()
                        val secret = parts[2]
                        if (isValidProxyFields(server, port, secret)) {
                            val id = ProxyEntity.buildId(server, port!!, secret)
                            resultMap.putIfAbsent(
                                id,
                                ProxyEntity(
                                    id = id,
                                    server = server,
                                    port = port,
                                    secret = secret,
                                    secretType = ProxyEntity.classifySecret(secret)
                                )
                            )
                        }
                    }
                }
            }
        }

        return resultMap.values.toList()
    }

    private fun parseProxyUri(rawUrl: String): ProxyEntity? {
        return try {
            val uri = Uri.parse(rawUrl.replace("&amp;", "&"))
            val server = uri.getQueryParameter("server")?.trim().orEmpty()
            val port = uri.getQueryParameter("port")?.trim()?.toIntOrNull()
            val secret = uri.getQueryParameter("secret")?.trim().orEmpty()
            if (!isValidProxyFields(server, port, secret)) return null
            val id = ProxyEntity.buildId(server, port!!, secret)
            ProxyEntity(
                id = id,
                server = server,
                port = port,
                secret = secret,
                secretType = ProxyEntity.classifySecret(secret)
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun isValidProxyFields(server: String, port: Int?, secret: String): Boolean {
        if (port == null) return false
        return SecurityGuard.sanitizeProxyFields(server, port, secret)
    }
}
