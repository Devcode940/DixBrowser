package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Bookmark::class, History::class, SavedTab::class, OfflinePage::class, PasswordCredential::class, CookiePreference::class], version = 6, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun savedTabDao(): SavedTabDao
    abstract fun offlinePageDao(): OfflinePageDao
    abstract fun passwordCredentialDao(): PasswordCredentialDao
    abstract fun cookiePreferenceDao(): CookiePreferenceDao

    companion object {
        @Volatile
        private var Instance: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, BrowserDatabase::class.java, "browser_database")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
