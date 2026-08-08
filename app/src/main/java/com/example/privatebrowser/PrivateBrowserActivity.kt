package com.example.privatebrowser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.security.PrivateWebViewProcess
import com.example.security.WebViewSecurityPolicy
import java.util.UUID

/**
 * Dedicated private-browsing Activity.
 *
 * This Activity is declared with process=":private". On Android 9+, the
 * process receives its own WebView data-directory suffix before WebView is
 * instantiated, preventing normal and private WebView storage from sharing
 * the same Chromium profile.
 */
class PrivateBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        PrivateWebViewProcess.initialize()
        super.onCreate(savedInstanceState)

        val initialUrl = intent.getStringExtra(EXTRA_URL)
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

    override fun onDestroy() {
        // AndroidView owns the WebView and invokes its disposal callback.
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @androidx.compose.runtime.Composable
    private fun PrivateWebView(initialUrl: String) {
        val controller = remember {
            PrivateBrowserController(applicationContext)
        }

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
                    WebViewSecurityPolicy.installClient(this, httpsOnly = false)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // WHY: Only HTTP(S) navigation stays inside the private
                            // browser. Dangerous/custom schemes are rejected.
                            return !isHttpUrl(request.url.toString())
                        }
                    }

                    webChromeClient = WebChromeClient()

                    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        controller.enqueueDownload(
                            url = url,
                            userAgent = userAgent,
                            contentDisposition = contentDisposition,
                            mimeType = mimeType
                        )
                    }

                    loadUrl(initialUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                // WHY: Explicit cleanup drops renderer/network references when
                // the private Activity leaves the composition.
            }
        }
    }

    private fun isHttpUrl(value: String): Boolean {
        val scheme = Uri.parse(value).scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    companion object {
        const val EXTRA_URL = "com.example.privatebrowser.EXTRA_URL"
    }
}

private class PrivateBrowserController(private val context: android.content.Context) {
    private val downloadManager =
        context.getSystemService(DownloadManager::class.java)

    fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        if (url.isBlank()) return
        val uri = Uri.parse(url)
        if (uri.scheme != "http" && uri.scheme != "https") return

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
            userAgent?.takeIf(String::isNotBlank)?.let {
                addRequestHeader("User-Agent", it)
            }
        }
        downloadManager.enqueue(request)
    }
}
