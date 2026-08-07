package com.example.data

class SavedTabRepository(private val savedTabDao: SavedTabDao) {
    suspend fun getAllSavedTabs(): List<SavedTab> = savedTabDao.getAllSavedTabs()

    suspend fun replaceAll(tabs: List<SavedTab>) {
        savedTabDao.replaceAll(tabs)
    }
}
