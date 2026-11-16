package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.TeleProxyApp
import com.example.data.model.ProxyEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProxyStabilityService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    (applicationContext as? TeleProxyApp)?.repository?.setStabilityServiceEnabled(false)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC_NOW -> {
                serviceScope.launch {
                    updateNotification("جارٍ فحص وتحديث البروكسيات...", null)
                    val app = applicationContext as? TeleProxyApp ?: return@launch
                    val summary = app.repository.syncAndVerify(maxTestCount = 50)
                    val proxy = summary.activeProxy
                    val text = if (proxy != null && proxy.isReachable) {
                        "${proxy.server}:${proxy.port} • ${proxy.latencyMs} ms"
                    } else {
                        summary.errorMessage ?: "تم الفحص (${summary.workingCount} شغال)"
                    }
                    updateNotification(text, proxy)
                }
                return START_STICKY
            }
        }

        val initialNotification = buildNotification("جارٍ مراقبة استقرار البروكسي...", null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val app = applicationContext as? TeleProxyApp ?: return@launch
            while (isActive) {
                val (activeProxy, switched) = app.repository.verifyActiveOrAutoSwitch()
                val statusText = when {
                    activeProxy != null && activeProxy.isReachable && switched ->
                        "تم التبديل لبروكسي أسرع: ${activeProxy.server} (${activeProxy.latencyMs} ms)"
                    activeProxy != null && activeProxy.isReachable ->
                        "متصل ومستقر: ${activeProxy.server} • ${activeProxy.latencyMs} ms"
                    else ->
                        "جارٍ البحث عن بروكسي بديل من SoliSpirit..."
                }
                updateNotification(statusText, activeProxy)

                if (activeProxy == null || !activeProxy.isReachable) {
                    app.repository.syncAndVerify(maxTestCount = 50)
                }

                delay(3 * 60 * 1000L) // Check every 3 minutes
            }
        }
    }

    private fun updateNotification(contentText: String, activeProxy: ProxyEntity?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText, activeProxy))
    }

    private fun buildNotification(contentText: String, activeProxy: ProxyEntity?): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ProxyStabilityService::class.java).setAction(ACTION_SYNC_NOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, ProxyStabilityService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notif_monitor_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, "تحديث", syncIntent)
            .addAction(0, "إيقاف", stopIntent)

        if (activeProxy != null) {
            val tgPendingIntent = PendingIntent.getActivity(
                this,
                3,
                Intent(Intent.ACTION_VIEW, Uri.parse(activeProxy.tgLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "فتح تلغرام", tgPendingIntent)
        }

        return builder.build()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "teleproxy_stability_channel"
        const val NOTIFICATION_ID = 4091
        const val ACTION_STOP = "com.example.action.STOP_STABILITY"
        const val ACTION_SYNC_NOW = "com.example.action.SYNC_NOW"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notif_channel_sync_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = context.getString(R.string.notif_channel_sync_desc)
                        setShowBadge(false)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun start(context: Context) {
            ensureChannel(context)
            val intent = Intent(context, ProxyStabilityService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ProxyStabilityService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
