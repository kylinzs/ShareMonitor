package com.codex.sharemonitor.data.watchlist

import com.codex.sharemonitor.domain.model.Exchange
import com.codex.sharemonitor.domain.model.Symbol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistRepositoryTest {
    @Test
    fun addSymbol_ignoresDuplicates() = runBlocking {
        val dao = FakeWatchlistDao()
        val repo = WatchlistRepository(dao)

        val s = Symbol(exchange = Exchange.SH, code = "600519", name = "贵州茅台")
        assertTrue(repo.addSymbol(s))
        assertFalse(repo.addSymbol(s))

        val entries = repo.observeEntries().first()
        assertEquals(1, entries.size)
        assertEquals(s.key, entries.first().symbol.key)
    }

    @Test
    fun pinned_itemsComeFirst() = runBlocking {
        val dao = FakeWatchlistDao()
        val repo = WatchlistRepository(dao)

        val a = Symbol(exchange = Exchange.SH, code = "600000", name = "浦发银行")
        val b = Symbol(exchange = Exchange.SZ, code = "000001", name = "平安银行")
        repo.addSymbol(a)
        repo.addSymbol(b)

        repo.setPinned(symbolKey = b.key, pinned = true)
        val entries = repo.observeEntries().first()
        assertEquals(b.key, entries.first().symbol.key)
    }

    @Test
    fun moveUpDown_changesOrderWithinPinnedGroup() = runBlocking {
        val dao = FakeWatchlistDao()
        val repo = WatchlistRepository(dao)

        val a = Symbol(exchange = Exchange.SH, code = "600000", name = "A")
        val b = Symbol(exchange = Exchange.SH, code = "600001", name = "B")
        val c = Symbol(exchange = Exchange.SH, code = "600002", name = "C")
        repo.addSymbol(a)
        repo.addSymbol(b)
        repo.addSymbol(c)

        repo.moveUp(c.key)
        var keys = repo.observeEntries().first().map { it.symbol.key }
        assertEquals(listOf(a.key, c.key, b.key), keys)

        repo.moveDown(a.key)
        keys = repo.observeEntries().first().map { it.symbol.key }
        assertEquals(listOf(c.key, a.key, b.key), keys)
    }

    private class FakeWatchlistDao : WatchlistDao {
        private val backing = MutableStateFlow<List<WatchlistEntity>>(emptyList())

        override fun observeAll(): Flow<List<WatchlistEntity>> = backing.map { sorted(it) }

        override suspend fun listAll(): List<WatchlistEntity> = sorted(backing.value)

        override suspend fun insertIgnore(entity: WatchlistEntity): Long {
            val cur = backing.value
            if (cur.any { it.symbolKey == entity.symbolKey }) return -1L
            val maxSort = cur.maxOfOrNull { it.sortOrder } ?: 0L
            val maxCreated = cur.maxOfOrNull { it.createdAtEpochMillis } ?: 0L
            backing.value = cur + entity.copy(
                sortOrder = maxOf(entity.sortOrder, maxSort + 1),
                createdAtEpochMillis = maxOf(entity.createdAtEpochMillis, maxCreated + 1),
            )
            return 1L
        }

        override suspend fun deleteByKey(symbolKey: String): Int {
            val cur = backing.value
            val next = cur.filterNot { it.symbolKey == symbolKey }
            backing.value = next
            return if (next.size != cur.size) 1 else 0
        }

        override suspend fun setPinned(symbolKey: String, pinned: Boolean): Int {
            backing.value = backing.value.map { if (it.symbolKey == symbolKey) it.copy(pinned = pinned) else it }
            return 1
        }

        override suspend fun setSortOrder(symbolKey: String, sortOrder: Long): Int {
            backing.value = backing.value.map { if (it.symbolKey == symbolKey) it.copy(sortOrder = sortOrder) else it }
            return 1
        }

        private fun sorted(list: List<WatchlistEntity>): List<WatchlistEntity> {
            return list.sortedWith(
                compareByDescending<WatchlistEntity> { it.pinned }
                    .thenBy { it.sortOrder }
                    .thenBy { it.createdAtEpochMillis }
            )
        }
    }
}
