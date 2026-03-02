package com.codex.sharemonitor.data.watchlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbolKey: String,
    val exchange: String,
    val code: String,
    val name: String,
    val pinned: Boolean,
    val sortOrder: Long,
    val createdAtEpochMillis: Long,
)

