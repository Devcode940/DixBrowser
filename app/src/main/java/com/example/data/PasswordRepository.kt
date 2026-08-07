package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PasswordRepository(private val dao: PasswordCredentialDao) {

    val allCredentials: Flow<List<PasswordCredential>> = dao.getAllCredentials()

    suspend fun getCredentialsForDomain(domain: String): List<PasswordCredential> {
        val cleanDomain = extractCleanDomain(domain)
        return dao.getCredentialsForDomain(cleanDomain)
    }

    suspend fun saveCredential(
        siteTitle: String,
        domain: String,
        username: String,
        rawPassword: String,
        notes: String = ""
    ) {
        val cleanDomain = extractCleanDomain(domain)
        val encrypted = PasswordSecurity.encrypt(rawPassword)
        val title = if (siteTitle.isBlank()) cleanDomain else siteTitle
        
        // Check if existing credential exists for domain + username
        val existing = dao.getCredentialsForDomain(cleanDomain).firstOrNull { it.username.equals(username, ignoreCase = true) }
        if (existing != null) {
            val updated = existing.copy(
                siteTitle = title,
                encryptedPassword = encrypted,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
            dao.updateCredential(updated)
        } else {
            val newCred = PasswordCredential(
                siteTitle = title,
                domain = cleanDomain,
                username = username,
                encryptedPassword = encrypted,
                notes = notes
            )
            dao.insertCredential(newCred)
        }
    }

    suspend fun deleteCredential(id: Int) {
        dao.deleteCredentialById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    fun decryptPassword(credential: PasswordCredential): String {
        return PasswordSecurity.decrypt(credential.encryptedPassword)
    }

    private fun extractCleanDomain(rawUrlOrDomain: String): String {
        var clean = rawUrlOrDomain.trim().lowercase()
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        return clean.substringBefore("/").substringBefore(":")
    }
}
