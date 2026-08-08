package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Bookmark
import com.example.data.BookmarkRepository
import com.example.data.CookiePreference
import com.example.data.CookiePreferenceRepository
import com.example.data.History
import com.example.data.HistoryRepository
import com.example.data.OfflinePage
import com.example.data.OfflinePageRepository
import com.example.data.PasswordCredential
import com.example.data.PasswordRepository
import com.example.data.SavedTab
import com.example.data.SavedTabRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns browser-domain persistence and user-facing state. Compose should invoke
 * commands here rather than reaching directly into repositories.
 */class BrowserViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val historyRepository: HistoryRepository,
    private val savedTabRepository: SavedTabRepository,
    val settingsRepository: SettingsRepository,
    private val offlinePageRepository: OfflinePageRepository,
    val passwordRepository: PasswordRepository,
    val cookiePreferenceRepository: CookiePreferenceRepository
) : ViewModel() {

    val cookiePreferences = cookiePreferenceRepository.allPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedPasswords = passwordRepository.allCredentials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookmarks = bookmarkRepository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history = historyRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val offlinePages = offlinePageRepository.allOfflinePages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveCookiePreference(domain: String, allowFirstParty: Boolean, allowThirdParty: Boolean) =
        viewModelScope.launch { cookiePreferenceRepository.savePreference(domain, allowFirstParty, allowThirdParty) }

    fun deleteCookiePreference(domain: String) =
        viewModelScope.launch { cookiePreferenceRepository.deletePreference(domain) }

    fun savePassword(siteTitle: String, domain: String, username: String, rawPassword: String, notes: String = "") =
        viewModelScope.launch { passwordRepository.saveCredential(siteTitle, domain, username, rawPassword, notes) }

    fun deletePassword(id: Int) = viewModelScope.launch { passwordRepository.deleteCredential(id) }

    fun clearAllPasswords() = viewModelScope.launch { passwordRepository.clearAll() }

    suspend fun getPasswordsForDomain(domain: String): List<PasswordCredential> =
        passwordRepository.getCredentialsForDomain(domain)

    fun decryptPassword(credential: PasswordCredential): String = passwordRepository.decryptPassword(credential)

    fun addBookmark(title: String, url: String) =
        viewModelScope.launch { bookmarkRepository.insert(Bookmark(title = title, url = url)) }

    fun updateBookmark(bookmark: Bookmark) = viewModelScope.launch { bookmarkRepository.update(bookmark) }

    fun removeBookmark(id: Int) = viewModelScope.launch { bookmarkRepository.deleteById(id) }

    fun addHistory(title: String, url: String) =
        viewModelScope.launch { historyRepository.insert(History(title = title, url = url)) }

    fun clearHistory() = viewModelScope.launch { historyRepository.clearHistory() }

    /**
     * Persists only serializable tab metadata. WebView references never enter Room.
     */
    fun saveTabs(tabs: List<SavedTab>) {
        viewModelScope.launch {
            savedTabRepository.replaceAll(tabs.map { it.copy() })
        }
    }

    suspend fun getSavedTabs(): List<SavedTab> = savedTabRepository.getAllSavedTabs()

    fun saveOfflinePage(title: String, url: String, htmlContent: String) =
        viewModelScope.launch { offlinePageRepository.savePage(title, url, htmlContent) }

    fun deleteOfflinePage(id: Int) = viewModelScope.launch { offlinePageRepository.deleteById(id) }
}

/** Supplies BrowserViewModel with application-scoped repositories. */
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
        require(modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return BrowserViewModel(
            bookmarkRepository,
            historyRepository,
            savedTabRepository,
            settingsRepository,
            offlinePageRepository,
            passwordRepository,
            cookiePreferenceRepository
        ) as T
    }
}
