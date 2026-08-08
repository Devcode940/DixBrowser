package com.example.security

import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/**
 * Central security policy for all DixBrowser WebViews.
 *
 * WHY: WebView configuration is security-sensitive; keeping it in one place
 * prevents individual screens from accidentally weakening the browser.
 */
object WebViewSecurityPolicy {
    private val allowedHttpSchemes = setOf("http", "https")
    private val blockedSchemes = setOf(
        "file", "content", "data", "javascript", "vbscript", "about", "blob"
    )

    fun configure(
        webView: WebView,
        incognito: Boolean,
        javaScriptEnabled: Boolean,
        allowThirdPartyCookies: Boolean,
        desktopUserAgent: String? = null
    ) {
        val settings = webView.settings
        settings.javaScriptEnabled = javaScriptEnabled
        settings.domStorageEnabled = !incognito
        settings.databaseEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        settings.setGeolocationEnabled(false)
        settings.displayZoomControls = false
        settings.builtInZoomControls = true
        settings.userAgentString = desktopUserAgent ?: WebSettings.getDefaultUserAgent(webView.context)

        CookieManager.getInstance().setAcceptCookie(!incognito)
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView,
            !incognito && allowThirdPartyCookies
        )

        if (incognito) {
            // WHY: Remove renderer-side navigation/cache/form state when a
            // private WebView is explicitly discarded.
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearSslPreferences()
        }
    }

    fun isSafeNavigation(uri: Uri, httpsOnly: Boolean): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme in blockedSchemes) return false
        if (scheme !in allowedHttpSchemes) return false
        if (uri.host.isNullOrBlank()) return false
        return !httpsOnly || scheme == "https"
    }

    fun installClient(webView: WebView, httpsOnly: Boolean) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // WHY: Never let WebView silently navigate to custom schemes or
                // local file/content resources.
                return !isSafeNavigation(request.url, httpsOnly)
            }

            @RequiresApi(Build.VERSION_CODES.O_MR1)
            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: SafeBrowsingResponse
            ) {
                // WHY: A known malicious page must be rejected, not rendered.
                callback.backToSafety(true)
            }
        }
    }

    fun destroy(webView: WebView) {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearCache(true)
        webView.clearFormData()
        webView.removeAllViews()
        webView.webChromeClient = null
        webView.webViewClient = null
        webView.destroy()
    }
}
