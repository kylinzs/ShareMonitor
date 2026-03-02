package com.codex.sharemonitor.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * OkHttpClient 工厂。
 *
 * 说明：
 * - 统一设置超时、UA、以及部分站点对 Referer 的要求；
 * - debug 模式下开启基础日志（BASIC）便于排障；
 * - 这里的请求头策略主要用于提高公开接口的兼容性与成功率。
 */
object HttpClientFactory {
    fun create(debug: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        if (debug) {
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            builder.addInterceptor(logging)
        }

        builder.addInterceptor { chain ->
            val original = chain.request()
            val host = original.url.host
            val requestBuilder = original.newBuilder()
                .header("User-Agent", "ShareMonitor/0.1")

            if (host.endsWith("eastmoney.com")) {
                // Eastmoney 相关接口在部分情况下会校验 Referer。
                requestBuilder.header("Referer", "https://quote.eastmoney.com/")
            }
            if (host.endsWith("gtimg.cn")) {
                // Tencent 相关接口在部分情况下会校验 Referer。
                requestBuilder.header("Referer", "https://qt.qq.com/")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }

        return builder.build()
    }
}
