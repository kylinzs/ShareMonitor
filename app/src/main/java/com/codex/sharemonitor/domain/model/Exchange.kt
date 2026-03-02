package com.codex.sharemonitor.domain.model

/**
 * 交易所/市场枚举。
 *
 * 说明：用于区分相同代码在不同市场下的含义，并用于生成统一主键（见 [Symbol.key]）。
 */
enum class Exchange {
    SH,
    SZ,
    BJ,
}
