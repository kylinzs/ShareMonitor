package com.codex.sharemonitor.data

import android.content.Context
import android.content.pm.ApplicationInfo
import com.codex.sharemonitor.data.network.HttpClientFactory
import com.codex.sharemonitor.data.quotes.MockQuotesDataSource
import com.codex.sharemonitor.data.quotes.QuotesRepository
import com.codex.sharemonitor.data.quotes.eastmoney.EastmoneyHistoryApi
import com.codex.sharemonitor.data.quotes.eastmoney.EastmoneyQuotesApi
import com.codex.sharemonitor.data.quotes.eastmoney.EastmoneyQuotesDataSource
import com.codex.sharemonitor.data.quotes.eastmoney.EastmoneySearchApi
import com.codex.sharemonitor.data.quotes.tencent.TencentFqKlineApi
import com.codex.sharemonitor.data.quotes.tencent.TencentMinuteApi
import com.codex.sharemonitor.data.quotes.tencent.TencentQuotesDataSource
import com.codex.sharemonitor.data.quotes.tencent.TencentQtQuoteApi
import com.codex.sharemonitor.data.quotes.tencent.TencentSmartboxApi
import com.codex.sharemonitor.data.settings.SettingsRepository
import com.codex.sharemonitor.data.watchlist.AppDatabase
import com.codex.sharemonitor.data.watchlist.WatchlistRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * 应用级依赖容器（简易 DI）。
 *
 * 说明：
 * - 统一构建网络、数据库、Repository、各数据源实例；
 * - 使用 `lazy` 控制初始化时机，避免启动阶段做过多工作；
 * - debug 开关来自 ApplicationInfo，用于网络日志等调试能力。
 */
class AppContainer(appContext: Context) {
    private val context = appContext.applicationContext
    private val isDebug: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private val database: AppDatabase by lazy { AppDatabase.create(context) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository.create(context) }
    val watchlistRepository: WatchlistRepository by lazy {
        WatchlistRepository(watchlistDao = database.watchlistDao())
    }

    private val okHttpClient by lazy { HttpClientFactory.create(debug = isDebug) }
    private val moshi: Moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    private val mockQuotesDataSource: MockQuotesDataSource by lazy { MockQuotesDataSource() }
    private val eastmoneyQuotesDataSource: EastmoneyQuotesDataSource by lazy {
        val searchRetrofit = Retrofit.Builder()
            .baseUrl("https://searchapi.eastmoney.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val quotesRetrofit = Retrofit.Builder()
            .baseUrl("https://push2.eastmoney.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val historyRetrofit = Retrofit.Builder()
            .baseUrl("https://push2his.eastmoney.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        EastmoneyQuotesDataSource(
            searchApi = searchRetrofit.create(EastmoneySearchApi::class.java),
            quotesApi = quotesRetrofit.create(EastmoneyQuotesApi::class.java),
            historyApi = historyRetrofit.create(EastmoneyHistoryApi::class.java),
        )
    }
    private val tencentQuotesDataSource: TencentQuotesDataSource by lazy {
        val apiRetrofit = Retrofit.Builder()
            .baseUrl("https://web.ifzq.gtimg.cn/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val qtRetrofit = Retrofit.Builder()
            .baseUrl("http://qt.gtimg.cn/")
            .client(okHttpClient)
            .build()

        val smartboxRetrofit = Retrofit.Builder()
            .baseUrl("https://smartbox.gtimg.cn/")
            .client(okHttpClient)
            .build()

        TencentQuotesDataSource(
            minuteApi = apiRetrofit.create(TencentMinuteApi::class.java),
            fqKlineApi = apiRetrofit.create(TencentFqKlineApi::class.java),
            qtQuoteApi = qtRetrofit.create(TencentQtQuoteApi::class.java),
            smartboxApi = smartboxRetrofit.create(TencentSmartboxApi::class.java),
        )
    }
    val quotesRepository: QuotesRepository by lazy {
        QuotesRepository(
            settingsRepository = settingsRepository,
            watchlistRepository = watchlistRepository,
            mockQuotesDataSource = mockQuotesDataSource,
            eastmoneyQuotesDataSource = eastmoneyQuotesDataSource,
            tencentQuotesDataSource = tencentQuotesDataSource,
        )
    }
}
