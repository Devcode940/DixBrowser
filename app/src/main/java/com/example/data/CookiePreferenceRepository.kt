package com.example.data

import kotlinx.coroutines.flow.Flow

class CookiePreferenceRepository(private val dao: CookiePreferenceDao) {
    val allPreferences: Flow<List<CookiePreference>> = dao.getAllPreferences()

    suspend fun getPreference(domain: String): CookiePreference? {
        return dao.getPreference(domain)
    }

    suspend fun savePreference(domain: String, allowFirstParty: Boolean, allowThirdParty: Boolean) {
        dao.savePreference(CookiePreference(domain, allowFirstParty, allowThirdParty))
    }
    
    suspend fun deletePreference(domain: String) {
        dao.deletePreference(domain)
    }
}
