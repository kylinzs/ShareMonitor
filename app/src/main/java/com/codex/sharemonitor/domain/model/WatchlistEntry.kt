package com.codex.sharemonitor.domain.model

/**
 * 自选条目（从本地数据库读取后映射到 UI 使用）。
 *
 * @property pinned 是否置顶（置顶与非置顶在排序/移动时分组处理）。
 * @property sortOrder 排序权重（越小越靠前；实现上通过调整数值来实现上移/下移）。
 * @property createdAtEpochMillis 创建时间（用于未来可能的排序/统计扩展）。
 */
data class WatchlistEntry(
    val symbol: Symbol,
    val pinned: Boolean,
    val sortOrder: Long,
    val createdAtEpochMillis: Long,
)
