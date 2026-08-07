package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordCredentialDao {
    @Query("SELECT * FROM password_credentials ORDER BY domain ASC, username ASC")
    fun getAllCredentials(): Flow<List<PasswordCredential>>

    @Query("SELECT * FROM password_credentials WHERE domain LIKE '%' || :domain || '%' OR siteTitle LIKE '%' || :domain || '%'")
    suspend fun getCredentialsForDomain(domain: String): List<PasswordCredential>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: PasswordCredential)

    @Update
    suspend fun updateCredential(credential: PasswordCredential)

    @Query("DELETE FROM password_credentials WHERE id = :id")
    suspend fun deleteCredentialById(id: Int)

    @Query("DELETE FROM password_credentials")
    suspend fun clearAll()
}
