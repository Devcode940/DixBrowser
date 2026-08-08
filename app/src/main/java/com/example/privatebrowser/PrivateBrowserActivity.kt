package com.example.privatebrowser

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.security.PrivateBrowsingSession
import com.example.security.PrivateWebViewProcess
import com.example.security.WebViewSecurityPolicy

/**
 * Dedicated private-browsing Activity running in the :private process.
 *
 * WHY: Private browsing must have a separate Chromium data directory and must
 * never write browser history, bookmarks, passwords, or persistent tab state.
 */
class PrivateBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // WHY: setDataDirectorySuffix must happen before the first WebView in this process.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        PrivateWebViewProcess.initialize()
        super.onCreate(savedInstanceState)

        val initialUrl = intent.getStringExtra(EXTRA_URL)
            ?.let(::normalizeUrl)
            ?.takeIf(::isHttpUrl)
            ?: "https://www.google.com"

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrivateWebView(initialUrl)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @androidx.compose.runtime.Composable
    private fun PrivateWebView(initialUrl: String) {
        val session = remember { PrivateBrowsingSession() }
        val webViewHolder = remember { mutableStateOf<WebView?>(null) }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    WebViewSecurityPolicy.configure(
                        webView = this,
                        incognito = true,
                        javaScriptEnabled = true,
                        allowThirdPartyCookies = false
                    )
                    WebViewSecurityPolicy.installClient(this, httpsOnly = true)
                    webChromeClient = WebChromeClient()

                    // WHY: DownloadManager creates persistent system download records
                    // and files. Private mode therefore refuses downloads rather than
                    // leaking private activity into durable shared storage.
                    setDownloadListener { _, _, _, _, _ -> }

                    session.register(this)
                    webViewHolder.value = this
                    loadUrl(initialUrl)
                }
            },
            update = { view ->
                webViewHolder.value = view
                WebViewSecurityPolicy.configure(
                    webView = view,
                    incognito = true,
                    javaScriptEnabled = true,
                    allowThirdPartyCookies = false
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                // WHY: Closing private mode must destroy the renderer state and
                // discard the isolated session rather than leaving it in memory.
                session.clear()
                webViewHolder.value?.let { WebViewSecurityPolicy.destroy(it) }
                webViewHolder.value = null
            }
        }
    }

    private fun normalizeUrl(value: String): String {
        val input = value.trim()
        return when {
            input.startsWith("https://", ignoreCase = true) -> input
            input.startsWith("http://", ignoreCase = true) -> input
            input.contains(' ') -> "https://www.google.com/search?q=${Uri.encode(input)}"
            input.contains('.') -> "https://$input"
            else -> "https://www.google.com/search?q=${Uri.encode(input)}"
        }
    }

    private fun isHttpUrl(value: String): Boolean {
        val scheme = runCatching { Uri.parse(value).scheme?.lowercase() }.getOrNull()
        return scheme == "http" || scheme == "https"
    }

    companion object {
        const val EXTRA_URL = "com.example.privatebrowser.EXTRA_URL"
    }
}
