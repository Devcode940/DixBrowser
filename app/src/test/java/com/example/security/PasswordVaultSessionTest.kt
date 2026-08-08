package com.example.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordVaultSessionTest {
    @Test
    fun startsLocked() {
        assertFalse(PasswordVaultSession().isUnlocked)
    }

    @Test
    fun unlockAndLockWork() {
        val session = PasswordVaultSession()
        session.unlock()
        assertTrue(session.isUnlocked)
        session.lock()
        assertFalse(session.isUnlocked)
    }
}
