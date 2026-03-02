package com.codex.sharemonitor.domain.model

/**
 * 指数信息（主要用于“行情-指数”列表展示）。
 *
 * @property secId 数据源侧的市场+代码标识（例如 `1.000001`），用于后续映射到 [Symbol]。
 */
data class Index(
    val secId: String,
    val code: String,
    val name: String,
)

/**
 * 板块信息（主要用于“行情-板块”列表展示）。
 *
 * 说明：板块行情由数据源返回时可能不包含全部字段，故各数值字段允许为空。
 */
data class Sector(
    val id: String,
    val name: String,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val changePct: Double? = null,
    val quoteTimeEpochMillis: Long? = null,
)

/**
 * 板块成分股条目。
 *
 * @property quote 可能为空：通常需要额外批量拉取报价后才能展示。
 */
data class Constituent(
    val symbol: Symbol,
    val quote: Quote? = null,
)
