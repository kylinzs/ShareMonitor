package com.codex.sharemonitor.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * 设置仓储（DataStore）。
 *
 * 说明：
 * - 用于持久化数据源选择、自动刷新开关、刷新间隔等；
 * - 会对“不可用的数据源”做兜底（例如运行环境不支持时回退到 Mock）。
 */
class SettingsRepository private constructor(
    private val context: Context,
) {
    private object Keys {
        val dataSourceId: Preferences.Key<String> = stringPreferencesKey("quote_data_source_id")
        val autoRefreshEnabled: Preferences.Key<Boolean> = booleanPreferencesKey("auto_refresh_enabled")
        val refreshIntervalMinutes: Preferences.Key<Int> = intPreferencesKey("refresh_interval_minutes")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val storedId = prefs[Keys.dataSourceId]
        val requestedId = storedId?.let { runCatching { QuoteDataSourceId.valueOf(it) }.getOrNull() }
            ?: AppSettings.default.quoteDataSourceId
        val supported = supportedDataSources()
        val id = if (supported.contains(requestedId)) requestedId else supported.firstOrNull() ?: QuoteDataSourceId.Mock

        AppSettings(
            quoteDataSourceId = id,
            autoRefreshEnabled = prefs[Keys.autoRefreshEnabled] ?: AppSettings.default.autoRefreshEnabled,
            refreshIntervalMinutes = prefs[Keys.refreshIntervalMinutes]
                ?: AppSettings.default.refreshIntervalMinutes,
        )
    }

    fun supportedDataSources(): List<QuoteDataSourceId> {
        return listOf(
            QuoteDataSourceId.Eastmoney,
            QuoteDataSourceId.Tencent,
            QuoteDataSourceId.Mock,
        )
    }

    suspend fun setQuoteDataSourceId(id: QuoteDataSourceId) {
        context.dataStore.edit { prefs ->
            prefs[Keys.dataSourceId] = id.name
        }
    }

    suspend fun setAutoRefreshEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.autoRefreshEnabled] = enabled
        }
    }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.refreshIntervalMinutes] = minutes
        }
    }

    companion object {
        fun create(context: Context): SettingsRepository = SettingsRepository(context.applicationContext)
    }
}
