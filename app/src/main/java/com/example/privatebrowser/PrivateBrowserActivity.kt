package com.example.privatebrowser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import java.util.UUID

/**
 * Dedicated private-browsing Activity running in the :private process.
 *
 * WHY: Private browsing must have a separate Chromium data directory and must
 * never write browser history, bookmarks, passwords, or persistent tab state.
 */
class PrivateBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // WHY: setDataDirectorySuffix must happen before this process creates any WebView.
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
        val controller = remember { PrivateBrowserController(applicationContext) }

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
                    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        controller.enqueueDownload(url, userAgent, contentDisposition, mimeType)
                    }
                    session.register(this)
                    webViewHolder.value = this
                    loadUrl(initialUrl)
                }
            },
            update = { view ->
                webViewHolder.value = view
                // WHY: Re-apply the immutable private profile policy after Compose updates.
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
                // WHY: Closing the private Activity must terminate the private renderer
                // state instead of leaving cookies/cache/form data alive in the process.
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

private class PrivateBrowserController(private val context: Context) {
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return

        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            .ifBlank { "private-download-${UUID.randomUUID()}" }

        val request = DownloadManager.Request(uri).apply {
            setTitle(fileName)
            setDescription("Private browsing download")
            setMimeType(mimeType)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
            userAgent?.takeIf(String::isNotBlank)?.let { addRequestHeader("User-Agent", it) }
        }
        downloadManager.enqueue(request)
    }
}
