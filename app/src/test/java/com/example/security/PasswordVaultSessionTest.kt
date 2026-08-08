package com.example.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
