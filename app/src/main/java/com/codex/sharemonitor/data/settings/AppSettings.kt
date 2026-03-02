package com.codex.sharemonitor.data.settings

data class AppSettings(
    val quoteDataSourceId: QuoteDataSourceId,
    val autoRefreshEnabled: Boolean,
    val refreshIntervalMinutes: Int,
) {
    companion object {
        const val DEFAULT_REFRESH_INTERVAL_MINUTES: Int = 15
        val default: AppSettings = AppSettings(
            quoteDataSourceId = QuoteDataSourceId.Eastmoney,
            autoRefreshEnabled = false,
            refreshIntervalMinutes = DEFAULT_REFRESH_INTERVAL_MINUTES,
        )
    }
}

