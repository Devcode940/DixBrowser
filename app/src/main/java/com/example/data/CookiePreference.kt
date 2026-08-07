package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookie_preferences")
data class CookiePreference(
    @PrimaryKey
    val domain: String,
    val allowFirstParty: Boolean = true,
    val allowThirdParty: Boolean = false
)
