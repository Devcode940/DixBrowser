package com.example.security

import com.example.data.PasswordCredential
import com.example.data.PasswordRepository

/**
 * Authenticated boundary around password decryption.
 *
 * WHY: UI code should never be able to decrypt a credential merely by holding
 * a PasswordCredential object. Decryption requires a live authenticated vault
 * session.
 */
class PasswordVault(
    private val repository: PasswordRepository,
    private val session: PasswordVaultSession
) {
    /** Returns the plaintext password only while the vault is unlocked. */
    fun reveal(credential: PasswordCredential): String? {
        if (!session.isUnlocked) return null
        session.touch()
        return runCatching { repository.decryptPassword(credential) }.getOrNull()
    }

    /** Saves a credential only while the vault is unlocked. */
    fun save(
        siteTitle: String,
        domain: String,
        username: String,
        password: String,
        notes: String = ""
    ): Boolean {
        if (!session.isUnlocked) return false
        repository.saveCredential(siteTitle, domain, username, password, notes)
        session.touch()
        return true
    }

    /** Locks the in-memory vault session. */
    fun lock() = session.lock()
}
