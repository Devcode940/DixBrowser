package com.example.security

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Owns the privacy boundary for an incognito session. Nothing from this object
 * is persisted to Room/DataStore. Call clear() when the private session ends.
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
                webView.clearHistory()
                webView.clearCache(true)
                webView.clearFormData()
                webView.clearSslPreferences()
                webView.loadUrl("about:blank")
            }
        }

        runCatching { CookieManager.getInstance().removeSessionCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
        runCatching { WebStorage.getInstance().deleteAllData() }
        webViews.clear()
    }
}
