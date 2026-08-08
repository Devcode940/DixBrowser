package com.example.browser

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** Compose adapter that keeps WebView ownership outside BrowserScreen. */
@Composable
fun BrowserWebView(
    tab: BrowserTabState,
    controller: BrowserWebViewController,
    modifier: Modifier = Modifier,
    onCreated: (WebView) -> Unit = {},
    onDisposed: () -> Unit = {}
) {
    val webViewHolder = remember(tab.id) { WebViewHolder() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).also { webView ->
                webView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewHolder.webView = webView
                controller.configure(webView, tab.isPrivate)
                onCreated(webView)
            }
        },
        update = { webView ->
            webViewHolder.webView = webView
            if (webView.url != tab.url && tab.url.isNotBlank() && tab.url != "about:blank") {
                webView.loadUrl(tab.url)
            }
        }
    )

    DisposableEffect(tab.id) {
        onDispose {
            webViewHolder.webView?.let(controller::destroy)
            webViewHolder.webView = null
            onDisposed()
        }
    }
}

private class WebViewHolder {
    var webView: WebView? = null
}
