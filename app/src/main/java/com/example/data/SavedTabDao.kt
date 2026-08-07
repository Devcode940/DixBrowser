package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SavedTabDao {
    @Query("SELECT * FROM saved_tabs ORDER BY orderIndex ASC")
    suspend fun getAllSavedTabs(): List<SavedTab>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<SavedTab>)

    @Query("DELETE FROM saved_tabs")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(tabs: List<SavedTab>) {
        clearAll()
        insertAll(tabs)
    }
}
