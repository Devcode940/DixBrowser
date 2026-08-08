package com.example.browser

import android.content.Context
import android.webkit.WebView

/** Creates WebViews through the browser engine boundary. */
class BrowserEngineFactory(
    private val context: Context,
    private val controller: BrowserWebViewController
) {
    fun create(privateTab: Boolean): WebView {
        // WHY: Application context prevents a WebView from accidentally retaining
        // an Activity beyond the UI lifecycle.
        val webView = WebView(context.applicationContext)
        controller.configure(webView, incognito = privateTab)
        return webView
    }
}
