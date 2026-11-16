package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.ProxyRepository
import com.example.service.ProxyStabilityService
import com.example.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TeleProxyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settingsStore: SettingsDataStore
        private set

    lateinit var repository: ProxyRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        settingsStore = SettingsDataStore(this)
        repository = ProxyRepository(
            proxyDao = database.proxyDao(),
            settingsStore = settingsStore
        )

        ProxyStabilityService.ensureChannel(this)

        appScope.launch {
            val settings = settingsStore.getSnapshot()
            WorkScheduler.schedulePeriodicSync(
                context = this@TeleProxyApp,
                intervalMinutes = settings.syncIntervalMinutes,
                enabled = settings.autoSyncEnabled
            )
        }
    }
}
