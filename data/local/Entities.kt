package com.animapper.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val lastEpisode: Int,
    val progressMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "my_list")
data class MyListItem(
    @PrimaryKey val mediaId: Int,
    val title: String,
    val coverUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
)
