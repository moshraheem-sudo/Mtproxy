package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Entity(tableName = "proxies")
data class ProxyEntity(
    @PrimaryKey val id: String,
    val server: String,
    val port: Int,
    val secret: String,
    val latencyMs: Int = LATENCY_UNTESTED,
    val isReachable: Boolean = false,
    val isFavorite: Boolean = false,
    val secretType: String = "MTProto",
    val lastCheckedAt: Long = 0L,
    val addedAt: Long = System.currentTimeMillis()
) {
    val tgLink: String
        get() {
            val s = URLEncoder.encode(server, StandardCharsets.UTF_8.name())
            val sec = URLEncoder.encode(secret, StandardCharsets.UTF_8.name())
            return "tg://proxy?server=$s&port=$port&secret=$sec"
        }

    val tmeLink: String
        get() {
            val s = URLEncoder.encode(server, StandardCharsets.UTF_8.name())
            val sec = URLEncoder.encode(secret, StandardCharsets.UTF_8.name())
            return "https://t.me/proxy?server=$s&port=$port&secret=$sec"
        }

    companion object {
        const val LATENCY_UNTESTED = -1
        const val LATENCY_FAILED = Int.MAX_VALUE

        fun buildId(server: String, port: Int, secret: String): String {
            return "${server.lowercase().trim()}:$port:${secret.trim()}"
        }

        fun classifySecret(secret: String): String {
            val clean = secret.trim().lowercase()
            return when {
                clean.startsWith("ee") && clean.length > 32 -> "Fake-TLS"
                clean.startsWith("dd") && clean.length == 34 -> "Secure DD"
                clean.length == 32 -> "Standard"
                else -> "MTProto"
            }
        }
    }
}
