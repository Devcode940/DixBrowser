package com.example.browser

import android.webkit.WebView
import java.util.UUID

/**
 * Owns browser-tab identity and lifecycle instead of Compose state owning the
 * browser engine directly.
 */
class BrowserTabManager(
    private val controller: BrowserWebViewController
) {
    private val tabs = LinkedHashMap<String, BrowserTabState>()

    val allTabs: List<BrowserTabState>
        get() = tabs.values.toList()

    fun createTab(
        url: String = "about:blank",
        title: String = "New Tab",
        private: Boolean = false
    ): BrowserTabState {
        val tab = BrowserTabState(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            isPrivate = private
        )
        tabs[tab.id] = tab
        return tab
    }

    fun attachWebView(tabId: String, webView: WebView) {
        val tab = tabs[tabId] ?: error("Unknown tab: $tabId")
        tab.webView?.let(controller::destroy)
        controller.configure(webView, incognito = tab.isPrivate)
        tab.webView = webView
    }

    fun closeTab(tabId: String) {
        tabs.remove(tabId)?.webView?.let(controller::destroy)
    }

    fun closeAll() {
        tabs.values.forEach { it.webView?.let(controller::destroy) }
        tabs.clear()
    }
}

data class BrowserTabState(
    val id: String,
    var url: String,
    var title: String,
    val isPrivate: Boolean,
    var webView: WebView? = null
)
