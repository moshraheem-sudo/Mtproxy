package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "teleproxy_settings")

data class ProxySettings(
    val activeProxyId: String? = null,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Long = 30L,
    val autoFailoverEnabled: Boolean = true,
    val stabilityServiceEnabled: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncStatus: String = "جاهز للمزامنة الأولى",
    val lastSyncCount: Int = 0,
    val totalUsersCount: Int = 0,
    val latestAvailableVersion: String? = null,
    val latestUpdateUrl: String? = null,
    val latestReleaseNotes: String? = null,
    val lastUpdateCheckAt: Long = 0L
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ACTIVE_PROXY_ID = stringPreferencesKey("active_proxy_id")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL_MINUTES = longPreferencesKey("sync_interval_minutes")
        val AUTO_FAILOVER_ENABLED = booleanPreferencesKey("auto_failover_enabled")
        val STABILITY_SERVICE_ENABLED = booleanPreferencesKey("stability_service_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_STATUS = stringPreferencesKey("last_sync_status")
        val LAST_SYNC_COUNT = intPreferencesKey("last_sync_count")
        val TOTAL_USERS_COUNT = intPreferencesKey("total_users_count")
        val LATEST_AVAILABLE_VERSION = stringPreferencesKey("latest_available_version")
        val LATEST_UPDATE_URL = stringPreferencesKey("latest_update_url")
        val LATEST_RELEASE_NOTES = stringPreferencesKey("latest_release_notes")
        val LAST_UPDATE_CHECK_AT = longPreferencesKey("last_update_check_at")
    }

    val settingsFlow: Flow<ProxySettings> = context.dataStore.data.map { prefs ->
        ProxySettings(
            activeProxyId = prefs[Keys.ACTIVE_PROXY_ID],
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            syncIntervalMinutes = prefs[Keys.SYNC_INTERVAL_MINUTES] ?: 30L,
            autoFailoverEnabled = prefs[Keys.AUTO_FAILOVER_ENABLED] ?: true,
            stabilityServiceEnabled = prefs[Keys.STABILITY_SERVICE_ENABLED] ?: false,
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L,
            lastSyncStatus = prefs[Keys.LAST_SYNC_STATUS] ?: "جاهز للمزامنة الأولى",
            lastSyncCount = prefs[Keys.LAST_SYNC_COUNT] ?: 0,
            totalUsersCount = prefs[Keys.TOTAL_USERS_COUNT] ?: 0,
            latestAvailableVersion = prefs[Keys.LATEST_AVAILABLE_VERSION],
            latestUpdateUrl = prefs[Keys.LATEST_UPDATE_URL],
            latestReleaseNotes = prefs[Keys.LATEST_RELEASE_NOTES],
            lastUpdateCheckAt = prefs[Keys.LAST_UPDATE_CHECK_AT] ?: 0L
        )
    }

    suspend fun getSnapshot(): ProxySettings = settingsFlow.first()

    suspend fun setActiveProxyId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.ACTIVE_PROXY_ID)
            else prefs[Keys.ACTIVE_PROXY_ID] = id
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SYNC_ENABLED] = enabled }
    }

    suspend fun setSyncIntervalMinutes(minutes: Long) {
        context.dataStore.edit { it[Keys.SYNC_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setAutoFailoverEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_FAILOVER_ENABLED] = enabled }
    }

    suspend fun setStabilityServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STABILITY_SERVICE_ENABLED] = enabled }
    }

    suspend fun updateSyncMetadata(timestamp: Long, status: String, count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp
            prefs[Keys.LAST_SYNC_STATUS] = status
            prefs[Keys.LAST_SYNC_COUNT] = count
        }
    }

    suspend fun updateAppRemoteInfo(
        totalUsersCount: Int,
        latestVersion: String?,
        updateUrl: String?,
        releaseNotes: String?,
        checkedAt: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOTAL_USERS_COUNT] = totalUsersCount
            prefs[Keys.LAST_UPDATE_CHECK_AT] = checkedAt
            if (latestVersion.isNullOrBlank()) {
                prefs.remove(Keys.LATEST_AVAILABLE_VERSION)
                prefs.remove(Keys.LATEST_UPDATE_URL)
                prefs.remove(Keys.LATEST_RELEASE_NOTES)
            } else {
                prefs[Keys.LATEST_AVAILABLE_VERSION] = latestVersion
                if (updateUrl != null) prefs[Keys.LATEST_UPDATE_URL] = updateUrl
                else prefs.remove(Keys.LATEST_UPDATE_URL)
                if (releaseNotes != null) prefs[Keys.LATEST_RELEASE_NOTES] = releaseNotes
                else prefs.remove(Keys.LATEST_RELEASE_NOTES)
            }
        }
    }
}
