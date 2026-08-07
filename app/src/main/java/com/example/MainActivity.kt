package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.BookmarkRepository
import com.example.data.BrowserDatabase
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val database by lazy { BrowserDatabase.getDatabase(this) }
    private val repository by lazy { BookmarkRepository(database.bookmarkDao()) }
    private val historyRepository by lazy { com.example.data.HistoryRepository(database.historyDao()) }
    private val savedTabRepository by lazy { com.example.data.SavedTabRepository(database.savedTabDao()) }
    private val settingsRepository by lazy { com.example.data.SettingsRepository(this) }
    private val offlinePageRepository by lazy { com.example.data.OfflinePageRepository(database.offlinePageDao()) }
    private val passwordRepository by lazy { com.example.data.PasswordRepository(database.passwordCredentialDao()) }
    private val cookiePreferenceRepository by lazy { com.example.data.CookiePreferenceRepository(database.cookiePreferenceDao()) }
    
    private val viewModel: BrowserViewModel by viewModels {
        BrowserViewModelFactory(repository, historyRepository, savedTabRepository, settingsRepository, offlinePageRepository, passwordRepository, cookiePreferenceRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto display cutout / full notch screen support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(viewModel = viewModel)
                }
            }
        }
    }
}
