package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.BookmarkRepository
import com.example.data.BrowserDatabase
import com.example.privatebrowser.PrivateBrowserActivity
import com.example.security.WebViewRuntimeHardener
import com.example.security.WebViewSecurityPolicy
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/** Main browser Activity. */
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

    private val webViewLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        // WHY: BrowserScreen currently creates WebViews inside Compose. This
        // observer hardens every instance as soon as it enters the view tree.
        hardenWebViews()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WHY: Never expose WebView remote debugging in production builds.
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        CookieManager.getInstance().setAcceptFileSchemeCookies(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        enableEdgeToEdge()
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(webViewLayoutListener)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                settingsRepository.httpsOnlyMode.collect { hardenWebViews() }
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BrowserScreen(viewModel = viewModel)

                        // WHY: The old in-screen incognito flag cannot provide
                        // process-level WebView isolation. This entry point always
                        // launches the genuine private browser Activity instead.
                        FloatingActionButton(
                            onClick = {
                                startActivity(Intent(this@MainActivity, PrivateBrowserActivity::class.java))
                            },
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Open private browser")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(webViewLayoutListener)
        destroyWebViews(window.decorView)
        super.onDestroy()
    }

    private fun hardenWebViews() {
        val httpsOnly = settingsRepository.httpsOnlyMode.value
        visitViews(window.decorView) { view ->
            if (view is WebView) {
                WebViewRuntimeHardener.harden(view, httpsOnly)
            }
        }
    }

    private fun visitViews(view: View, action: (View) -> Unit) {
        action(view)
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                visitViews(view.getChildAt(index), action)
            }
        }
    }

    private fun destroyWebViews(view: View) {
        if (view is WebView) {
            WebViewRuntimeHardener.forget(view)
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
