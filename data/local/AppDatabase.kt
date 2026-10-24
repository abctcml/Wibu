package com.animapper.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<WatchHistory>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchHistory)
    @Query("DELETE FROM watch_history WHERE mediaId = :id")
    suspend fun delete(id: Int)
}

@Dao
interface MyListDao {
    @Query("SELECT * FROM my_list ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<MyListItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MyListItem)
    @Query("DELETE FROM my_list WHERE mediaId = :id")
    suspend fun delete(id: Int)
    @Query("SELECT EXISTS(SELECT 1 FROM my_list WHERE mediaId = :id)")
    suspend fun exists(id: Int): Boolean
}

@Database(entities = [WatchHistory::class, MyListItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun myListDao(): MyListDao
}
