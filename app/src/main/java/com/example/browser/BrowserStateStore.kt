package com.example.browser

import com.example.data.SavedTab
import com.example.data.SavedTabRepository

/**
 * Persistence boundary for browser state. WebView instances never cross this
 * boundary; only serializable tab metadata is stored.
 */
class BrowserStateStore(
    private val repository: SavedTabRepository
) {
    suspend fun load(): List<SavedTab> = repository.getAllSavedTabs()

    suspend fun save(tabs: List<SavedTab>) {
        repository.replaceAll(tabs.map { it.copy() })
    }
}
