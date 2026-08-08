package com.example.browser

import android.net.Uri
import android.webkit.WebView

/** Validates and executes browser navigation at the engine boundary. */
object BrowserNavigation {
    /**
     * Validates a URL without touching WebView state.
     *
     * WHY: Validation must be testable independently from Android's rendering
     * engine and must reject non-web schemes before they reach WebView.
     */
    fun isAllowed(rawUrl: String, httpsOnly: Boolean): Boolean {
        val url = rawUrl.trim()
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        if (uri.host.isNullOrBlank()) return false
        return !httpsOnly || scheme == "https"
    }

    /** Loads a validated HTTP(S) URL into the supplied WebView. */
    fun load(webView: WebView, rawUrl: String, httpsOnly: Boolean): Boolean {
        if (!isAllowed(rawUrl, httpsOnly)) return false
        webView.loadUrl(rawUrl.trim())
        return true
    }
}
