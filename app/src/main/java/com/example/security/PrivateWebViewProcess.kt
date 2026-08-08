package com.example.security

import android.os.Build
import android.webkit.WebView

/**
 * Call from a dedicated :private Android process before any WebView/API access.
 * This is the mechanism Android provides for separate WebView data stores.
 */
object PrivateWebViewProcess {
    private const val DATA_DIRECTORY_SUFFIX = "private"

    fun initialize() {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            "Dedicated WebView data directories require Android 9 (API 28)+"
        }
        WebView.setDataDirectorySuffix(DATA_DIRECTORY_SUFFIX)
    }
}
