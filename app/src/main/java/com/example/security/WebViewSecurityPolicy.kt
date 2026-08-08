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
import java.util.Collections
import java.util.WeakHashMap

/** Central security policy for all DixBrowser WebViews. */
object WebViewSecurityPolicy {
    private val allowedHttpSchemes = setOf("http", "https")
    private val blockedSchemes = setOf(
        "file", "content", "data", "javascript", "vbscript", "about"
    )
    private val destroyedWebViews = Collections.newSetFromMap(WeakHashMap<WebView, Boolean>())

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
        // WHY: DOM storage is required by modern sites; private isolation comes
        // from the dedicated WebView data directory.
        settings.domStorageEnabled = true
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

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(
            webView,
            !incognito && allowThirdPartyCookies
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }

        if (incognito) {
            // WHY: Remove state created before this WebView was attached to the
            // private session. The private process owns a separate data directory.
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

    /**
     * Installs navigation, Safe Browsing, and renderer-crash handling.
     *
     * WHY: Android requires a WebView whose renderer died to be discarded and
     * recreated; reusing it can crash the host application.
     */
    fun installClient(
        webView: WebView,
        httpsOnly: Boolean,
        onRendererGone: () -> Unit = {}
    ) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = !isSafeNavigation(request.url, httpsOnly)

            @RequiresApi(Build.VERSION_CODES.O_MR1)
            override fun onSafeBrowsingHit(
                view: WebView,
                request: WebResourceRequest,
                threatType: Int,
                callback: SafeBrowsingResponse
            ) {
                callback.backToSafety(true)
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: android.webkit.RenderProcessGoneDetail
            ): Boolean {
                // WHY: A dead renderer cannot safely be reused. Destroy this
                // instance and let the browser layer create a replacement.
                destroy(view)
                onRendererGone()
                return true
            }
        }
    }

    /** Clears all Chromium state owned by the dedicated private process. */
    fun clearPrivateProfile() {
        // WHY: This runs only inside :private, so it cannot erase the normal profile.
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        runCatching { CookieManager.getInstance().flush() }
        runCatching { WebStorage.getInstance().deleteAllData() }
    }

    /** Releases a WebView without destroying the normal browser HTTP cache. */
    fun destroy(webView: WebView) {
        // WHY: Compose disposal and Activity teardown can both attempt cleanup;
        // idempotent destruction prevents double-destroy renderer crashes.
        synchronized(destroyedWebViews) {
            if (!destroyedWebViews.add(webView)) return
        }
        runCatching {
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
}
