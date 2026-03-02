package com.codex.sharemonitor.data.quotes.eastmoney

import retrofit2.http.GET
import retrofit2.http.Query

interface EastmoneySearchApi {
    @GET("api/suggest/get")
    suspend fun suggest(
        @Query("input") input: String,
        @Query("type") type: Int = 14,
        @Query("token") token: String = "EASTMONEY",
        @Query("count") count: Int = 20,
    ): EastmoneySuggestResponse
}

interface EastmoneyQuotesApi {
    @GET("api/qt/stock/get")
    suspend fun stockGet(
        @Query("secid") secId: String,
        @Query("fields") fields: String,
    ): EastmoneyStockGetResponse

    @GET("api/qt/clist/get")
    suspend fun clistGet(
        @Query("pn") page: Int = 1,
        @Query("pz") pageSize: Int = 200,
        @Query("po") po: Int = 1,
        @Query("np") np: Int = 1,
        @Query("fltt") fltt: Int = 2,
        @Query("invt") invt: Int = 2,
        @Query("fid") fid: String = "f3",
        @Query("fs") fs: String,
        @Query("fields") fields: String,
    ): EastmoneyClistResponse
}

interface EastmoneyHistoryApi {
    @GET("api/qt/stock/trends2/get")
    suspend fun trends2Get(
        @Query("secid") secId: String,
        @Query("ndays") ndays: Int = 1,
        @Query("fields1") fields1: String = "f1,f2,f3,f4,f5,f6",
        @Query("fields2") fields2: String = "f51,f52,f53,f54,f55,f56,f57,f58",
    ): EastmoneyTrends2Response

    @GET("api/qt/stock/kline/get")
    suspend fun klineGet(
        @Query("secid") secId: String,
        @Query("klt") klt: Int = 101,
        @Query("fqt") fqt: Int = 1,
        @Query("lmt") limit: Int = 180,
        @Query("fields1") fields1: String = "f1,f2,f3,f4,f5,f6",
        @Query("fields2") fields2: String = "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61",
    ): EastmoneyKlineResponse
}

