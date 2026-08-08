package com.example.data

import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val dao: PasswordCredentialDao) {

    val allCredentials: Flow<List<PasswordCredential>> = dao.getAllCredentials()

    suspend fun getCredentialsForDomain(domain: String): List<PasswordCredential> {
        val cleanDomain = extractCleanDomain(domain)
        if (cleanDomain.isBlank()) return emptyList()
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
        require(cleanDomain.isNotBlank()) { "A valid domain is required" }
        require(username.isNotBlank()) { "Username is required" }
        require(rawPassword.isNotEmpty()) { "Password cannot be empty" }

        val encrypted = PasswordSecurity.encrypt(rawPassword)
        val title = siteTitle.trim().ifBlank { cleanDomain }
        val normalizedUsername = username.trim()

        val existing = dao.getCredentialsForDomain(cleanDomain)
            .firstOrNull { it.username.equals(normalizedUsername, ignoreCase = true) }

        if (existing != null) {
            dao.updateCredential(
                existing.copy(
                    siteTitle = title,
                    encryptedPassword = encrypted,
                    notes = notes,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertCredential(
                PasswordCredential(
                    siteTitle = title,
                    domain = cleanDomain,
                    username = normalizedUsername,
                    encryptedPassword = encrypted,
                    notes = notes
                )
            )
        }
    }

    suspend fun deleteCredential(id: Int) = dao.deleteCredentialById(id)

    suspend fun clearAll() = dao.clearAll()

    fun decryptPassword(credential: PasswordCredential): String =
        PasswordSecurity.decrypt(credential.encryptedPassword)

    private fun extractCleanDomain(rawUrlOrDomain: String): String {
        var clean = rawUrlOrDomain.trim().lowercase()
        if (clean.startsWith("https://")) clean = clean.removePrefix("https://")
        if (clean.startsWith("http://")) clean = clean.removePrefix("http://")
        if (clean.startsWith("www.")) clean = clean.removePrefix("www.")
        return clean.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
    }
}
