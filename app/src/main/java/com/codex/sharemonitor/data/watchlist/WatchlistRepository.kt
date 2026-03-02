package com.codex.sharemonitor.data.watchlist

import com.codex.sharemonitor.domain.model.Exchange
import com.codex.sharemonitor.domain.model.Symbol
import com.codex.sharemonitor.domain.model.WatchlistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 自选仓储（Room DAO 的轻封装）。
 *
 * 说明：
 * - 负责把数据库实体映射为 domain model；
 * - 插入时用 `symbolKey` 做唯一约束（重复添加返回 false）；
 * - 排序使用 `sortOrder` 数值，置顶与非置顶在移动时保持分组不交叉。
 */
class WatchlistRepository(
    private val watchlistDao: WatchlistDao,
) {
    fun observeEntries(): Flow<List<WatchlistEntry>> {
        return watchlistDao.observeAll().map { entities ->
            entities.mapNotNull { e ->
                val exchange = runCatching { Exchange.valueOf(e.exchange) }.getOrNull() ?: return@mapNotNull null
                WatchlistEntry(
                    symbol = Symbol(exchange = exchange, code = e.code, name = e.name),
                    pinned = e.pinned,
                    sortOrder = e.sortOrder,
                    createdAtEpochMillis = e.createdAtEpochMillis,
                )
            }
        }
    }

    fun observeSymbols(): Flow<List<Symbol>> {
        return watchlistDao.observeAll().map { entities ->
            entities.map { e ->
                val exchange = runCatching { Exchange.valueOf(e.exchange) }.getOrNull() ?: Exchange.SH
                Symbol(exchange = exchange, code = e.code, name = e.name)
            }
        }
    }

    suspend fun addSymbol(symbol: Symbol, pinned: Boolean = false): Boolean {
        val id = watchlistDao.insertIgnore(
            WatchlistEntity(
                symbolKey = symbol.key,
                exchange = symbol.exchange.name,
                code = symbol.code,
                name = symbol.name,
                pinned = pinned,
                sortOrder = System.currentTimeMillis(),
                createdAtEpochMillis = System.currentTimeMillis(),
            )
        )
        return id != -1L
    }

    suspend fun removeSymbol(symbolKey: String) {
        watchlistDao.deleteByKey(symbolKey)
    }

    suspend fun setPinned(symbolKey: String, pinned: Boolean) {
        watchlistDao.setPinned(symbolKey, pinned)
    }

    suspend fun setSortOrder(symbolKey: String, sortOrder: Long) {
        watchlistDao.setSortOrder(symbolKey, sortOrder)
    }

    suspend fun moveUp(symbolKey: String) {
        val list = watchlistDao.listAll()
        val index = list.indexOfFirst { it.symbolKey == symbolKey }
        if (index <= 0) return
        val current = list[index]
        val prev = list[index - 1]
        if (current.pinned != prev.pinned) return
        watchlistDao.setSortOrder(symbolKey = symbolKey, sortOrder = prev.sortOrder - 1)
    }

    suspend fun moveDown(symbolKey: String) {
        val list = watchlistDao.listAll()
        val index = list.indexOfFirst { it.symbolKey == symbolKey }
        if (index < 0 || index >= list.lastIndex) return
        val current = list[index]
        val next = list[index + 1]
        if (current.pinned != next.pinned) return
        watchlistDao.setSortOrder(symbolKey = symbolKey, sortOrder = next.sortOrder + 1)
    }
}
