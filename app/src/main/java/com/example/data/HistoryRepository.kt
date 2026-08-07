package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<History>> = historyDao.getAllHistory()

    suspend fun insert(history: History) {
        historyDao.insert(history)
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}
