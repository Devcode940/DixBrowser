package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Bookmark
import com.example.data.BookmarkRepository
import com.example.data.History
import com.example.data.HistoryRepository
import com.example.data.OfflinePage
import com.example.data.OfflinePageRepository
import com.example.data.SavedTab
import com.example.data.SavedTabRepository
import com.example.data.SettingsRepository
import com.example.data.PasswordCredential
import com.example.data.PasswordRepository
import com.example.data.CookiePreference
import com.example.data.CookiePreferenceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BrowserViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val savedTabRepository: SavedTabRepository,
    val settingsRepository: SettingsRepository,
    private val offlinePageRepository: OfflinePageRepository,
    val passwordRepository: PasswordRepository,
    val cookiePreferenceRepository: CookiePreferenceRepository
) : ViewModel() {

    val cookiePreferences: StateFlow<List<CookiePreference>> = cookiePreferenceRepository.allPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveCookiePreference(domain: String, allowFirstParty: Boolean, allowThirdParty: Boolean) {
        viewModelScope.launch {
            cookiePreferenceRepository.savePreference(domain, allowFirstParty, allowThirdParty)
        }
    }

    fun deleteCookiePreference(domain: String) {
        viewModelScope.launch {
            cookiePreferenceRepository.deletePreference(domain)
        }
    }

    val savedPasswords: StateFlow<List<PasswordCredential>> = passwordRepository.allCredentials
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun savePassword(siteTitle: String, domain: String, username: String, rawPassword: String, notes: String = "") {
        viewModelScope.launch {
            passwordRepository.saveCredential(siteTitle, domain, username, rawPassword, notes)
        }
    }

    fun deletePassword(id: Int) {
        viewModelScope.launch {
            passwordRepository.deleteCredential(id)
        }
    }

    fun clearAllPasswords() {
        viewModelScope.launch {
            passwordRepository.clearAll()
        }
    }

    suspend fun getPasswordsForDomain(domain: String): List<PasswordCredential> {
        return passwordRepository.getCredentialsForDomain(domain)
    }

    fun decryptPassword(credential: PasswordCredential): String {
        return passwordRepository.decryptPassword(credential)
    }

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<History>> = historyRepository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val offlinePages: StateFlow<List<OfflinePage>> = offlinePageRepository.allOfflinePages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            bookmarkRepository.insert(Bookmark(title = title, url = url))
        }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepository.update(bookmark)
        }
    }

    fun removeBookmark(id: Int) {
        viewModelScope.launch {
            bookmarkRepository.deleteById(id)
        }
    }
    
    fun addHistory(title: String, url: String) {
        viewModelScope.launch {
            historyRepository.insert(History(title = title, url = url))
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
    
    fun saveTabs(tabs: List<SavedTab>) {
        viewModelScope.launch {
            savedTabRepository.replaceAll(tabs)
        }
    }

    suspend fun getSavedTabs(): List<SavedTab> {
        return savedTabRepository.getAllSavedTabs()
    }

    fun saveOfflinePage(title: String, url: String, htmlContent: String) {
        viewModelScope.launch {
            offlinePageRepository.savePage(title, url, htmlContent)
        }
    }

    fun deleteOfflinePage(id: Int) {
        viewModelScope.launch {
            offlinePageRepository.deleteById(id)
        }
    }
}

class BrowserViewModelFactory(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val savedTabRepository: SavedTabRepository,
    private val settingsRepository: SettingsRepository,
    private val offlinePageRepository: OfflinePageRepository,
    private val passwordRepository: PasswordRepository,
    private val cookiePreferenceRepository: CookiePreferenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(bookmarkRepository, historyRepository, savedTabRepository, settingsRepository, offlinePageRepository, passwordRepository, cookiePreferenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
