package com.codex.sharemonitor.data.quotes.eastmoney

import com.squareup.moshi.Json

data class EastmoneySuggestResponse(
    @field:Json(name = "QuotationCodeTable") val quotationCodeTable: EastmoneySuggestTable?,
    @field:Json(name = "quotationCodeTable") val quotationCodeTableLower: EastmoneySuggestTable? = null,
)

data class EastmoneySuggestTable(
    @field:Json(name = "Data") val data: List<EastmoneySuggestItem>?,
    @field:Json(name = "data") val dataLower: List<EastmoneySuggestItem>? = null,
)

data class EastmoneySuggestItem(
    @field:Json(name = "QuoteID") val quoteId: String?,
    @field:Json(name = "QuoteId") val quoteIdAlt: String? = null,
    @field:Json(name = "quoteId") val quoteIdAlt2: String? = null,
    @field:Json(name = "Code") val code: String?,
    @field:Json(name = "code") val codeLower: String? = null,
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "name") val nameLower: String? = null,
)

data class EastmoneyStockGetResponse(
    @field:Json(name = "data") val data: EastmoneyStockData?,
)

data class EastmoneyStockData(
    @field:Json(name = "f43") val lastPrice: Double?,
    @field:Json(name = "f44") val high: Double?,
    @field:Json(name = "f45") val low: Double?,
    @field:Json(name = "f46") val open: Double?,
    @field:Json(name = "f47") val volume: Long?,
    @field:Json(name = "f57") val code: String?,
    @field:Json(name = "f58") val name: String?,
    @field:Json(name = "f60") val prevClose: Double?,
    @field:Json(name = "f169") val change: Double?,
    @field:Json(name = "f170") val changePct: Double?,
    @field:Json(name = "f171") val quoteTimeEpochMillis: Long?,
)

data class EastmoneyClistResponse(
    @field:Json(name = "data") val data: EastmoneyClistData?,
)

data class EastmoneyClistData(
    @field:Json(name = "diff") val diff: List<EastmoneyClistItem>?,
)

data class EastmoneyClistItem(
    @field:Json(name = "f12") val code: String?,
    @field:Json(name = "f13") val market: Int?,
    @field:Json(name = "f14") val name: String?,
    @field:Json(name = "f2") val lastPrice: Double?,
    @field:Json(name = "f3") val changePct: Double?,
    @field:Json(name = "f4") val change: Double?,
    @field:Json(name = "f124") val quoteTimeEpochSeconds: Long?,
)

data class EastmoneyTrends2Response(
    @field:Json(name = "data") val data: EastmoneyTrends2Data?,
)

data class EastmoneyTrends2Data(
    @field:Json(name = "trends") val trends: List<String>?,
)

data class EastmoneyKlineResponse(
    @field:Json(name = "data") val data: EastmoneyKlineData?,
)

data class EastmoneyKlineData(
    @field:Json(name = "klines") val klines: List<String>?,
)
