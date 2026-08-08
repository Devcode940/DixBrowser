package com.example.security

import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
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
        "file", "content", "data", "javascript", "vbscript", "about"
    )

    /** Applies the minimum secure WebView configuration. */
    fun configure(
        webView: WebView,
        incognito: Boolean,
        javaScriptEnabled: Boolean,
        allowThirdPartyCookies: Boolean,
        desktopUserAgent: String? = null
    ) {
        val settings = webView.settings
        settings.javaScriptEnabled = javaScriptEnabled
        // WHY: DOM storage is required by many modern sites; private isolation
        // comes from the dedicated WebView data directory, not from disabling it.
        settings.domStorageEnabled = true
        settings.databaseEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // WHY: Private tabs should avoid persistent HTTP cache state while normal
        // tabs retain Chromium's cache for performance.
        settings.cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        settings.setGeolocationEnabled(false)
        settings.displayZoomControls = false
        settings.builtInZoomControls = true
        settings.userAgentString = desktopUserAgent ?: WebSettings.getDefaultUserAgent(webView.context)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView,
            !incognito && allowThirdPartyCookies
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }

        if (incognito) {
            // WHY: Remove any state that could have been created before this
            // WebView was attached to the private session. This never touches the
            // normal profile because the private process owns a separate data dir.
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearSslPreferences()
        }
    }

    /** Returns true only for navigations the browser is willing to load. */
    fun isSafeNavigation(uri: Uri, httpsOnly: Boolean): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme in blockedSchemes) return false
        if (scheme !in allowedHttpSchemes) return false
        if (uri.host.isNullOrBlank()) return false
        return !httpsOnly || scheme == "https"
    }

    /** Installs the navigation and Safe Browsing security boundary. */
    fun installClient(webView: WebView, httpsOnly: Boolean) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // WHY: Returning true for unsafe URLs prevents WebView from
                // navigating to file/content/javascript/custom-scheme targets.
                return !isSafeNavigation(request.url, httpsOnly)
            }

            @RequiresApi(Build.VERSION_CODES.O_MR1)
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

    /** Clears all Chromium state owned by the dedicated private process. */
    fun clearPrivateProfile() {
        // WHY: This runs only inside :private, so it cannot erase the normal
        // browser profile.
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
        runCatching { WebStorage.getInstance().deleteAllData() }
    }

    /** Releases a WebView without destroying the normal browser HTTP cache. */
    fun destroy(webView: WebView) {
        // WHY: Clearing cache on every normal tab close defeats browser caching
        // and can cause severe I/O and performance regressions.
        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.clearHistory()
        webView.clearFormData()
        webView.removeAllViews()
        webView.webChromeClient = null
        webView.webViewClient = null
        webView.destroy()
    }
}
