package com.example.security

import android.webkit.WebView

/**
 * Lifecycle helper for a private WebView session.
 *
 * IMPORTANT: true cookie/storage isolation requires the WebView to run in a
 * dedicated process/data directory. This class only owns WebView cleanup; it
 * deliberately does not clear global CookieManager/WebStorage state.
 */
class PrivateBrowsingSession {
    private val webViews = LinkedHashSet<WebView>()
    private var closed = false

    fun register(webView: WebView) {
        check(!closed) { "Private browsing session is closed" }
        webViews += webView
        WebViewSecurityPolicy.configure(
            webView = webView,
            incognito = true,
            javaScriptEnabled = webView.settings.javaScriptEnabled,
            allowThirdPartyCookies = false
        )
    }

    fun clear() {
        if (closed) return
        closed = true
        webViews.forEach { webView ->
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.clearFormData()
                webView.clearSslPreferences()
            }
        }
        webViews.clear()
    }
}
