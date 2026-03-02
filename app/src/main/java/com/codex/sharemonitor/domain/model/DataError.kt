package com.codex.sharemonitor.domain.model

/**
 * 面向 UI 的错误类型（用于把底层异常映射为可读的中文提示）。
 *
 * 设计原则：
 * - UI 优先展示 [userMessage]；
 * - 具体异常栈不直接暴露到界面（仅用于日志/调试）。
 */
sealed interface DataError {
    val userMessage: String

    /** 网络不可用、连接超时等。 */
    data class Network(override val userMessage: String = "网络错误") : DataError
    /** 被限流/请求过于频繁。 */
    data class RateLimited(override val userMessage: String = "请求过于频繁") : DataError
    /** 数据格式变化或字段缺失导致解析失败。 */
    data class Parse(override val userMessage: String = "数据解析失败") : DataError
    /** 兜底错误。 */
    data class Unknown(override val userMessage: String = "未知错误") : DataError
}
