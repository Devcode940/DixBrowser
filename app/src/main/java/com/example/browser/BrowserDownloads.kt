package com.example.browser

import android.webkit.URLUtil
import com.example.download.BrowserDownloadController

/** Browser-facing download boundary; Compose never owns download persistence. */
class BrowserDownloads(
    private val controller: BrowserDownloadController
) {
    fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ): Long? {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return null
        if (URLUtil.isNetworkUrl(url).not()) return null
        return runCatching {
            controller.enqueue(url, userAgent, contentDisposition, mimeType)
        }.getOrNull()
    }
}
