package com.example.browser

import android.net.Uri
import android.webkit.WebView

/** Validates and executes browser navigation at the engine boundary. */
object BrowserNavigation {
    fun load(webView: WebView, rawUrl: String, httpsOnly: Boolean): Boolean {
        val url = rawUrl.trim()
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false

        if (scheme != "http" && scheme != "https") return false
        if (httpsOnly && scheme != "https") return false

        webView.loadUrl(url)
        return true
    }
}
