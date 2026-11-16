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

enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", true),
    ENGLISH("en", "English", false),
    KURDISH("ckb", "کوردی", true),
    PERSIAN("fa", "فارسی", true);

    companion object {
        fun fromCode(code: String?): AppLanguage = entries.firstOrNull { it.code == code } ?: ARABIC
    }
}

object SyncStateCode {
    const val IDLE = "IDLE"
    const val WORKING = "WORKING"
    const val NONE_WORKING = "NONE_WORKING"
    const val ERROR = "ERROR"
}

data class ProxySettings(
    val activeProxyId: String? = null,
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Long = 30L,
    val autoFailoverEnabled: Boolean = true,
    val stabilityServiceEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ARABIC,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncStateCode: String = SyncStateCode.IDLE,
    val lastSyncWorkingCount: Int = 0,
    val lastSyncCount: Int = 0,
    val lastSyncErrorDetail: String = "",
    val latestAvailableVersion: String? = null,
    val latestUpdateUrl: String? = null,
    val latestApkAssetUrl: String? = null,
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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_SYNC_STATE_CODE = stringPreferencesKey("last_sync_state_code")
        val LAST_SYNC_WORKING_COUNT = intPreferencesKey("last_sync_working_count")
        val LAST_SYNC_COUNT = intPreferencesKey("last_sync_count")
        val LAST_SYNC_ERROR_DETAIL = stringPreferencesKey("last_sync_error_detail")
        val LATEST_AVAILABLE_VERSION = stringPreferencesKey("latest_available_version")
        val LATEST_UPDATE_URL = stringPreferencesKey("latest_update_url")
        val LATEST_APK_ASSET_URL = stringPreferencesKey("latest_apk_asset_url")
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
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            appLanguage = AppLanguage.fromCode(prefs[Keys.APP_LANGUAGE]),
            lastSyncTimestamp = prefs[Keys.LAST_SYNC_TIMESTAMP] ?: 0L,
            lastSyncStateCode = prefs[Keys.LAST_SYNC_STATE_CODE] ?: SyncStateCode.IDLE,
            lastSyncWorkingCount = prefs[Keys.LAST_SYNC_WORKING_COUNT] ?: 0,
            lastSyncCount = prefs[Keys.LAST_SYNC_COUNT] ?: 0,
            lastSyncErrorDetail = prefs[Keys.LAST_SYNC_ERROR_DETAIL].orEmpty(),
            latestAvailableVersion = prefs[Keys.LATEST_AVAILABLE_VERSION],
            latestUpdateUrl = prefs[Keys.LATEST_UPDATE_URL],
            latestApkAssetUrl = prefs[Keys.LATEST_APK_ASSET_URL],
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

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.code }
    }

    suspend fun updateSyncMetadata(
        timestamp: Long,
        stateCode: String,
        workingCount: Int,
        totalCount: Int,
        errorDetail: String = ""
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNC_TIMESTAMP] = timestamp
            prefs[Keys.LAST_SYNC_STATE_CODE] = stateCode
            prefs[Keys.LAST_SYNC_WORKING_COUNT] = workingCount
            prefs[Keys.LAST_SYNC_COUNT] = totalCount
            prefs[Keys.LAST_SYNC_ERROR_DETAIL] = errorDetail
        }
    }

    suspend fun updateAppRemoteInfo(
        latestVersion: String?,
        updateUrl: String?,
        apkAssetUrl: String?,
        releaseNotes: String?,
        checkedAt: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_UPDATE_CHECK_AT] = checkedAt
            if (latestVersion.isNullOrBlank()) {
                prefs.remove(Keys.LATEST_AVAILABLE_VERSION)
                prefs.remove(Keys.LATEST_UPDATE_URL)
                prefs.remove(Keys.LATEST_APK_ASSET_URL)
                prefs.remove(Keys.LATEST_RELEASE_NOTES)
            } else {
                prefs[Keys.LATEST_AVAILABLE_VERSION] = latestVersion
                if (updateUrl != null) prefs[Keys.LATEST_UPDATE_URL] = updateUrl
                else prefs.remove(Keys.LATEST_UPDATE_URL)
                if (apkAssetUrl != null) prefs[Keys.LATEST_APK_ASSET_URL] = apkAssetUrl
                else prefs.remove(Keys.LATEST_APK_ASSET_URL)
                if (releaseNotes != null) prefs[Keys.LATEST_RELEASE_NOTES] = releaseNotes
                else prefs.remove(Keys.LATEST_RELEASE_NOTES)
            }
        }
    }
}
