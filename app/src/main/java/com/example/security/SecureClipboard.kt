package com.example.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper

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

    fun copySensitive(label: String, value: CharSequence, clearAfterMs: Long = 30_000L) {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        handler.removeCallbacksAndMessages(CLEAR_TOKEN)
        handler.postAtTime({ clearIfUnchanged(value) }, CLEAR_TOKEN, android.os.SystemClock.uptimeMillis() + clearAfterMs)
    }

    fun clear() {
        clipboard.clearPrimaryClip()
        handler.removeCallbacksAndMessages(CLEAR_TOKEN)
    }

    private fun clearIfUnchanged(expected: CharSequence) {
        val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(null)
        if (current?.toString() == expected.toString()) {
            clipboard.clearPrimaryClip()
        }
    }

    companion object {
        private val CLEAR_TOKEN = Any()
    }
}
