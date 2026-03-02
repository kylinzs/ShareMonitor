package com.codex.sharemonitor.data.quotes.tencent

import com.squareup.moshi.Json

data class TencentMinuteResponse(
    @field:Json(name = "code") val code: Int?,
    @field:Json(name = "msg") val msg: String?,
    @field:Json(name = "data") val data: Map<String, TencentMinuteStock>?,
)

data class TencentMinuteStock(
    @field:Json(name = "data") val data: TencentMinuteStockData?,
)

data class TencentMinuteStockData(
    @field:Json(name = "data") val data: List<String>?,
)

data class TencentFqKlineResponse(
    @field:Json(name = "code") val code: Int?,
    @field:Json(name = "msg") val msg: String?,
    @field:Json(name = "data") val data: Map<String, TencentFqKlineStock>?,
)

data class TencentFqKlineStock(
    @field:Json(name = "qfqday") val qfqDay: List<List<String>>?,
    @field:Json(name = "day") val day: List<List<String>>?,
)

