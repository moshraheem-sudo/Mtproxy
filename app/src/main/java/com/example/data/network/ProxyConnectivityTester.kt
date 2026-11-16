package com.example.data.network

import com.example.data.model.ProxyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class PingMeasurement(
    val proxyId: String,
    val latencyMs: Int,
    val isReachable: Boolean
)

class ProxyConnectivityTester {

    suspend fun testSingleProxy(
        proxy: ProxyEntity,
        timeoutMs: Int = 3500
    ): PingMeasurement = withContext(Dispatchers.IO) {
        val startNanos = System.nanoTime()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.soTimeout = timeoutMs
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(proxy.server, proxy.port), timeoutMs)
            val elapsedMs = ((System.nanoTime() - startNanos) / 1_000_000L).toInt().coerceAtLeast(1)
            PingMeasurement(
                proxyId = proxy.id,
                latencyMs = elapsedMs,
                isReachable = true
            )
        } catch (_: Throwable) {
            PingMeasurement(
                proxyId = proxy.id,
                latencyMs = ProxyEntity.LATENCY_FAILED,
                isReachable = false
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun testBatch(
        proxies: List<ProxyEntity>,
        maxConcurrency: Int = 12,
        timeoutMs: Int = 3200,
        onProgress: (checkedCount: Int, total: Int) -> Unit = { _, _ -> }
    ): List<PingMeasurement> = coroutineScope {
        if (proxies.isEmpty()) return@coroutineScope emptyList()
        val semaphore = Semaphore(maxConcurrency)
        var completed = 0
        val total = proxies.size
        proxies.map { proxy ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val result = testSingleProxy(proxy, timeoutMs)
                    synchronized(this@coroutineScope) {
                        completed++
                        onProgress(completed, total)
                    }
                    result
                }
            }
        }.awaitAll()
    }
}
