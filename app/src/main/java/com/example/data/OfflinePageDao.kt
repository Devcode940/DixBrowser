package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflinePageDao {
    @Query("SELECT * FROM offline_pages ORDER BY timestamp DESC")
    fun getAllOfflinePages(): Flow<List<OfflinePage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflinePage(page: OfflinePage)

    @Query("DELETE FROM offline_pages WHERE id = :id")
    suspend fun deleteOfflinePageById(id: Int)

    @Query("DELETE FROM offline_pages")
    suspend fun clearAllOfflinePages()
}
