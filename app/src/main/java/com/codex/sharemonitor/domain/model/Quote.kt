package com.codex.sharemonitor.domain.model

/**
 * 行情报价（已做字段归一化，供 UI 直接展示）。
 *
 * 注意：不同数据源对“时间/成交量”等字段含义可能略有差异，本模型以“尽量统一展示”为目标。
 *
 * @property symbolKey 统一的标识键，格式为 `EXCHANGE:CODE`（见 [Symbol.key]）。
 * @property lastPrice 最新价（可能为 null）。
 * @property change 涨跌额（可能为 null）。
 * @property changePct 涨跌幅（百分比，可能为 null）。
 * @property open 今日开盘价（可能为 null）。
 * @property high 今日最高价（可能为 null）。
 * @property low 今日最低价（可能为 null）。
 * @property prevClose 昨收（可能为 null）。
 * @property volume 成交量（可能为 null；各数据源单位可能不同，仅用于展示）。
 * @property quoteTimeEpochMillis 行情本身的时间戳（毫秒）；若数据源未提供则为 null。
 * @property fetchedAtEpochMillis 本次拉取/缓存写入时刻（毫秒），用于“更新于”与过期判断。
 * @property sourceName 数据来源名称（用于 UI 提示/排障）。
 */
data class Quote(
    val symbolKey: String,
    val lastPrice: Double?,
    val change: Double?,
    val changePct: Double?,
    val open: Double?,
    val high: Double?,
    val low: Double?,
    val prevClose: Double?,
    val volume: Long?,
    val quoteTimeEpochMillis: Long?,
    val fetchedAtEpochMillis: Long,
    val sourceName: String,
)
