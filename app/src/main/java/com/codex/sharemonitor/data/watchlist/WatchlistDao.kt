package com.codex.sharemonitor.data.watchlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY pinned DESC, sortOrder ASC, createdAtEpochMillis ASC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY pinned DESC, sortOrder ASC, createdAtEpochMillis ASC")
    suspend fun listAll(): List<WatchlistEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: WatchlistEntity): Long

    @Query("DELETE FROM watchlist WHERE symbolKey = :symbolKey")
    suspend fun deleteByKey(symbolKey: String): Int

    @Query("UPDATE watchlist SET pinned = :pinned WHERE symbolKey = :symbolKey")
    suspend fun setPinned(symbolKey: String, pinned: Boolean): Int

    @Query("UPDATE watchlist SET sortOrder = :sortOrder WHERE symbolKey = :symbolKey")
    suspend fun setSortOrder(symbolKey: String, sortOrder: Long): Int
}
