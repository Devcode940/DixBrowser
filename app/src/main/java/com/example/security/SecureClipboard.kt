package com.example.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Clipboard helper for sensitive browser data.
 *
 * WHY: Passwords copied to the Android clipboard can remain available to other
 * apps. Clearing the entry after a short delay reduces that exposure window.
 */
class SecureClipboard(context: Context) {
    private val clipboard = context.applicationContext
        .getSystemService(ClipboardManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    /** Copies sensitive data and schedules an automatic clear. */
    fun copySensitive(label: String, value: CharSequence, clearAfterMs: Long = 30_000L) {
        require(clearAfterMs > 0) { "clearAfterMs must be positive" }
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        handler.removeCallbacksAndMessages(CLEAR_TOKEN)
        handler.postAtTime(
            { clearIfUnchanged(value) },
            CLEAR_TOKEN,
            SystemClock.uptimeMillis() + clearAfterMs
        )
    }

    /** Clears the clipboard immediately and cancels the pending clear. */
    fun clear() {
        clearPrimaryClipCompat()
        handler.removeCallbacksAndMessages(CLEAR_TOKEN)
    }

    private fun clearIfUnchanged(expected: CharSequence) {
        val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(null)
        if (current?.toString() == expected.toString()) {
            clearPrimaryClipCompat()
        }
    }

    private fun clearPrimaryClipCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            // WHY: clearPrimaryClip() is API 28+. Replacing the clip with an
            // empty value provides equivalent behavior on older supported devices.
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    companion object {
        private val CLEAR_TOKEN = Any()
    }
}
