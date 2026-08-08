package com.example.browser

import com.example.data.SavedTab
import com.example.data.SavedTabRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounced persistence for tab metadata. WebView instances are never stored.
 */
class TabStatePersistence(
    private val repository: SavedTabRepository,
    private val scope: CoroutineScope
) {
    private var pendingJob: Job? = null

    fun scheduleSave(tabs: List<SavedTab>, delayMs: Long = 350L) {
        val snapshot = tabs.map { it.copy() }
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(delayMs)
            repository.replaceAll(snapshot)
        }
    }

    suspend fun flush(tabs: List<SavedTab>) {
        pendingJob?.cancel()
        repository.replaceAll(tabs.map { it.copy() })
    }

    fun cancel() {
        pendingJob?.cancel()
        pendingJob = null
    }
}
