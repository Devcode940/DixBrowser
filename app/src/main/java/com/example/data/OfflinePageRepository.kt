package com.example.data

import kotlinx.coroutines.flow.Flow

class OfflinePageRepository(private val offlinePageDao: OfflinePageDao) {
    val allOfflinePages: Flow<List<OfflinePage>> = offlinePageDao.getAllOfflinePages()

    suspend fun savePage(title: String, url: String, htmlContent: String) {
        offlinePageDao.insertOfflinePage(
            OfflinePage(
                title = title,
                url = url,
                htmlContent = htmlContent
            )
        )
    }

    suspend fun deleteById(id: Int) {
        offlinePageDao.deleteOfflinePageById(id)
    }

    suspend fun clearAll() {
        offlinePageDao.clearAllOfflinePages()
    }
}
