package com.example

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
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
import com.example.security.WebViewSecurityPolicy
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
        BrowserViewModelFactory(
            repository,
            historyRepository,
            savedTabRepository,
            settingsRepository,
            offlinePageRepository,
            passwordRepository,
            cookiePreferenceRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Never expose WebView remote debugging in production builds.
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        CookieManager.getInstance().setAcceptFileSchemeCookies(false)

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

    override fun onDestroy() {
        // AndroidView does not automatically release WebView resources when a
        // Compose hierarchy is torn down. Explicitly destroy every WebView in
        // the activity tree to avoid renderer/memory leaks.
        destroyWebViews(window.decorView)
        super.onDestroy()
    }

    private fun destroyWebViews(view: View) {
        if (view is WebView) {
            runCatching { WebViewSecurityPolicy.destroy(view) }
            return
        }
        if (view is ViewGroup) {
            for (index in view.childCount - 1 downTo 0) {
                destroyWebViews(view.getChildAt(index))
            }
        }
    }
}
