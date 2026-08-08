package com.example.browser

import android.net.Uri

/**
 * Coordinates browser commands without exposing WebView details to Compose.
 */
class BrowserCoordinator(
    private val tabManager: BrowserTabManager,
    private val stateStore: BrowserStateStore
) {
    suspend fun restore(): List<BrowserTabState> {
        return stateStore.load()
            .filter { it.url.isBlank() || it.url == "about:blank" || isHttpUrl(it.url) }
            .sortedBy { it.orderIndex }
            .map {
                tabManager.createTab(
                    url = it.url,
                    title = it.title,
                    private = false
                )
            }
    }

    suspend fun persist(tabs: List<BrowserTabState>) {
        stateStore.save(
            tabs.mapIndexed { index, tab ->
                com.example.data.SavedTab(
                    id = tab.id,
                    url = tab.url,
                    title = tab.title,
                    isHome = tab.url == "about:blank",
                    scrollY = tab.webView?.scrollY ?: 0,
                    orderIndex = index,
                    isActive = false
                )
            }
        )
    }

    fun close(tabId: String) = tabManager.closeTab(tabId)

    private fun isHttpUrl(value: String): Boolean {
        val scheme = Uri.parse(value).scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
