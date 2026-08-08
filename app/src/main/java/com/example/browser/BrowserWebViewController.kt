package com.example.browser

import android.webkit.WebView
import com.example.security.WebViewSecurityPolicy

/**
 * Browser-engine boundary. UI code should call this controller instead of
 * configuring WebView settings inline.
 */
class BrowserWebViewController(
    private val httpsOnly: Boolean,
    private val javaScriptEnabled: Boolean,
    private val thirdPartyCookiesEnabled: Boolean,
    private val desktopUserAgent: String? = null
) {
    /** Configures a newly created WebView and installs security callbacks. */
    fun configure(
        webView: WebView,
        incognito: Boolean,
        onRendererGone: () -> Unit = {}
    ) {
        WebViewSecurityPolicy.configure(
            webView = webView,
            incognito = incognito,
            javaScriptEnabled = javaScriptEnabled,
            allowThirdPartyCookies = thirdPartyCookiesEnabled,
            desktopUserAgent = desktopUserAgent
        )
        WebViewSecurityPolicy.installClient(webView, httpsOnly, onRendererGone)
    }

    /** Releases a WebView safely and idempotently. */
    fun destroy(webView: WebView) = WebViewSecurityPolicy.destroy(webView)
}
