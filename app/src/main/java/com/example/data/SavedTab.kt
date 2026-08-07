package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_tabs")
data class SavedTab(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val isHome: Boolean,
    val scrollY: Int,
    val orderIndex: Int,
    val isActive: Boolean
)
