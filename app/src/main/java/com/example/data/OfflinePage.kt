package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_pages")
data class OfflinePage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val htmlContent: String,
    val timestamp: Long = System.currentTimeMillis()
)
