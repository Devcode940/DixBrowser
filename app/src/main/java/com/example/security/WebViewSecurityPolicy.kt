package com.example.security

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import android.os.Build

/** Central WebView hardening policy. Apply this to every browser WebView. */
object WebViewSecurityPolicy {
    private val allowedHttpSchemes = setOf("http", "https")
    private val blockedSchemes = setOf(
        "file", "content", "data", "javascript", "vbscript", "about"
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
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !incognito && allowThirdPartyCookies)

        if (incognito) {
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
        }
    }

    fun isSafeNavigation(uri: Uri, httpsOnly: Boolean): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme in blockedSchemes) return false
        if (scheme !in allowedHttpSchemes) return false
        return !httpsOnly || scheme == "https" || uri.host == null
    }

    fun installClient(webView: WebView, httpsOnly: Boolean) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                return !isSafeNavigation(uri, httpsOnly)
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: android.webkit.WebResourceError
            ) {
                super.onReceivedError(view, request, error)
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: SafeBrowsingResponse
            ) {
                callback.backToSafety(true)
            }
        }
    }

    fun destroy(webView: WebView) {
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearCache(true)
        webView.removeAllViews()
        webView.webChromeClient = null
        webView.webViewClient = null
        webView.destroy()
    }
}
