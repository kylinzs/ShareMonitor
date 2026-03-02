package com.codex.sharemonitor.domain.model

/**
 * 历史走势类型。
 *
 * - [Intraday]：分时（通常为当日分钟级序列，受数据源与交易日影响）。
 * - [DailyK]：日 K（通常覆盖最近 6 个月左右，受数据源限制）。
 */
enum class HistoryType {
    Intraday,
    DailyK,
}

/**
 * 单个时间点的历史数据。
 *
 * 约定：分时只需要 close；日 K 通常提供 OHLC 与成交量。
 */
data class HistoryPoint(
    val epochMillis: Long,
    val close: Double,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val volume: Long? = null,
)

/**
 * 某个标的在指定 [HistoryType] 下的时间序列。
 *
 * @property fetchedAtEpochMillis 拉取并写入缓存的时间，用于“更新于”或过期判断。
 * @property sourceName 数据来源名称，便于 UI 提示与排障。
 */
data class HistorySeries(
    val type: HistoryType,
    val points: List<HistoryPoint>,
    val fetchedAtEpochMillis: Long,
    val sourceName: String,
)
