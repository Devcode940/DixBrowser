package com.example.security

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap

/**
 * Applies security controls to WebViews created by legacy UI code.
 *
 * WHY: BrowserScreen historically creates WebViews directly. This runtime
 * boundary lets us harden those instances without duplicating the policy in
 * every Compose branch, while preserving the existing WebViewClient features.
 */
object WebViewRuntimeHardener {
    private const val PASSWORD_BRIDGE = "PasswordAutoFillBridge"
    private val wrappedClients = Collections.synchronizedMap(
        WeakHashMap<WebView, WeakReference<WebViewClient>>()
    )

    /**
     * Hardens an existing WebView without replacing its application behavior.
     */
    fun harden(webView: WebView, httpsOnly: Boolean) {
        val settings = webView.settings
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.databaseEnabled = false

        // WHY: This legacy bridge exposes password operations to arbitrary page JS.
        // Removing it breaks the unsafe trust boundary while keeping normal JS useful.
        runCatching { webView.removeJavascriptInterface(PASSWORD_BRIDGE) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = true
        }

        val current = webView.webViewClient
        if (current !is HardenedWebViewClient) {
            webView.webViewClient = HardenedWebViewClient(current, httpsOnly)
            wrappedClients[webView] = WeakReference(current)
        } else {
            current.httpsOnly = httpsOnly
        }
    }

    /**
     * Removes runtime references to a WebView after destruction.
     */
    fun forget(webView: WebView) {
        wrappedClients.remove(webView)
    }

    private class HardenedWebViewClient(
        private val delegate: WebViewClient,
        var httpsOnly: Boolean
    ) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!WebViewSecurityPolicy.isSafeNavigation(request.url, httpsOnly)) {
                return true
            }
            return delegate.shouldOverrideUrlLoading(view, request)
        }

        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            val uri = runCatching { Uri.parse(url) }.getOrNull()
            if (uri == null || !WebViewSecurityPolicy.isSafeNavigation(uri, httpsOnly)) {
                return true
            }
            return delegate.shouldOverrideUrlLoading(view, url)
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            if (!WebViewSecurityPolicy.isSafeNavigation(request.url, httpsOnly) &&
                request.url.scheme?.lowercase() !in setOf("http", "https")) {
                return emptyResponse()
            }
            if (httpsOnly && request.url.scheme.equals("http", ignoreCase = true)) {
                return emptyResponse()
            }
            return delegate.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            // WHY: A page reload can recreate JS globals, so remove the legacy
            // password interface every time navigation starts.
            runCatching { view.removeJavascriptInterface(PASSWORD_BRIDGE) }
            delegate.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            runCatching { view.removeJavascriptInterface(PASSWORD_BRIDGE) }
            delegate.onPageFinished(view, url)
        }

        override fun onSafeBrowsingHit(
            view: WebView,
            request: WebResourceRequest,
            threatType: Int,
            callback: SafeBrowsingResponse
        ) {
            callback.backToSafety(true)
        }
    }

    private fun emptyResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Blocked by browser security policy",
            emptyMap(),
            ""
        )
}
