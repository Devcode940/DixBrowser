package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_credentials")
data class PasswordCredential(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val siteTitle: String,
    val domain: String,
    val username: String,
    val encryptedPassword: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
