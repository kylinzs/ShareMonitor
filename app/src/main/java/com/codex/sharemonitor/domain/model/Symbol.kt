package com.codex.sharemonitor.domain.model

/**
 * 证券标的（股票/指数等）的最小标识信息。
 *
 * @property exchange 交易所/市场（用于区分同代码在不同市场的含义）。
 * @property code 代码（如 600519）。
 * @property name 名称（用于展示；有些入口可能先用 code 占位，后续再补全）。
 */
data class Symbol(
    val exchange: Exchange,
    val code: String,
    val name: String,
) {
    /** 统一主键，便于缓存、数据库去重与路由传参。 */
    val key: String = "${exchange.name}:$code"
}
