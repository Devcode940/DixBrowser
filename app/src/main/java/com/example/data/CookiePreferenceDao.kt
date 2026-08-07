package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CookiePreferenceDao {
    @Query("SELECT * FROM cookie_preferences WHERE domain = :domain")
    suspend fun getPreference(domain: String): CookiePreference?

    @Query("SELECT * FROM cookie_preferences")
    fun getAllPreferences(): Flow<List<CookiePreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: CookiePreference)
    
    @Query("DELETE FROM cookie_preferences WHERE domain = :domain")
    suspend fun deletePreference(domain: String)
}
