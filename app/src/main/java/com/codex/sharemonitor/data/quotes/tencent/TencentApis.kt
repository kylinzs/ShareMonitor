package com.codex.sharemonitor.data.quotes.tencent

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface TencentMinuteApi {
    @GET("appstock/app/minute/query")
    suspend fun minuteQuery(
        @Query("code") code: String,
    ): TencentMinuteResponse
}

interface TencentFqKlineApi {
    @GET("appstock/app/fqkline/get")
    suspend fun fqKlineGet(
        @Query("param") param: String,
    ): TencentFqKlineResponse
}

interface TencentQtQuoteApi {
    @GET("q")
    suspend fun quote(
        @Query("q") codes: String,
    ): ResponseBody
}

interface TencentSmartboxApi {
    @GET("s3/")
    suspend fun suggest(
        @Query("q") query: String,
        @Query("t") type: String = "all",
        @Query("v") version: Int = 2,
        @Query("c") c: Int = 1,
    ): ResponseBody
}
