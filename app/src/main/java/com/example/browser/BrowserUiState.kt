package com.example.browser

/** Immutable state exposed by the browser domain to Compose. */
sealed interface BrowserUiState {
    data object Empty : BrowserUiState
    data object Loading : BrowserUiState
    data class Ready(
        val activeTabId: String?,
        val tabs: List<BrowserTabSummary>
    ) : BrowserUiState
    data class Error(val code: Code) : BrowserUiState

    enum class Code {
        RESTORE_FAILED,
        TAB_NOT_FOUND
    }
}

data class BrowserTabSummary(
    val id: String,
    val title: String,
    val url: String,
    val isPrivate: Boolean
)
