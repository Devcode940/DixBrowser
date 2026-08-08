package com.example.privatebrowser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.security.PrivateWebViewProcess
import com.example.security.WebViewSecurityPolicy
import java.util.UUID

/** Dedicated private-browsing Activity running in the :private process. */
class PrivateBrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // WHY: Android 8 and older cannot isolate Chromium data directories.
            // Failing closed is safer than presenting a false private mode.
            finish()
            return
        }

        PrivateWebViewProcess.initialize()

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

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun PrivateWebView(initialUrl: String) {
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
                    WebViewSecurityPolicy.installClient(this, httpsOnly = false)

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean = !isHttpUrl(request.url.toString())
                    }

                    webChromeClient = WebChromeClient()
                    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        controller.enqueueDownload(url, userAgent, contentDisposition, mimeType)
                    }
                    loadUrl(initialUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                // WHY: Release private WebView resources with the composition.
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
    private val downloadManager = context.getSystemService(DownloadManager::class.java)

    fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
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
            userAgent?.takeIf(String::isNotBlank)?.let { addRequestHeader("User-Agent", it) }
        }
        downloadManager.enqueue(request)
    }
}
