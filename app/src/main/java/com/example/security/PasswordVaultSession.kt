package com.example.security

import android.os.SystemClock

/** In-memory lock for the password vault. No plaintext is retained here. */
class PasswordVaultSession(private val timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
    private var unlockedAtElapsedMs: Long = 0L

    val isUnlocked: Boolean
        get() = unlockedAtElapsedMs != 0L &&
            SystemClock.elapsedRealtime() - unlockedAtElapsedMs < timeoutMs

    fun unlock() {
        unlockedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    fun touch() {
        if (isUnlocked) unlockedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    fun lock() {
        unlockedAtElapsedMs = 0L
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
