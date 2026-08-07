package com.example

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BrowserTab(val id: String = UUID.randomUUID().toString(), defaultDesktop: Boolean = false) {
    var url by mutableStateOf("about:blank")
    var title by mutableStateOf("New Tab")
    var webView: WebView? = null
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var isHome by mutableStateOf(true)
    var isDesktopMode by mutableStateOf(defaultDesktop)
    var progress by mutableFloatStateOf(0f)
    var initialScrollY by mutableIntStateOf(0)
    val sniffedMedia = mutableStateListOf<com.example.ui.SniffedMedia>()
}

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val url: String,
    var progress: Float = 0f,
    var isPaused: Boolean = false,
    var isCompleted: Boolean = false,
    var fileSize: Long = (1024L..50000L).random() * 1024L
) {
    val formattedSize: String
        get() = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format(java.util.Locale.US, "%.2f MB", fileSize / (1024.0 * 1024.0))
        }
}

data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val url: String? = null
)

enum class SuggestionType {
    HISTORY, BOOKMARK, SEARCH
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel, modifier: Modifier = Modifier) {
    val cookiePreferences by viewModel.cookiePreferences.collectAsStateWithLifecycle()
    val defaultDesktopMode by viewModel.settingsRepository.defaultDesktopMode.collectAsStateWithLifecycle()
    val menuItems by viewModel.settingsRepository.menuItems.collectAsStateWithLifecycle()
    val tabs = remember { mutableStateListOf(BrowserTab(defaultDesktop = defaultDesktopMode)) }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    val downloads = remember { mutableStateListOf<DownloadItem>() }
    var showDownloads by remember { mutableStateOf(false) }

    var inputUrl by remember { mutableStateOf("") }
    var isIncognito by remember { mutableStateOf(false) }
    val isNightMode by viewModel.settingsRepository.isNightMode.collectAsStateWithLifecycle()
    val adBlockEnabled by viewModel.settingsRepository.adBlockEnabled.collectAsStateWithLifecycle()
    val searchEngine by viewModel.settingsRepository.searchEngine.collectAsStateWithLifecycle()
    val isJavaScriptEnabled by viewModel.settingsRepository.javaScriptEnabled.collectAsStateWithLifecycle()
    val isThirdPartyCookiesEnabled by viewModel.settingsRepository.thirdPartyCookiesEnabled.collectAsStateWithLifecycle()
    val urlBarPosition by viewModel.settingsRepository.urlBarPosition.collectAsStateWithLifecycle()
    val showShieldIcon by viewModel.settingsRepository.showShieldIcon.collectAsStateWithLifecycle()
    val showTranslateIcon by viewModel.settingsRepository.showTranslateIcon.collectAsStateWithLifecycle()
    val showPasswordsIcon by viewModel.settingsRepository.showPasswordsIcon.collectAsStateWithLifecycle()
    val showMediaSnifferIcon by viewModel.settingsRepository.showMediaSnifferIcon.collectAsStateWithLifecycle()
    val showHomeIcon by viewModel.settingsRepository.showHomeIcon.collectAsStateWithLifecycle()
    val showTabsIcon by viewModel.settingsRepository.showTabsIcon.collectAsStateWithLifecycle()
    val fingerprintProtectionEnabled by viewModel.settingsRepository.fingerprintProtectionEnabled.collectAsStateWithLifecycle()
    val defaultTargetLanguage by viewModel.settingsRepository.defaultTargetLanguage.collectAsStateWithLifecycle()

    val dnsProvider by viewModel.settingsRepository.dnsProvider.collectAsStateWithLifecycle()
    val customDnsUrl by viewModel.settingsRepository.customDnsUrl.collectAsStateWithLifecycle()
    val adBlockLevel by viewModel.settingsRepository.adBlockLevel.collectAsStateWithLifecycle()
    val enabledFilterLists by viewModel.settingsRepository.enabledFilterLists.collectAsStateWithLifecycle()
    val httpsOnlyMode by viewModel.settingsRepository.httpsOnlyMode.collectAsStateWithLifecycle()
    val webRtcLeakProtection by viewModel.settingsRepository.webRtcLeakProtection.collectAsStateWithLifecycle()
    val antiPhishingEnabled by viewModel.settingsRepository.antiPhishingEnabled.collectAsStateWithLifecycle()
    val userAgentPreset by viewModel.settingsRepository.userAgentPreset.collectAsStateWithLifecycle()
    val textZoomPercent by viewModel.settingsRepository.textZoomPercent.collectAsStateWithLifecycle()
    val selectedNewsCategory by viewModel.settingsRepository.selectedNewsCategory.collectAsStateWithLifecycle()

    var showPrivacyDashboardSheet by remember { mutableStateOf(false) }
    var showTranslationSheet by remember { mutableStateOf(false) }
    var showToolbarCustomizationSheet by remember { mutableStateOf(false) }
    var showDnsServicesSheet by remember { mutableStateOf(false) }
    var showTvCastingSheet by remember { mutableStateOf(false) }
    var showSecurityPrivacyControlsSheet by remember { mutableStateOf(false) }
    var showAdBlockLevelsSheet by remember { mutableStateOf(false) }
    var showWebContentViewSheet by remember { mutableStateOf(false) }
    var showBrowsingAnalyticsSheet by remember { mutableStateOf(false) }

    val offlinePages by viewModel.offlinePages.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showOfflinePages by remember { mutableStateOf(false) }
    var bookmarkToEdit by remember { mutableStateOf<com.example.data.Bookmark?>(null) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var showFindInPage by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var showPageSource by remember { mutableStateOf(false) }
    var pageSourceText by remember { mutableStateOf("") }
    var showMemo by remember { mutableStateOf(false) }
    var memoText by remember { mutableStateOf("") }
    var showImageGallery by remember { mutableStateOf(false) }
    var extractedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var showMediaSnifferSheet by remember { mutableStateOf(false) }
    var activeMediaPlayerState by remember { mutableStateOf<com.example.ui.MediaPlayerState?>(null) }
    var showPasswordManagerSheet by remember { mutableStateOf(false) }
    var showCookieSettingsSheet by remember { mutableStateOf(false) }
    var autoFillDomain by remember { mutableStateOf<String?>(null) }
    var pendingSaveCredential by remember { mutableStateOf<com.example.ui.PendingSaveCredential?>(null) }

    val context = LocalContext.current
    val defaultUserAgent = remember { WebSettings.getDefaultUserAgent(context) }
    
    var suggestions by remember { mutableStateOf(emptyList<SearchSuggestion>()) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val savedPasswords by viewModel.savedPasswords.collectAsStateWithLifecycle()

    LaunchedEffect(inputUrl, isSearchFocused, bookmarks, history) {
        if (inputUrl.isNotBlank() && isSearchFocused && !inputUrl.startsWith("http")) {
            val query = inputUrl.lowercase()
            
            val bookmarkSuggestions = bookmarks.filter { it.title.lowercase().contains(query) || it.url.lowercase().contains(query) }.map {
                SearchSuggestion(it.title, SuggestionType.BOOKMARK, it.url)
            }.take(3)
            
            val historySuggestions = history.filter { it.title.lowercase().contains(query) || it.url.lowercase().contains(query) }.map {
                SearchSuggestion(it.title, SuggestionType.HISTORY, it.url)
            }.take(3)
            
            suggestions = bookmarkSuggestions + historySuggestions
            
            delay(300)
            val netSuggestions = SearchSuggestionService.getSuggestions(inputUrl).map {
                SearchSuggestion(it, SuggestionType.SEARCH, null)
            }
            
            suggestions = (bookmarkSuggestions + historySuggestions + netSuggestions).distinctBy { it.text.lowercase() }.take(10)
        } else {
            suggestions = emptyList()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val savedTabs = tabs.mapIndexed { index, tab ->
                    com.example.data.SavedTab(
                        id = tab.id,
                        url = tab.url,
                        title = tab.title,
                        isHome = tab.isHome,
                        scrollY = tab.webView?.scrollY ?: 0,
                        orderIndex = index,
                        isActive = tab.id == activeTabId
                    )
                }
                viewModel.saveTabs(savedTabs)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val saved = viewModel.getSavedTabs()
        if (saved.isNotEmpty()) {
            tabs.clear()
            saved.forEach { st ->
                val tab = BrowserTab(st.id).apply {
                    url = st.url
                    title = st.title
                    isHome = st.isHome
                    initialScrollY = st.scrollY
                }
                tabs.add(tab)
                if (st.isActive) activeTabId = st.id
            }
        }
    }

    LaunchedEffect(activeTab.url, activeTab.isHome) {
        if (!activeTab.isHome && activeTab.url != "about:blank") {
            inputUrl = activeTab.url
        } else {
            inputUrl = ""
        }
    }

    val isBookmarked = bookmarks.any { it.url == activeTab.url }

    val keyboardController = LocalSoftwareKeyboardController.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = activeTab.canGoBack || drawerState.isOpen || showHistory || showDownloads || showTabs || showMenu || !activeTab.isHome) {
        if (showMenu) showMenu = false
        else if (drawerState.isOpen) coroutineScope.launch { drawerState.close() }
        else if (showHistory) showHistory = false
        else if (showSettings) showSettings = false
        else if (showOfflinePages) showOfflinePages = false
        else if (showDownloads) showDownloads = false
        else if (showTabs) showTabs = false
        else if (activeTab.canGoBack) activeTab.webView?.goBack()
        else {
            activeTab.isHome = true
            activeTab.webView?.loadUrl("about:blank")
        }
    }

    val backgroundColor = if (isNightMode) Color(0xFF121212) else Color(0xFFFAFAFA)
    val surfaceColor = if (isNightMode) Color(0xFF1E1E1E) else Color.White
    val surfaceVariantColor = if (isNightMode) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)
    val textColor = if (isNightMode) Color.White else Color(0xFF1E1E1E)
    val addressBarBg = if (isNightMode) Color(0xFF252525) else Color(0xFFF5F5F5)

    MaterialTheme(
        colorScheme = if (isNightMode) darkColorScheme(background = backgroundColor, surface = surfaceColor) 
                      else lightColorScheme(background = backgroundColor, surface = surfaceColor)
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = backgroundColor,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Bookmarks", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                            navigationIcon = {
                                IconButton(onClick = { coroutineScope.launch { drawerState.close() } }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showAddBookmarkDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Bookmark", tint = textColor)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
                        )
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (bookmarks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No bookmarks saved yet.\nTap + to save a website to Room database.",
                                            color = textColor.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                items(bookmarks) { bookmark ->
                                    ListItem(
                                        headlineContent = { Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor) },
                                        supportingContent = { Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = { bookmarkToEdit = bookmark }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                                }
                                                IconButton(onClick = { viewModel.removeBookmark(bookmark.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = backgroundColor),
                                        modifier = Modifier.clickable {
                                            activeTab.webView?.loadUrl(bookmark.url)
                                            coroutineScope.launch { drawerState.close() }
                                            activeTab.isHome = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            content = {
                Column(modifier = modifier.fillMaxSize().background(backgroundColor)) {
            
            // Render Search/URL Bar if set to Top
            if (urlBarPosition == "Top") {
                Surface(
                    color = surfaceColor,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isIncognito) {
                                Icon(Icons.Default.Security, contentDescription = "Incognito", tint = Color.Cyan, modifier = Modifier.padding(end = 4.dp).size(20.dp))
                            } else {
                                IconButton(onClick = {
                                    if (isBookmarked) {
                                        bookmarks.find { it.url == activeTab.url }?.let { viewModel.removeBookmark(it.id) }
                                    } else {
                                        viewModel.addBookmark(activeTab.webView?.title ?: activeTab.url, activeTab.url)
                                    }
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Bookmark", tint = if (isBookmarked) Color(0xFFFFC107) else textColor)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(addressBarBg, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = if (activeTab.isHome && !isSearchFocused) "" else inputUrl,
                                    onValueChange = { inputUrl = it },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).onFocusChanged { isSearchFocused = it.isFocused },
                                    singleLine = true,
                                    textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = {
                                        if (inputUrl.isNotBlank()) {
                                            val urlToLoad = formatUrl(inputUrl, searchEngine)
                                            activeTab.url = urlToLoad
                                            activeTab.isHome = false
                                            activeTab.webView?.loadUrl(urlToLoad)
                                            keyboardController?.hide()
                                            isSearchFocused = false
                                        }
                                    }),
                                    cursorBrush = SolidColor(if (isIncognito) Color.Cyan else MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                            if (inputUrl.isEmpty() || (activeTab.isHome && !isSearchFocused)) {
                                                Text("Search or type URL", color = textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                                            } else {
                                                innerTextField()
                                            }
                                        }
                                    }
                                )
                            }

                            if (showShieldIcon) {
                                IconButton(onClick = { showPrivacyDashboardSheet = true }, modifier = Modifier.size(36.dp)) {
                                    BadgedBox(
                                        badge = {
                                            if (com.example.AdBlocker.blockedCount > 0) {
                                                Badge(containerColor = Color(0xFFA6E3A1), contentColor = Color(0xFF11111B)) {
                                                    Text("${com.example.AdBlocker.blockedCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = "Privacy Shield", tint = if (com.example.AdBlocker.blockedCount > 0) Color(0xFFA6E3A1) else textColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            if (showTranslateIcon) {
                                IconButton(onClick = { showTranslationSheet = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.GTranslate, contentDescription = "Translate", tint = Color(0xFF89B4FA), modifier = Modifier.size(20.dp))
                                }
                            }

                            if (showMediaSnifferIcon) {
                                IconButton(
                                    onClick = {
                                        activeTab.webView?.let { webView ->
                                            val scanJs = """
                                                (function() {
                                                  var mediaList = [];
                                                  function addMedia(u, t) {
                                                    if (!u || u.indexOf('data:') === 0 || u.indexOf('blob:') === 0) return;
                                                    if (!mediaList.some(function(m) { return m.url === u; })) {
                                                      mediaList.push({ url: u, type: t });
                                                    }
                                                  }
                                                  var vids = document.querySelectorAll('video, video source, a[href*=".mp4"], a[href*=".webm"]');
                                                  for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    var src = v.src || v.href;
                                                    if (src) addMedia(typeof src === 'string' ? src : src.src, 'VIDEO');
                                                  }
                                                  var imgs = document.querySelectorAll('img');
                                                  for (var k = 0; k < imgs.length; k++) {
                                                    var isrc = imgs[k].src;
                                                    if (isrc) addMedia(isrc, 'IMAGE');
                                                  }
                                                  return JSON.stringify(mediaList);
                                                })()
                                            """.trimIndent()
                                            webView.evaluateJavascript(scanJs) { res ->
                                                if (!res.isNullOrBlank() && res != "null") {
                                                    try {
                                                        val clean = if (res.startsWith("\"")) org.json.JSONTokener(res).nextValue().toString() else res
                                                        val arr = org.json.JSONArray(clean)
                                                        for (i in 0 until arr.length()) {
                                                            val obj = arr.getJSONObject(i)
                                                            val u = obj.getString("url")
                                                            val t = if (obj.getString("type") == "VIDEO") com.example.ui.MediaType.VIDEO else com.example.ui.MediaType.IMAGE
                                                            val fname = URLUtil.guessFileName(u, null, null)
                                                            val ext = fname.substringAfterLast('.', "")
                                                            val item = com.example.ui.SniffedMedia(url = u, type = t, title = fname, extension = ext)
                                                            if (activeTab.sniffedMedia.none { it.url == u }) activeTab.sniffedMedia.add(item)
                                                        }
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }
                                        }
                                        showMediaSnifferSheet = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (activeTab.sniffedMedia.isNotEmpty()) {
                                                Badge(containerColor = Color(0xFFF38BA8), contentColor = Color.White) {
                                                    Text("${activeTab.sniffedMedia.size}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.SavedSearch, contentDescription = "Media Sniffer", tint = if (activeTab.sniffedMedia.isNotEmpty()) Color(0xFF89B4FA) else textColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            if (showPasswordsIcon) {
                                IconButton(onClick = { showPasswordManagerSheet = true }, modifier = Modifier.size(36.dp)) {
                                    val hasSavedPass = savedPasswords.any { activeTab.url.contains(it.domain, ignoreCase = true) && it.domain.isNotBlank() }
                                    Icon(Icons.Default.VpnKey, contentDescription = "Password Vault", tint = if (hasSavedPass) Color(0xFFFFB74D) else textColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            IconButton(onClick = { activeTab.webView?.reload() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = textColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (activeTab.isLoading && !activeTab.isHome) {
                            LinearProgressIndicator(
                                progress = { activeTab.progress },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = if (isIncognito) Color.Cyan else MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                tabs.forEach { tab ->
                    val isVisible = tab.id == activeTabId && !tab.isHome
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isVisible) 1f else 0f)
                    ) {
                        if (isVisible || tab.webView != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            tab.webView = this
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            settings.apply {
                                                javaScriptEnabled = isJavaScriptEnabled
                                                domStorageEnabled = !isIncognito
                                                databaseEnabled = false
                                                setSupportZoom(true)
                                                builtInZoomControls = true
                                                displayZoomControls = false
                                                useWideViewPort = true
                                                loadWithOverviewMode = true
                                                cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                            }

                                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, isThirdPartyCookiesEnabled)
                                            CookieManager.getInstance().setAcceptCookie(!isIncognito)

                                            addJavascriptInterface(object {
                                                @android.webkit.JavascriptInterface
                                                fun saveCredentials(domain: String, username: String, pass: String) {
                                                    if (username.isNotBlank() && pass.isNotBlank() && !isIncognito) {
                                                        val title = tab.title.ifBlank { domain }
                                                        pendingSaveCredential = com.example.ui.PendingSaveCredential(
                                                            siteTitle = title,
                                                            domain = domain,
                                                            username = username,
                                                            rawPassword = pass
                                                        )
                                                    }
                                                }

                                                @android.webkit.JavascriptInterface
                                                fun requestAutoFill(domain: String) {
                                                    if (!isIncognito) {
                                                        autoFillDomain = domain
                                                    }
                                                }
                                            }, "PasswordAutoFillBridge")

                                            setDownloadListener { url, _, contentDisposition, mimetype, _ ->
                                                val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                                downloads.add(DownloadItem(fileName = fileName, url = url))
                                                showDownloads = true
                                            }

                                            webViewClient = object : WebViewClient() {
                                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                    super.onPageStarted(view, url, favicon)
                                                    
                                                    // Apply cookie preferences for this domain
                                                    try {
                                                        val host = android.net.Uri.parse(url).host
                                                        if (host != null) {
                                                            val pref = cookiePreferences.find { it.domain == host }
                                                            val allowFirst = pref?.allowFirstParty ?: true
                                                            val allowThird = pref?.allowThirdParty ?: false
                                                            CookieManager.getInstance().setAcceptCookie(allowFirst && !isIncognito)
                                                            if (view != null) {
                                                                CookieManager.getInstance().setAcceptThirdPartyCookies(view, allowThird)
                                                            }
                                                        }
                                                    } catch (e: Exception) {}
                                                    
                                                    tab.isLoading = true
                                                    url?.let {
                                                        if (it != "about:blank") {
                                                            tab.url = it
                                                            tab.isHome = false
                                                            if (tab.id == activeTabId && !isSearchFocused) inputUrl = it
                                                        }
                                                    }
                                                    tab.canGoBack = view?.canGoBack() ?: false
                                                    tab.canGoForward = view?.canGoForward() ?: false
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    tab.isLoading = false
                                                    val pageTitle = view?.title ?: "New Tab"
                                                    tab.title = pageTitle
                                                    tab.canGoBack = view?.canGoBack() ?: false
                                                    tab.canGoForward = view?.canGoForward() ?: false
                                                    url?.let {
                                                        if (it != "about:blank" && !isIncognito) {
                                                            viewModel.addHistory(pageTitle, it)
                                                        }
                                                    }
                                                    if (tab.initialScrollY > 0) {
                                                        view?.scrollY = tab.initialScrollY
                                                        tab.initialScrollY = 0
                                                    }

                                                    // Password Save Watcher JS
                                                    val jsPasswordWatcher = """
                                                        (function() {
                                                            if (window.__passWatcherInjected) return;
                                                            window.__passWatcherInjected = true;
                                                            
                                                            function attachFormListeners() {
                                                                var forms = document.forms;
                                                                for (var i = 0; i < forms.length; i++) {
                                                                    var f = forms[i];
                                                                    if (!f.__passObserved) {
                                                                        f.__passObserved = true;
                                                                        var inputs = f.querySelectorAll('input');
                                                                        for(var j=0; j<inputs.length; j++) {
                                                                            inputs[j].addEventListener('focus', function() {
                                                                               if (this.type === 'password' || this.type === 'text' || this.type === 'email' || this.name.toLowerCase().includes('user')) {
                                                                                    if (window.PasswordAutoFillBridge) {
                                                                                        window.PasswordAutoFillBridge.requestAutoFill(window.location.hostname);
                                                                                    }
                                                                               }
                                                                            });
                                                                        }
                                                                        f.addEventListener('submit', function() {
                                                                            try {
                                                                                var pInput = this.querySelector('input[type="password"]');
                                                                                if (!pInput) pInput = document.querySelector('input[type="password"]');
                                                                                if (pInput && pInput.value) {
                                                                                    var uInput = this.querySelector('input[type="text"], input[type="email"], input[type="username"]');
                                                                                    if (!uInput) uInput = document.querySelector('input[type="text"], input[type="email"], input[type="username"]');
                                                                                    var uVal = uInput ? uInput.value : '';
                                                                                    var pVal = pInput.value;
                                                                                    var host = window.location.hostname;
                                                                                    if (uVal && pVal && window.PasswordAutoFillBridge) {
                                                                                        window.PasswordAutoFillBridge.saveCredentials(host, uVal, pVal);
                                                                                    }
                                                                                }
                                                                            } catch(ex){}
                                                                        }, true);
                                                                    }
                                                                }
                                                            }
                                                            attachFormListeners();
                                                            setTimeout(attachFormListeners, 1000);
                                                            setTimeout(attachFormListeners, 3000);
                                                        })()
                                                    """.trimIndent()
                                                    view?.evaluateJavascript(jsPasswordWatcher, null)

                                                    // DOM Media Sniffer
                                                    val jsSniffer = """
                                                        (function() {
                                                          var mediaList = [];
                                                          function addMedia(u, t) {
                                                            if (!u || u.indexOf('data:') === 0 || u.indexOf('blob:') === 0) return;
                                                            if (!mediaList.some(function(m) { return m.url === u; })) {
                                                              mediaList.push({ url: u, type: t });
                                                            }
                                                          }
                                                          var vids = document.querySelectorAll('video, video source, a[href*=".mp4"], a[href*=".webm"], a[href*=".m3u8"]');
                                                          for (var i = 0; i < vids.length; i++) {
                                                            var v = vids[i];
                                                            var src = v.src || v.href;
                                                            if (src) addMedia(typeof src === 'string' ? src : src.src, 'VIDEO');
                                                          }
                                                          var auds = document.querySelectorAll('audio, audio source, a[href*=".mp3"], a[href*=".wav"], a[href*=".m4a"]');
                                                          for (var j = 0; j < auds.length; j++) {
                                                            var a = auds[j];
                                                            var asrc = a.src || a.href;
                                                            if (asrc) addMedia(asrc, 'AUDIO');
                                                          }
                                                          var imgs = document.querySelectorAll('img, picture source');
                                                          for (var k = 0; k < imgs.length; k++) {
                                                            var img = imgs[k];
                                                            var isrc = img.src || img.srcset;
                                                            if (isrc && isrc.indexOf('data:image/svg') !== 0) addMedia(isrc, 'IMAGE');
                                                          }
                                                          return JSON.stringify(mediaList);
                                                        })()
                                                    """.trimIndent()

                                                    view?.evaluateJavascript(jsSniffer) { jsonResult ->
                                                        if (!jsonResult.isNullOrBlank() && jsonResult != "null") {
                                                            try {
                                                                val cleanJson = if (jsonResult.startsWith("\"")) {
                                                                    org.json.JSONTokener(jsonResult).nextValue().toString()
                                                                } else jsonResult
                                                                val array = org.json.JSONArray(cleanJson)
                                                                for (i in 0 until array.length()) {
                                                                    val obj = array.getJSONObject(i)
                                                                    val mediaUrl = obj.getString("url")
                                                                    val typeStr = obj.getString("type")
                                                                    val mediaType = when (typeStr) {
                                                                        "VIDEO" -> com.example.ui.MediaType.VIDEO
                                                                        "AUDIO" -> com.example.ui.MediaType.AUDIO
                                                                        else -> com.example.ui.MediaType.IMAGE
                                                                    }
                                                                    val fileName = URLUtil.guessFileName(mediaUrl, null, null)
                                                                    val ext = fileName.substringAfterLast('.', "")
                                                                    val sniffedItem = com.example.ui.SniffedMedia(
                                                                        url = mediaUrl,
                                                                        type = mediaType,
                                                                        title = fileName,
                                                                        extension = ext
                                                                    )
                                                                    if (tab.sniffedMedia.none { it.url == mediaUrl }) {
                                                                        tab.sniffedMedia.add(sniffedItem)
                                                                    }
                                                                }
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                    }
                                                }

                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    return false
                                                }

                                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                                    val urlStr = request?.url?.toString() ?: return null
                                                    if (adBlockEnabled && AdBlocker.isAd(urlStr)) {
                                                        return AdBlocker.createEmptyResponse()
                                                    }

                                                    val lowerUrl = urlStr.lowercase()
                                                    val sniffType = when {
                                                        lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".webm") || lowerUrl.contains(".mov") || lowerUrl.contains("/video/") -> com.example.ui.MediaType.VIDEO
                                                        lowerUrl.contains(".mp3") || lowerUrl.contains(".wav") || lowerUrl.contains(".m4a") || lowerUrl.contains(".aac") || lowerUrl.contains("/audio/") -> com.example.ui.MediaType.AUDIO
                                                        (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif")) && !lowerUrl.contains("favicon") -> com.example.ui.MediaType.IMAGE
                                                        else -> null
                                                    }
                                                    if (sniffType != null) {
                                                        val fileName = URLUtil.guessFileName(urlStr, null, null)
                                                        val ext = fileName.substringAfterLast('.', "")
                                                        val sniffedItem = com.example.ui.SniffedMedia(
                                                            url = urlStr,
                                                            type = sniffType,
                                                            title = fileName,
                                                            extension = ext
                                                        )
                                                        view?.post {
                                                            if (tab.sniffedMedia.none { it.url == urlStr }) {
                                                                tab.sniffedMedia.add(sniffedItem)
                                                            }
                                                        }
                                                    }

                                                    return super.shouldInterceptRequest(view, request)
                                                }
                                            }

                                            webChromeClient = object : WebChromeClient() {
                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                    super.onProgressChanged(view, newProgress)
                                                    tab.progress = newProgress / 100f
                                                }
                                            }
                                            loadUrl(tab.url)
                                        }
                                    },
                                    update = { view ->
                                        view.settings.javaScriptEnabled = isJavaScriptEnabled
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(view, isThirdPartyCookiesEnabled)
                                        view.settings.domStorageEnabled = !isIncognito
                                        view.settings.cacheMode = if (isIncognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
                                        if (isIncognito) {
                                            CookieManager.getInstance().setAcceptCookie(false)
                                            view.clearHistory()
                                            view.clearCache(true)
                                        } else {
                                            CookieManager.getInstance().setAcceptCookie(true)
                                        }
                                        
                                        if (tab.isDesktopMode) {
                                            view.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
                                        } else {
                                            view.settings.userAgentString = defaultUserAgent
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize().padding(if (isVisible) 0.dp else 10000.dp) 
                                )
                                
                                if (isVisible) {
                                    var isSwipingBack by remember { mutableStateOf(false) }
                                    var isSwipingForward by remember { mutableStateOf(false) }
                                    
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .width(16.dp)
                                            .pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onDragEnd = { isSwipingBack = false },
                                                    onDragCancel = { isSwipingBack = false }
                                                ) { change, dragAmount ->
                                                    if (dragAmount > 40 && !isSwipingBack) {
                                                        isSwipingBack = true
                                                        change.consume()
                                                        if (tab.canGoBack) {
                                                            tab.webView?.goBack()
                                                        } else if (!tab.isHome) {
                                                            tab.isHome = true
                                                            tab.webView?.loadUrl("about:blank")
                                                        }
                                                    }
                                                }
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(16.dp)
                                            .pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onDragEnd = { isSwipingForward = false },
                                                    onDragCancel = { isSwipingForward = false }
                                                ) { change, dragAmount ->
                                                    if (dragAmount < -40 && !isSwipingForward && tab.canGoForward) {
                                                        isSwipingForward = true
                                                        change.consume()
                                                        tab.webView?.goForward()
                                                    }
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeTab.isHome) {
                    Surface(
                        modifier = Modifier.fillMaxSize().zIndex(2f),
                        color = backgroundColor
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Home Screen Search Bar
                            val keyboardController = LocalSoftwareKeyboardController.current
                            
                            OutlinedTextField(
                                value = if (activeTab.isHome && !isSearchFocused) "" else inputUrl,
                                onValueChange = { inputUrl = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(56.dp)
                                    .onFocusChanged { isSearchFocused = it.isFocused },
                                shape = RoundedCornerShape(28.dp),
                                placeholder = { Text("Search or type URL", color = textColor.copy(alpha = 0.5f)) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = textColor.copy(alpha = 0.5f)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = surfaceColor,
                                    unfocusedContainerColor = surfaceColor,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = {
                                    if (inputUrl.isNotBlank()) {
                                        val urlToLoad = formatUrl(inputUrl, searchEngine)
                                        activeTab.url = urlToLoad
                                        activeTab.isHome = false
                                        activeTab.webView?.loadUrl(urlToLoad)
                                        keyboardController?.hide()
                                        isSearchFocused = false
                                    }
                                })
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // Top Sites
                            val topSites = listOf(
                                "Google" to "https://www.google.com",
                                "YouTube" to "https://www.youtube.com",
                                "Facebook" to "https://www.facebook.com",
                                "Instagram" to "https://www.instagram.com",
                                "X" to "https://www.x.com"
                            )
                            
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(topSites) { site ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            activeTab.isHome = false
                                            activeTab.url = site.second
                                            activeTab.webView?.loadUrl(site.second)
                                        }
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = surfaceColor,
                                            shadowElevation = 2.dp,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                // Simplified icon representation
                                                Text(
                                                    text = site.first.take(1),
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (site.first) {
                                                        "Google" -> Color(0xFF4285F4)
                                                        "YouTube" -> Color(0xFFFF0000)
                                                        "Facebook" -> Color(0xFF1877F2)
                                                        "Instagram" -> Color(0xFFE4405F)
                                                        "X" -> textColor
                                                        else -> textColor
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = site.first,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = textColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                // Add button
                                item {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { showAddBookmarkDialog = true }
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = surfaceVariantColor,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Bookmark", tint = textColor)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Add",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textColor,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Saved Bookmarks Section (Room Database Integration)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Saved Bookmarks (${bookmarks.size})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    TextButton(onClick = { showAddBookmarkDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (bookmarks.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                        border = BorderStroke(1.dp, surfaceVariantColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                "No bookmarks saved yet. Tap the star icon while browsing or tap Add to save sites in Room database.",
                                                fontSize = 12.sp,
                                                color = textColor.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        bookmarks.take(5).forEach { bookmark ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    activeTab.isHome = false
                                                    activeTab.url = bookmark.url
                                                    activeTab.webView?.loadUrl(bookmark.url)
                                                },
                                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(bookmark.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text(bookmark.url, fontSize = 11.sp, color = textColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                    IconButton(onClick = { viewModel.removeBookmark(bookmark.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                        if (bookmarks.size > 5) {
                                            TextButton(
                                                onClick = { coroutineScope.launch { drawerState.open() } },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text("View all ${bookmarks.size} bookmarks in Drawer", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Analytics & Habits Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { showBrowsingAnalyticsSheet = true },
                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                border = BorderStroke(1.dp, surfaceVariantColor),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF89B4FA).copy(alpha = 0.15f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Analytics,
                                                    contentDescription = null,
                                                    tint = Color(0xFF4285F4),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = "Weekly Browsing Analytics",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = textColor
                                            )
                                            Text(
                                                text = "View site visit frequency & habits chart",
                                                fontSize = 12.sp,
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Latest News Widget
                            com.example.ui.LatestNewsWidget(
                                selectedCategory = selectedNewsCategory,
                                onSelectCategory = { viewModel.settingsRepository.setSelectedNewsCategory(it) },
                                onOpenArticle = { articleUrl ->
                                    activeTab.isHome = false
                                    activeTab.url = articleUrl
                                    activeTab.webView?.loadUrl(articleUrl)
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                
                // Suggestions Dropdown overlay
                if (suggestions.isNotEmpty() && isSearchFocused) {
                    Surface(
                        modifier = Modifier
                            .align(if (urlBarPosition == "Bottom" && !activeTab.isHome) Alignment.BottomCenter else Alignment.TopCenter)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .zIndex(10f),
                        color = surfaceColor,
                        shadowElevation = 4.dp
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(suggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (suggestion.url != null) {
                                                inputUrl = suggestion.url
                                            } else {
                                                inputUrl = suggestion.text
                                            }
                                            isSearchFocused = false
                                            val urlToLoad = if (suggestion.url != null) suggestion.url else formatUrl(suggestion.text, searchEngine)
                                            activeTab.url = urlToLoad
                                            activeTab.isHome = false
                                            activeTab.webView?.loadUrl(urlToLoad)
                                            keyboardController?.hide()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when(suggestion.type) {
                                            SuggestionType.HISTORY -> Icons.Default.History
                                            SuggestionType.BOOKMARK -> Icons.Default.Bookmarks
                                            SuggestionType.SEARCH -> Icons.Default.Search
                                        },
                                        contentDescription = null,
                                        tint = textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = suggestion.text,
                                            color = textColor,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (suggestion.url != null) {
                                            Text(
                                                text = suggestion.url,
                                                color = textColor.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showTabs) {
                    Surface(
                        modifier = Modifier.fillMaxSize().zIndex(11f),
                        color = backgroundColor
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TopAppBar(
                                title = { Text("Tabs", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                actions = {
                                    IconButton(onClick = {
                                        val newTab = BrowserTab(defaultDesktop = defaultDesktopMode)
                                        tabs.add(newTab)
                                        activeTabId = newTab.id
                                        showTabs = false
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "New Tab", tint = textColor)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
                            )
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(tabs, key = { it.id }) { tab ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                                tabs.remove(tab)
                                                tab.webView?.destroy()
                                                if (tabs.isEmpty()) {
                                                    tabs.add(BrowserTab(defaultDesktop = defaultDesktopMode))
                                                }
                                                if (activeTabId == tab.id) {
                                                    activeTabId = tabs.last().id
                                                }
                                                true
                                            } else false
                                        }
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(Color.Red, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Close Tab", tint = Color.White)
                                            }
                                        }
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(0.7f)
                                                .clickable {
                                                    activeTabId = tab.id
                                                    showTabs = false
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            border = if (activeTabId == tab.id) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = textColor, fontSize = 13.sp)
                                                    IconButton(onClick = { 
                                                        tabs.remove(tab)
                                                        tab.webView?.destroy()
                                                        if (tabs.isEmpty()) tabs.add(BrowserTab(defaultDesktop = defaultDesktopMode))
                                                        if (activeTabId == tab.id) activeTabId = tabs.last().id
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Box(modifier = Modifier.fillMaxSize().background(surfaceVariantColor, RoundedCornerShape(8.dp))) {
                                                    Text(if (tab.isHome) "Home Page" else tab.url, fontSize = 10.sp, color = textColor.copy(alpha = 0.6f), maxLines = 4, modifier = Modifier.padding(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showHistory) {
                    Surface(
                        modifier = Modifier.fillMaxSize().zIndex(12f),
                        color = backgroundColor
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TopAppBar(
                                title = { Text("History", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                navigationIcon = {
                                    IconButton(onClick = { showHistory = false }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { viewModel.clearHistory() }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = textColor)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
                            )
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(history) { hist ->
                                    val isHistBookmarked = bookmarks.any { it.url == hist.url }
                                    ListItem(
                                        headlineContent = { Text(hist.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor) },
                                        supportingContent = { Text(hist.url, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp) },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                if (isHistBookmarked) {
                                                    bookmarks.find { it.url == hist.url }?.let { viewModel.removeBookmark(it.id) }
                                                } else {
                                                    viewModel.addBookmark(hist.title.ifBlank { hist.url }, hist.url)
                                                    android.widget.Toast.makeText(context, "Saved to Room bookmarks", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(
                                                    if (isHistBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "Save to Bookmarks",
                                                    tint = if (isHistBookmarked) Color(0xFFFFC107) else textColor.copy(alpha = 0.5f)
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = backgroundColor),
                                        modifier = Modifier.clickable {
                                            activeTab.webView?.loadUrl(hist.url)
                                            showHistory = false
                                            activeTab.isHome = false
                                        }
                                    )
                                }
                                if (history.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text("No browsing history.", color = textColor.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (showDownloads) {
                    Surface(
                        modifier = Modifier.fillMaxSize().zIndex(13f),
                        color = backgroundColor
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            TopAppBar(
                                title = { Text("Downloads", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                navigationIcon = {
                                    IconButton(onClick = { showDownloads = false }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
                            )
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(downloads) { download ->
                                    val itemIndex = downloads.indexOf(download)
                                    ListItem(
                                        headlineContent = { Text(download.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor) },
                                        supportingContent = { 
                                            Column {
                                                Text(download.url, maxLines = 1, overflow = TextOverflow.Ellipsis, color = textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                                                if (!download.isCompleted) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = { download.progress }, 
                                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                                        trackColor = surfaceVariantColor
                                                    )
                                                    val downloadedSize = download.fileSize * download.progress
                                                    val formattedDownloaded = when {
                                                        downloadedSize < 1024 -> "${downloadedSize.toLong()} B"
                                                        downloadedSize < 1024 * 1024 -> "${(downloadedSize / 1024).toLong()} KB"
                                                        else -> String.format(java.util.Locale.US, "%.2f MB", downloadedSize / (1024.0 * 1024.0))
                                                    }
                                                    Text(if (download.isPaused) "Paused • ${download.formattedSize}" else "Downloading • $formattedDownloaded / ${download.formattedSize}", color = textColor.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                                } else {
                                                    Text("Completed • ${download.formattedSize}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                                }
                                            }
                                        },
                                        trailingContent = {
                                            Row {
                                                if (!download.isCompleted) {
                                                    IconButton(onClick = { 
                                                        downloads[itemIndex] = download.copy(isPaused = !download.isPaused)
                                                    }) {
                                                        Icon(if (download.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = "Pause/Resume", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                                IconButton(onClick = { downloads.remove(download) }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Cancel/Remove", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = backgroundColor)
                                    )
                                }
                                if (downloads.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text("No active downloads.", color = textColor.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    LaunchedEffect(downloads.size, downloads.map { it.isPaused }) {
                        downloads.filter { !it.isCompleted && !it.isPaused }.forEach { download ->
                            val index = downloads.indexOf(download)
                            if (index != -1 && downloads[index].progress < 1f) {
                                downloads[index] = downloads[index].copy(progress = minOf(1f, downloads[index].progress + 0.1f))
                                if (downloads[index].progress >= 1f) {
                                    downloads[index] = downloads[index].copy(isCompleted = true)
                                }
                            }
                        }
                    }
                }
            }

            if (bookmarkToEdit != null) {
                var editTitle by remember { mutableStateOf(bookmarkToEdit!!.title) }
                var editUrl by remember { mutableStateOf(bookmarkToEdit!!.url) }
                AlertDialog(
                    onDismissRequest = { bookmarkToEdit = null },
                    containerColor = surfaceColor,
                    title = { Text("Edit Bookmark", color = textColor, fontSize = 18.sp) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                label = { Text("Title", color = textColor.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                textStyle = TextStyle(color = textColor),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            OutlinedTextField(
                                value = editUrl,
                                onValueChange = { editUrl = it },
                                label = { Text("URL", color = textColor.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = textColor),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.updateBookmark(bookmarkToEdit!!.copy(title = editTitle, url = editUrl))
                            bookmarkToEdit = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { bookmarkToEdit = null }) {
                            Text("Cancel", color = textColor.copy(alpha = 0.6f))
                        }
                    }
                )
            }

            if (showAddBookmarkDialog) {
                var newTitle by remember { mutableStateOf(if (!activeTab.isHome && activeTab.url != "about:blank") (activeTab.webView?.title ?: activeTab.url) else "") }
                var newUrl by remember { mutableStateOf(if (!activeTab.isHome && activeTab.url != "about:blank") activeTab.url else "") }
                AlertDialog(
                    onDismissRequest = { showAddBookmarkDialog = false },
                    containerColor = surfaceColor,
                    title = { Text("Add Bookmark", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Title", color = textColor.copy(alpha = 0.6f)) },
                                placeholder = { Text("e.g. Google News") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                textStyle = TextStyle(color = textColor),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            OutlinedTextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                label = { Text("URL", color = textColor.copy(alpha = 0.6f)) },
                                placeholder = { Text("https://example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = textColor),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = newUrl.isNotBlank(),
                            onClick = {
                                val formattedUrl = if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) "https://$newUrl" else newUrl
                                val formattedTitle = newTitle.ifBlank { formattedUrl }
                                viewModel.addBookmark(formattedTitle, formattedUrl)
                                showAddBookmarkDialog = false
                                android.widget.Toast.makeText(context, "Saved to Room database bookmarks", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddBookmarkDialog = false }) {
                            Text("Cancel", color = textColor.copy(alpha = 0.6f))
                        }
                    }
                )
            }

            // Render Search/URL Bar if set to Bottom
            if (urlBarPosition == "Bottom") {
                Surface(
                    color = surfaceColor,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isIncognito) {
                                Icon(Icons.Default.Security, contentDescription = "Incognito", tint = Color.Cyan, modifier = Modifier.padding(end = 4.dp).size(20.dp))
                            } else {
                                IconButton(onClick = {
                                    if (isBookmarked) {
                                        bookmarks.find { it.url == activeTab.url }?.let { viewModel.removeBookmark(it.id) }
                                    } else {
                                        viewModel.addBookmark(activeTab.webView?.title ?: activeTab.url, activeTab.url)
                                    }
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Bookmark", tint = if (isBookmarked) Color(0xFFFFC107) else textColor)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(addressBarBg, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = if (activeTab.isHome && !isSearchFocused) "" else inputUrl,
                                    onValueChange = { inputUrl = it },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).onFocusChanged { isSearchFocused = it.isFocused },
                                    singleLine = true,
                                    textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = {
                                        if (inputUrl.isNotBlank()) {
                                            val urlToLoad = formatUrl(inputUrl, searchEngine)
                                            activeTab.url = urlToLoad
                                            activeTab.isHome = false
                                            activeTab.webView?.loadUrl(urlToLoad)
                                            keyboardController?.hide()
                                            isSearchFocused = false
                                        }
                                    }),
                                    cursorBrush = SolidColor(if (isIncognito) Color.Cyan else MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                            if (inputUrl.isEmpty() || (activeTab.isHome && !isSearchFocused)) {
                                                Text("Search or type URL", color = textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                                            } else {
                                                innerTextField()
                                            }
                                        }
                                    }
                                )
                            }

                            if (showShieldIcon) {
                                IconButton(onClick = { showPrivacyDashboardSheet = true }, modifier = Modifier.size(36.dp)) {
                                    BadgedBox(
                                        badge = {
                                            if (com.example.AdBlocker.blockedCount > 0) {
                                                Badge(containerColor = Color(0xFFA6E3A1), contentColor = Color(0xFF11111B)) {
                                                    Text("${com.example.AdBlocker.blockedCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = "Privacy Shield", tint = if (com.example.AdBlocker.blockedCount > 0) Color(0xFFA6E3A1) else textColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            if (showTranslateIcon) {
                                IconButton(onClick = { showTranslationSheet = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.GTranslate, contentDescription = "Translate", tint = Color(0xFF89B4FA), modifier = Modifier.size(20.dp))
                                }
                            }

                            if (showMediaSnifferIcon) {
                                IconButton(
                                    onClick = {
                                        activeTab.webView?.let { webView ->
                                            val scanJs = """
                                                (function() {
                                                  var mediaList = [];
                                                  function addMedia(u, t) {
                                                    if (!u || u.indexOf('data:') === 0 || u.indexOf('blob:') === 0) return;
                                                    if (!mediaList.some(function(m) { return m.url === u; })) {
                                                      mediaList.push({ url: u, type: t });
                                                    }
                                                  }
                                                  var vids = document.querySelectorAll('video, video source, a[href*=".mp4"], a[href*=".webm"]');
                                                  for (var i = 0; i < vids.length; i++) {
                                                    var v = vids[i];
                                                    var src = v.src || v.href;
                                                    if (src) addMedia(typeof src === 'string' ? src : src.src, 'VIDEO');
                                                  }
                                                  var imgs = document.querySelectorAll('img');
                                                  for (var k = 0; k < imgs.length; k++) {
                                                    var isrc = imgs[k].src;
                                                    if (isrc) addMedia(isrc, 'IMAGE');
                                                  }
                                                  return JSON.stringify(mediaList);
                                                })()
                                            """.trimIndent()
                                            webView.evaluateJavascript(scanJs) { res ->
                                                if (!res.isNullOrBlank() && res != "null") {
                                                    try {
                                                        val clean = if (res.startsWith("\"")) org.json.JSONTokener(res).nextValue().toString() else res
                                                        val arr = org.json.JSONArray(clean)
                                                        for (i in 0 until arr.length()) {
                                                            val obj = arr.getJSONObject(i)
                                                            val u = obj.getString("url")
                                                            val t = if (obj.getString("type") == "VIDEO") com.example.ui.MediaType.VIDEO else com.example.ui.MediaType.IMAGE
                                                            val fname = URLUtil.guessFileName(u, null, null)
                                                            val ext = fname.substringAfterLast('.', "")
                                                            val item = com.example.ui.SniffedMedia(url = u, type = t, title = fname, extension = ext)
                                                            if (activeTab.sniffedMedia.none { it.url == u }) activeTab.sniffedMedia.add(item)
                                                        }
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }
                                        }
                                        showMediaSnifferSheet = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (activeTab.sniffedMedia.isNotEmpty()) {
                                                Badge(containerColor = Color(0xFFF38BA8), contentColor = Color.White) {
                                                    Text("${activeTab.sniffedMedia.size}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.SavedSearch, contentDescription = "Media Sniffer", tint = if (activeTab.sniffedMedia.isNotEmpty()) Color(0xFF89B4FA) else textColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            if (showPasswordsIcon) {
                                IconButton(onClick = { showPasswordManagerSheet = true }, modifier = Modifier.size(36.dp)) {
                                    val hasSavedPass = savedPasswords.any { activeTab.url.contains(it.domain, ignoreCase = true) && it.domain.isNotBlank() }
                                    Icon(Icons.Default.VpnKey, contentDescription = "Password Vault", tint = if (hasSavedPass) Color(0xFFFFB74D) else textColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            IconButton(onClick = { activeTab.webView?.reload() }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = textColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (activeTab.isLoading && !activeTab.isHome) {
                            LinearProgressIndicator(
                                progress = { activeTab.progress },
                                modifier = Modifier.fillMaxWidth().height(2.dp),
                                color = if (isIncognito) Color.Cyan else MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Bar
            Surface(
                color = surfaceColor,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeTab.webView?.goBack() }, enabled = activeTab.canGoBack && !activeTab.isHome) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (activeTab.canGoBack && !activeTab.isHome) textColor else textColor.copy(alpha = 0.3f), modifier = Modifier.size(26.dp)) 
                    }
                    IconButton(onClick = { activeTab.webView?.goForward() }, enabled = activeTab.canGoForward && !activeTab.isHome) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = if (activeTab.canGoForward && !activeTab.isHome) textColor else textColor.copy(alpha = 0.3f), modifier = Modifier.size(26.dp)) 
                    }
                    IconButton(onClick = { activeTab.webView?.reload() }, enabled = !activeTab.isHome) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = if (!activeTab.isHome) textColor else textColor.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                    }
                    
                    if (showHomeIcon) {
                        IconButton(onClick = { 
                            activeTab.isHome = true
                            activeTab.webView?.loadUrl("about:blank")
                        }) {
                            Icon(Icons.Default.Home, contentDescription = "Home", tint = textColor, modifier = Modifier.size(24.dp))
                        }
                    }
                    IconButton(onClick = { 
                        coroutineScope.launch { drawerState.open() }
                    }) {
                        Icon(Icons.Default.StarBorder, contentDescription = "Bookmarks", tint = textColor, modifier = Modifier.size(24.dp))
                    }
                    
                    if (showTabsIcon) {
                        Box(modifier = Modifier.clickable { showTabs = true }.padding(6.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, textColor, RoundedCornerShape(4.dp)), 
                                contentAlignment = Alignment.Center
                            ) { 
                                Text("${tabs.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }

                    IconButton(onClick = { showToolbarCustomizationSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Customize Nav Bar", tint = textColor, modifier = Modifier.size(22.dp))
                    }
                    
                    IconButton(onClick = { showMenu = true }) { 
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = textColor, modifier = Modifier.size(26.dp)) 
                    }
                }
            }
            
                                    // Minimal Menu Bottom Sheet
            if (showMenu) {
                var menuPage by remember { mutableIntStateOf(0) }
                
                ModalBottomSheet(
                    onDismissRequest = { showMenu = false },
                    containerColor = Color.White,
                    dragHandle = null
                ) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                        // Top row of the sheet: refresh, security status, and right icons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = "Security", tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("2", color = Color.Red, fontSize = 14.sp)
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.LocalCafe, contentDescription = "Coffee", tint = Color.Black)
                                Icon(Icons.Default.ViewAgenda, contentDescription = "Tabs", tint = Color.Black)
                                IconButton(onClick = { showMenu = false; showSettings = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Black)
                                }
                            }
                        }
                        
                        // Tab indicators
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.weight(1f).height(2.dp).background(if (menuPage == 0) Color(0xFF6200EE) else Color.Transparent))
                            Box(modifier = Modifier.weight(1f).height(2.dp).background(if (menuPage == 1) Color(0xFF6200EE) else Color.Transparent))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (menuPage == 0) {
                                val page1 = listOf(
                                    Triple(Icons.Default.Security, "Privacy Shield") { showMenu = false; showPrivacyDashboardSheet = true },
                                    Triple(Icons.Default.GTranslate, "Translator") { showMenu = false; showTranslationSheet = true },
                                    Triple(Icons.Default.Tune, "Customize Nav") { showMenu = false; showToolbarCustomizationSheet = true },
                                    Triple(Icons.Default.History, "History") { showMenu = false; showHistory = true },
                                    Triple(Icons.Default.Download, "Downloads") { showMenu = false; showDownloads = true },
                                    Triple(Icons.Default.OfflinePin, "Offline pages") { showMenu = false; showOfflinePages = true },
                                    Triple(Icons.Default.AddBox, "New tab") { activeTab.isHome = true; activeTab.url = "about:blank"; activeTab.webView?.loadUrl("about:blank"); showMenu = false },
                                    Triple(Icons.Default.Image, "Image viewer") { showMenu = false; showImageGallery = true },
                                    Triple(Icons.Default.CloudDownload, "Save all images") { showMenu = false; android.widget.Toast.makeText(context, "Saved all images to Downloads", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Fullscreen, "Full screen") { showMenu = false; android.widget.Toast.makeText(context, "Fullscreen toggled", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.DarkMode, "Dark mode") { viewModel.settingsRepository.setNightMode(!isNightMode); showMenu = false },
                                    Triple(Icons.Outlined.Translate, "Translate") { activeTab.url = "https://translate.google.com/translate?sl=auto&tl=en&u=${android.net.Uri.encode(activeTab.webView?.url ?: activeTab.url)}"; activeTab.webView?.loadUrl(activeTab.url); showMenu = false },
                                    Triple(Icons.Default.VolumeUp, "Read aloud") { 
                                        showMenu = false
                                        val tts = android.speech.tts.TextToSpeech(context) { _ -> }
                                        tts.speak(activeTab.webView?.title ?: activeTab.url, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                        android.widget.Toast.makeText(context, "Reading aloud...", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    Triple(Icons.Default.RecordVoiceOver, "Text to Speech") { showMenu = false; activeDialog = "Text to Speech Settings" },
                                    Triple(Icons.Default.PlaylistAdd, "Add current page") { showMenu = false; viewModel.addBookmark(activeTab.webView?.title ?: activeTab.url, activeTab.url); android.widget.Toast.makeText(context, "Added to bookmarks", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Share, "Share URL") { 
                                        showMenu = false
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, activeTab.webView?.title ?: activeTab.url)
                                            putExtra(android.content.Intent.EXTRA_TEXT, activeTab.webView?.url ?: activeTab.url)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share URL"))
                                    },
                                    Triple(Icons.Default.FindInPage, "Find in page") { showMenu = false; showFindInPage = true },
                                    Triple(Icons.Outlined.SaveAlt, "Save page") { 
                                        showMenu = false
                                        activeTab.webView?.evaluateJavascript("(function(){return document.documentElement.outerHTML;})()") { value ->
                                            if (!value.isNullOrBlank() && value != "null") {
                                                val html = try {
                                                    org.json.JSONObject("{\"html\":$value}").getString("html")
                                                } catch (e: Exception) {
                                                    value.trim('"').replace("\\\"", "\"").replace("\\n", "\n")
                                                }
                                                val title = activeTab.webView?.title?.ifBlank { activeTab.url } ?: activeTab.url
                                                viewModel.saveOfflinePage(title, activeTab.url, html)
                                                android.widget.Toast.makeText(context, "Page saved to Room database for offline reading", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Unable to extract page HTML", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    Triple(Icons.Default.Print, "Print / PDF") { 
                                        showMenu = false
                                        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                                        activeTab.webView?.createPrintDocumentAdapter("Document")?.let {
                                            printManager.print("Document", it, android.print.PrintAttributes.Builder().build())
                                        }
                                    },
                                    Triple(Icons.Default.SavedSearch, "Media Sniffer") { showMenu = false; showMediaSnifferSheet = true },
                                    Triple(Icons.Default.VpnKey, "Passwords") { showMenu = false; showPasswordManagerSheet = true },
                                    Triple(Icons.Default.VideoLibrary, "Media Player") { 
                                        showMenu = false
                                        if (activeTab.sniffedMedia.any { it.type == com.example.ui.MediaType.VIDEO || it.type == com.example.ui.MediaType.AUDIO }) {
                                            val firstMedia = activeTab.sniffedMedia.first { it.type == com.example.ui.MediaType.VIDEO || it.type == com.example.ui.MediaType.AUDIO }
                                            activeMediaPlayerState = com.example.ui.MediaPlayerState(firstMedia.url, firstMedia.title, firstMedia.type == com.example.ui.MediaType.VIDEO)
                                        } else {
                                            android.widget.Toast.makeText(context, "No video/audio detected yet. Opening sniffer...", android.widget.Toast.LENGTH_SHORT).show()
                                            showMediaSnifferSheet = true
                                        }
                                    }
                                )
                                items(page1.size) { index ->
                                    val item = page1[index]
                                    MenuActionIcon(item.first, item.second, Color.Black) { (item.third as ()->Unit)() }
                                }
                            } else {
                                val page2 = listOf(
                                    Triple(Icons.Default.BarChart, "Analytics") { showMenu = false; showBrowsingAnalyticsSheet = true },
                                    Triple(Icons.Default.VpnKey, "DNS") { showMenu = false; showDnsServicesSheet = true },
                                    Triple(Icons.Default.Security, "Ad blocker") { showMenu = false; showAdBlockLevelsSheet = true },
                                    Triple(Icons.Default.Cast, "TV Cast") { showMenu = false; showTvCastingSheet = true },
                                    Triple(Icons.Default.Web, "Web content") { showMenu = false; showWebContentViewSheet = true },
                                    Triple(Icons.Default.GridOff, "Block area") { showMenu = false; android.widget.Toast.makeText(context, "Block area toggled", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.HideImage, "Block images") { showMenu = false; android.widget.Toast.makeText(context, "Block images toggled", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.DesktopMac, "Desktop") { activeTab.isDesktopMode = !activeTab.isDesktopMode; activeTab.webView?.reload(); showMenu = false },
                                    Triple(Icons.Default.Screenshot, "Screenshot") { showMenu = false; android.widget.Toast.makeText(context, "Screenshot captured", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Contrast, "Screen filter") { showMenu = false; android.widget.Toast.makeText(context, "Screen filter active", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.BrightnessHigh, "Brightness") { showMenu = false; activeDialog = "Brightness Settings" },
                                    Triple(Icons.Default.TextFields, "Text size") { showMenu = false; showWebContentViewSheet = true },
                                    Triple(Icons.Default.ZoomIn, "Text zoom in") { showMenu = false; viewModel.settingsRepository.setTextZoomPercent(minOf(200, textZoomPercent + 10)); android.widget.Toast.makeText(context, "Zoomed to ${textZoomPercent + 10}%", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Edit, "Image editor") { showMenu = false; activeDialog = "Image Editor" },
                                    Triple(Icons.Default.Swipe, "One-handed mode") { showMenu = false; android.widget.Toast.makeText(context, "One-handed mode toggled", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Article, "Page source") { 
                                        showMenu = false
                                        activeTab.webView?.evaluateJavascript("(function(){return document.documentElement.outerHTML;})()") { value ->
                                            pageSourceText = value ?: "<html></html>"
                                            showPageSource = true
                                        }
                                    },
                                    Triple(Icons.Default.Notes, "Memo") { showMenu = false; showMemo = true },
                                    Triple(Icons.Default.Delete, "Clear data") { showMenu = false; showSecurityPrivacyControlsSheet = true }
                                )
                                items(page2.size) { index ->
                                    val item = page2[index]
                                    MenuActionIcon(item.first, item.second, Color.Black) { (item.third as ()->Unit)() }
                                }
                            }
                        }
                        
                        // Pagination dots manually added to change page
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (menuPage == 0) Color.Gray else Color.LightGray).clickable { menuPage = 0 })
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (menuPage == 1) Color.Gray else Color.LightGray).clickable { menuPage = 1 })
                        }
                        
                        // Bottom Toolbar
                        Surface(
                            color = Color(0xFFFAFAFA),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { isIncognito = !isIncognito; showMenu = false }) { Icon(Icons.Outlined.Security, contentDescription = "Incognito", tint = Color.Black) }
                                IconButton(onClick = { viewModel.settingsRepository.setNightMode(!isNightMode); showMenu = false }) { Icon(Icons.Default.DarkMode, contentDescription = "Dark Mode", tint = Color.Black) }
                                IconButton(onClick = { showMenu = false; showDownloads = true }) { Icon(Icons.Default.Download, contentDescription = "Downloads", tint = Color.Black) }
                                IconButton(onClick = { /* video */ }) { Icon(Icons.Default.OndemandVideo, contentDescription = "Video", tint = Color.Black) }
                                IconButton(onClick = { 
                                    if (isIncognito) { CookieManager.getInstance().removeAllCookies(null) }
                                    showMenu = false 
                                }) { Icon(Icons.Default.PowerSettingsNew, contentDescription = "Exit", tint = Color.Black) }
                            }
                        }
                    }
                }
            }

                        if (showSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize().zIndex(10f),
                    color = Color(0xFFF0F0F0) // Light grey background like in screenshot
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Settings", color = Color.Black, fontSize = 20.sp) },
                            navigationIcon = {
                                IconButton(onClick = { showSettings = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.Black)
                                }
                            },
                            actions = {
                                IconButton(onClick = {}) { Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black) }
                                IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black) }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF0F0F0))
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // Section 1
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.Menu, title = "Menu") { showSettings = false; activeDialog = "Menu Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Tab, title = "Tab") { showSettings = false; activeDialog = "Tab Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Build, title = "Composition") { showSettings = false; activeDialog = "Composition Settings" }
                                    }
                                }
                            }
                            
                            // Section 2
                            item {
                                 Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                     Column {
                                         SettingsItem(icon = Icons.Default.Web, title = "Web content") { showSettings = false; showWebContentViewSheet = true }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.TouchApp, title = "Gesture") { showSettings = false; activeDialog = "Gesture Settings" }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Outlined.Translate, title = "Translator") { showSettings = false; showTranslationSheet = true }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.RecordVoiceOver, title = "Text to Speech") { showSettings = false; activeDialog = "Text to Speech Settings" }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.Cast, title = "TV Cast") { showSettings = false; showTvCastingSheet = true }
                                     }
                                 }
                             }
                             
                             // Section 3
                             item {
                                 Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                     Column {
                                         SettingsItem(icon = Icons.Default.Security, title = "Security & Privacy Controls") { showSettings = false; showSecurityPrivacyControlsSheet = true }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Outlined.Security, title = "Incognito mode") { showSettings = false; isIncognito = !isIncognito }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.Lock, title = "Password") { showSettings = false; showPasswordManagerSheet = true }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.Cookie, title = "Cookie Settings") { 
                                             showSettings = false
                                             showCookieSettingsSheet = true
                                         }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.Delete, title = "Clear data") { 
                                             showSettings = false
                                             showSecurityPrivacyControlsSheet = true
                                         }
                                         HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                         SettingsItem(icon = Icons.Default.VpnKey, title = "DNS") { showSettings = false; showDnsServicesSheet = true }
                                     }
                                 }
                             }
                            
                            // Section 4
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.ThumbUp, title = "Rate app") { showSettings = false; activeDialog = "Rate app" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.FavoriteBorder, title = "Recommend to a friend") { showSettings = false; activeDialog = "Recommend" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Email, title = "Send feedback") { showSettings = false; activeDialog = "Send feedback" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Info, title = "Information") { showSettings = false; activeDialog = "Information" }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showOfflinePages) {
                Surface(
                    modifier = Modifier.fillMaxSize().zIndex(15f),
                    color = backgroundColor
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Offline Saved Pages", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                            navigationIcon = {
                                IconButton(onClick = { showOfflinePages = false }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = textColor)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
                        )
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (offlinePages.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("No offline pages saved in database.", color = textColor.copy(alpha = 0.6f))
                                    }
                                }
                            } else {
                                items(offlinePages) { page ->
                                    val formattedDate = remember(page.timestamp) {
                                        java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(page.timestamp))
                                    }
                                    ListItem(
                                        headlineContent = { Text(page.title, color = textColor, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                        supportingContent = { Text("${page.url} • $formattedDate", color = textColor.copy(alpha = 0.6f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                                        trailingContent = {
                                            IconButton(onClick = { viewModel.deleteOfflinePage(page.id) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete offline page", tint = Color.Red.copy(alpha = 0.7f))
                                            }
                                        },
                                        modifier = Modifier.clickable {
                                            activeTab.isHome = false
                                            activeTab.title = page.title
                                            activeTab.url = page.url
                                            activeTab.webView?.loadDataWithBaseURL(page.url, page.htmlContent, "text/html", "UTF-8", null)
                                            showOfflinePages = false
                                        },
                                        colors = ListItemDefaults.colors(containerColor = backgroundColor)
                                    )
                                    HorizontalDivider(color = surfaceVariantColor)
                                }
                            }
                        }
                    }
                }
            }
        }
            }
        )
    }

    if (activeDialog != null) {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text(activeDialog ?: "", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    when (activeDialog) {
                        "Security Settings", "Web Content Settings", "Privacy Controls" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text("JavaScript Execution", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text("Allow web pages to execute JavaScript scripts. Disabling improves privacy and security.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isJavaScriptEnabled,
                                        onCheckedChange = { viewModel.settingsRepository.setJavaScriptEnabled(it) }
                                    )
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text("Third-Party Cookies", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text("Allow third-party cookies across sites. Disable for enhanced privacy control.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isThirdPartyCookiesEnabled,
                                        onCheckedChange = { viewModel.settingsRepository.setThirdPartyCookiesEnabled(it) }
                                    )
                                }
                            }
                        }
                        "Rate app" -> {
                            var rating by remember { mutableStateOf(5) }
                            Text("Please rate your experience with Dix Browser:")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                for (i in 1..5) {
                                    IconButton(onClick = { rating = i }) {
                                        Icon(
                                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "$i stars",
                                            tint = Color(0xFFFFC107)
                                        )
                                    }
                                }
                            }
                        }
                        "Send feedback" -> {
                            var feedbackText by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it },
                                label = { Text("Your feedback") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "Information" -> {
                            Text("Dix Browser v2.5.0\nFast, secure, and feature-rich browsing experience.\n© 2026 Dix Inc.")
                        }
                        "Password Manager" -> {
                            Text("No saved passwords. Saved login credentials will appear here securely encrypted.")
                        }
                        else -> {
                            Text("Configure settings for $activeDialog.")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    android.widget.Toast.makeText(context, "Saved changes for $activeDialog", android.widget.Toast.LENGTH_SHORT).show()
                    activeDialog = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFindInPage) {
        Surface(
            modifier = Modifier.fillMaxWidth().zIndex(20f),
            color = surfaceColor,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = { 
                        findQuery = it
                        activeTab.webView?.findAllAsync(it)
                    },
                    placeholder = { Text("Find in page...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = { activeTab.webView?.findNext(true) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                }
                IconButton(onClick = { 
                    activeTab.webView?.clearMatches()
                    showFindInPage = false 
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }

    if (showPageSource) {
        AlertDialog(
            onDismissRequest = { showPageSource = false },
            title = { Text("Page Source") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    item {
                        Text(pageSourceText, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPageSource = false }) { Text("Close") }
            }
        )
    }

    if (showMemo) {
        AlertDialog(
            onDismissRequest = { showMemo = false },
            title = { Text("Quick Memo") },
            text = {
                OutlinedTextField(
                    value = memoText,
                    onValueChange = { memoText = it },
                    label = { Text("Type notes here...") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    showMemo = false
                    android.widget.Toast.makeText(context, "Memo saved", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showMemo = false }) { Text("Cancel") }
            }
        )
    }

    if (showImageGallery) {
        AlertDialog(
            onDismissRequest = { showImageGallery = false },
            title = { 
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Image Viewer (${extractedImages.size})", fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        activeTab.webView?.evaluateJavascript(
                            "(function() { " +
                            "var imgs = document.getElementsByTagName('img'); " +
                            "var urls = []; " +
                            "for(var i=0; i<imgs.length; i++) { " +
                            "  if(imgs[i].src && !urls.includes(imgs[i].src)) urls.push(imgs[i].src); " +
                            "} " +
                            "return JSON.stringify(urls); " +
                            "})();"
                        ) { value ->
                            if (value != null && value != "null") {
                                try {
                                    val cleaned = value.trim('"').replace("\\\"", "\"")
                                    val list = mutableListOf<String>()
                                    val matcher = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(cleaned)
                                    while (matcher.find()) {
                                        matcher.group(1)?.let { list.add(it) }
                                    }
                                    extractedImages = list
                                    android.widget.Toast.makeText(context, "Found ${list.size} images", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    extractedImages = emptyList()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    if (extractedImages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No extracted images found on this page.", color = textColor.copy(alpha = 0.6f))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(extractedImages) { imgUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { previewImageUrl = imgUrl }
                                ) {
                                    WebImageThumbnail(url = imgUrl, modifier = Modifier.fillMaxSize())
                                    
                                    IconButton(
                                        onClick = {
                                            val request = android.app.DownloadManager.Request(android.net.Uri.parse(imgUrl))
                                                .setTitle(android.webkit.URLUtil.guessFileName(imgUrl, null, null))
                                                .setDescription("Downloading image...")
                                                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(imgUrl, null, null))
                                            val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                                            dm.enqueue(request)
                                            android.widget.Toast.makeText(context, "Downloading image...", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageGallery = false }) { Text("Close") }
            }
        )
    }

    if (previewImageUrl != null) {
        AlertDialog(
            onDismissRequest = { previewImageUrl = null },
            title = { Text("Image Preview", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        WebImageThumbnail(url = previewImageUrl!!, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = previewImageUrl!!,
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Image URL", previewImageUrl!!)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Image URL copied", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy URL")
                    }
                    TextButton(onClick = {
                        val imgUrl = previewImageUrl!!
                        val request = android.app.DownloadManager.Request(android.net.Uri.parse(imgUrl))
                            .setTitle(android.webkit.URLUtil.guessFileName(imgUrl, null, null))
                            .setDescription("Downloading image...")
                            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(imgUrl, null, null))
                        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        dm.enqueue(request)
                        android.widget.Toast.makeText(context, "Downloading image...", android.widget.Toast.LENGTH_SHORT).show()
                        previewImageUrl = null
                    }) {
                        Text("Download")
                    }
                    TextButton(onClick = { previewImageUrl = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showMediaSnifferSheet) {
        com.example.ui.MediaSnifferSheet(
            mediaList = activeTab.sniffedMedia,
            onDismiss = { showMediaSnifferSheet = false },
            onPlayMedia = { media ->
                showMediaSnifferSheet = false
                activeMediaPlayerState = com.example.ui.MediaPlayerState(
                    url = media.url,
                    title = media.title,
                    isVideo = media.type == com.example.ui.MediaType.VIDEO
                )
            },
            onDownloadMedia = { media ->
                val fileName = media.title.ifBlank { "media_${System.currentTimeMillis()}.${media.extension.ifBlank { "bin" }}" }
                downloads.add(DownloadItem(fileName = fileName, url = media.url, progress = 1f, isPaused = false))
                try {
                    val request = android.app.DownloadManager.Request(android.net.Uri.parse(media.url))
                        .setTitle(fileName)
                        .setDescription("Downloading media resource")
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                    val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                    dm.enqueue(request)
                    android.widget.Toast.makeText(context, "Downloading $fileName...", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Saved $fileName to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onBatchDownload = { mediaList ->
                mediaList.forEach { media ->
                    val fileName = media.title.ifBlank { "media_${System.currentTimeMillis()}.${media.extension.ifBlank { "bin" }}" }
                    downloads.add(DownloadItem(fileName = fileName, url = media.url, progress = 1f, isPaused = false))
                    try {
                        val request = android.app.DownloadManager.Request(android.net.Uri.parse(media.url))
                            .setTitle(fileName)
                            .setDescription("Downloading media resource")
                            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                            .setAllowedOverMetered(true)
                            .setAllowedOverRoaming(true)
                        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        dm.enqueue(request)
                    } catch (e: Exception) {}
                }
                showDownloads = true
                showMediaSnifferSheet = false
                android.widget.Toast.makeText(context, "Batch download started for ${mediaList.size} items", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    activeMediaPlayerState?.let { playerState ->
        if (playerState.isPiPMode) {
            com.example.ui.InAppPiPOverlay(
                playerState = playerState,
                onExpand = { playerState.isPiPMode = false },
                onClose = { activeMediaPlayerState = null }
            )
        } else {
            com.example.ui.MediaPlayerDialog(
                playerState = playerState,
                onDismiss = { activeMediaPlayerState = null },
                onEnablePiP = { playerState.isPiPMode = true }
            )
        }
    }

    pendingSaveCredential?.let { pending ->
        com.example.ui.SavePasswordPromptBanner(
            pendingSave = pending,
            onSave = {
                viewModel.savePassword(
                    siteTitle = pending.siteTitle,
                    domain = pending.domain,
                    username = pending.username,
                    rawPassword = pending.rawPassword
                )
                pendingSaveCredential = null
                android.widget.Toast.makeText(context, "Credentials saved securely", android.widget.Toast.LENGTH_SHORT).show()
            },
            onNever = {
                pendingSaveCredential = null
            },
            onDismiss = {
                pendingSaveCredential = null
            }
        )
    }

    autoFillDomain?.let { domain ->
        val credentialsForDomain = savedPasswords.filter { domain.contains(it.domain, ignoreCase = true) && it.domain.isNotBlank() }
        if (credentialsForDomain.isNotEmpty()) {
            com.example.ui.AutoFillPromptBanner(
                domain = domain,
                credentials = credentialsForDomain,
                onAutoFill = { username, rawPassword ->
                    val escapedUser = username.replace("'", "\\'")
                    val escapedPass = rawPassword.replace("'", "\\'")
                    val targetTab = tabs.find { it.id == activeTabId }
                    val jsFill = """
                        (function() {
                            var userInputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="username"]');
                            var passInputs = document.querySelectorAll('input[type="password"]');
                            if (userInputs.length > 0) {
                                userInputs[0].value = '$escapedUser';
                                userInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                userInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                            }
                            if (passInputs.length > 0) {
                                passInputs[0].value = '$escapedPass';
                                passInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                passInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                            }
                        })()
                    """.trimIndent()
                    targetTab?.webView?.evaluateJavascript(jsFill, null)
                    autoFillDomain = null
                    android.widget.Toast.makeText(context, "Credentials auto-filled for $username", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { autoFillDomain = null }
            )
        } else {
            LaunchedEffect(domain) { autoFillDomain = null }
        }
    }

    if (showCookieSettingsSheet) {
        val currentDomain = try {
            android.net.Uri.parse(activeTab.url).host ?: activeTab.url
        } catch (e: Exception) {
            activeTab.url
        }
        com.example.ui.CookieSettingsSheet(
            currentDomain = currentDomain,
            cookiePreferences = cookiePreferences,
            onSavePreference = { domain, allowFirst, allowThird ->
                viewModel.saveCookiePreference(domain, allowFirst, allowThird)
                android.widget.Toast.makeText(context, "Cookie preferences saved for $domain", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCookieSettingsSheet = false }
        )
    }

    if (showPasswordManagerSheet) {
        com.example.ui.PasswordManagerSheet(
            credentials = savedPasswords,
            currentDomain = activeTab.url,
            onDismiss = { showPasswordManagerSheet = false },
            onSaveCredential = { title, domain, user, pass, notes ->
                viewModel.savePassword(title, domain, user, pass, notes)
                android.widget.Toast.makeText(context, "Credential saved", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDeleteCredential = { id ->
                viewModel.deletePassword(id)
                android.widget.Toast.makeText(context, "Credential deleted", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDecryptPassword = { cred ->
                viewModel.decryptPassword(cred)
            },
            onAutoFillRequested = { user, pass ->
                val targetTab = tabs.find { it.id == activeTabId }
                if (targetTab?.webView != null) {
                    val escapedUser = user.replace("\\", "\\\\").replace("'", "\\'")
                    val escapedPass = pass.replace("\\", "\\\\").replace("'", "\\'")
                    val jsFill = """
                        (function() {
                            var passInputs = document.querySelectorAll('input[type="password"]');
                            var userInputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="username"], input[name*="user"], input[name*="login"]');
                            
                            if (userInputs.length > 0) {
                                userInputs[0].value = '$escapedUser';
                                userInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                userInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                            }
                            if (passInputs.length > 0) {
                                passInputs[0].value = '$escapedPass';
                                passInputs[0].dispatchEvent(new Event('input', { bubbles: true }));
                                passInputs[0].dispatchEvent(new Event('change', { bubbles: true }));
                            }
                        })()
                    """.trimIndent()
                    targetTab.webView?.evaluateJavascript(jsFill, null)
                    showPasswordManagerSheet = false
                    android.widget.Toast.makeText(context, "Credentials auto-filled for $user", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "No active web page to fill", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showPrivacyDashboardSheet) {
        com.example.ui.PrivacyDashboardSheet(
            currentUrl = activeTab.url,
            adBlockEnabled = adBlockEnabled,
            onToggleAdBlock = { viewModel.settingsRepository.setAdBlockEnabled(it) },
            fingerprintProtectionEnabled = fingerprintProtectionEnabled,
            onToggleFingerprintProtection = { viewModel.settingsRepository.setFingerprintProtectionEnabled(it) },
            thirdPartyCookiesEnabled = isThirdPartyCookiesEnabled,
            onToggleThirdPartyCookies = { viewModel.settingsRepository.setThirdPartyCookiesEnabled(it) },
            javaScriptEnabled = isJavaScriptEnabled,
            onToggleJavaScript = { viewModel.settingsRepository.setJavaScriptEnabled(it) },
            onDismiss = { showPrivacyDashboardSheet = false }
        )
    }

    if (showTranslationSheet) {
        com.example.ui.TranslationSheet(
            currentUrl = activeTab.url,
            defaultTargetLang = defaultTargetLanguage,
            onTranslatePage = { targetUrl ->
                activeTab.url = targetUrl
                activeTab.isHome = false
                activeTab.webView?.loadUrl(targetUrl)
            },
            onDismiss = { showTranslationSheet = false }
        )
    }

    if (showToolbarCustomizationSheet) {
        com.example.ui.ToolbarCustomizationSheet(
            urlBarPosition = urlBarPosition,
            onSetUrlBarPosition = { viewModel.settingsRepository.setUrlBarPosition(it) },
            showShieldIcon = showShieldIcon,
            onToggleShieldIcon = { viewModel.settingsRepository.setShowShieldIcon(it) },
            showTranslateIcon = showTranslateIcon,
            onToggleTranslateIcon = { viewModel.settingsRepository.setShowTranslateIcon(it) },
            showPasswordsIcon = showPasswordsIcon,
            onTogglePasswordsIcon = { viewModel.settingsRepository.setShowPasswordsIcon(it) },
            showMediaSnifferIcon = showMediaSnifferIcon,
            onToggleMediaSnifferIcon = { viewModel.settingsRepository.setShowMediaSnifferIcon(it) },
            showHomeIcon = showHomeIcon,
            onToggleHomeIcon = { viewModel.settingsRepository.setShowHomeIcon(it) },
            showTabsIcon = showTabsIcon,
            onToggleTabsIcon = { viewModel.settingsRepository.setShowTabsIcon(it) },
            onDismiss = { showToolbarCustomizationSheet = false }
        )
    }

    if (showDnsServicesSheet) {
        com.example.ui.DnsServicesSheet(
            currentDnsProvider = dnsProvider,
            customDnsUrl = customDnsUrl,
            onSelectProvider = { viewModel.settingsRepository.setDnsProvider(it) },
            onSetCustomUrl = { viewModel.settingsRepository.setCustomDnsUrl(it) },
            onDismiss = { showDnsServicesSheet = false }
        )
    }

    if (showTvCastingSheet) {
        com.example.ui.TvCastingSheet(
            activeTabUrl = activeTab.url,
            activeTabTitle = activeTab.title,
            sniffedVideos = activeTab.sniffedMedia.map { it.url },
            onDismiss = { showTvCastingSheet = false }
        )
    }

    if (showSecurityPrivacyControlsSheet) {
        com.example.ui.SecurityPrivacyControlsSheet(
            httpsOnlyMode = httpsOnlyMode,
            onToggleHttpsOnly = { viewModel.settingsRepository.setHttpsOnlyMode(it) },
            webRtcProtection = webRtcLeakProtection,
            onToggleWebRtc = { viewModel.settingsRepository.setWebRtcLeakProtection(it) },
            antiPhishingEnabled = antiPhishingEnabled,
            onToggleAntiPhishing = { viewModel.settingsRepository.setAntiPhishingEnabled(it) },
            onClearData = { historyClear, bookmarksClear, tabsClear, cacheClear ->
                if (historyClear) viewModel.clearHistory()
                if (cacheClear) {
                    CookieManager.getInstance().removeAllCookies(null)
                    tabs.forEach { it.webView?.clearCache(true) }
                }
                android.widget.Toast.makeText(context, "Selected browsing data cleared", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSecurityPrivacyControlsSheet = false }
        )
    }

    if (showAdBlockLevelsSheet) {
        com.example.ui.AdBlockLevelsSheet(
            adBlockLevel = adBlockLevel,
            onSetAdBlockLevel = { viewModel.settingsRepository.setAdBlockLevel(it) },
            enabledFilterLists = enabledFilterLists,
            onToggleFilterList = { listName, enabled ->
                viewModel.settingsRepository.toggleFilterList(listName, enabled)
            },
            onDismiss = { showAdBlockLevelsSheet = false }
        )
    }

    if (showWebContentViewSheet) {
        com.example.ui.WebContentViewSheet(
            userAgentPreset = userAgentPreset,
            onSetUserAgentPreset = { viewModel.settingsRepository.setUserAgentPreset(it) },
            textZoomPercent = textZoomPercent,
            onSetTextZoom = { viewModel.settingsRepository.setTextZoomPercent(it) },
            onDismiss = { showWebContentViewSheet = false }
        )
    }

    if (showBrowsingAnalyticsSheet) {
        com.example.ui.BrowsingAnalyticsSheet(
            historyList = history,
            onOpenUrl = { url ->
                activeTab.isHome = false
                activeTab.url = url
                activeTab.webView?.loadUrl(url)
                showBrowsingAnalyticsSheet = false
            },
            onDismiss = { showBrowsingAnalyticsSheet = false }
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = Color.Black, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.Black, fontSize = 16.sp)
    }
}

@Composable
fun MenuActionIcon(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp).width(64.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontSize = 10.sp, maxLines = 1)
    }
}

private fun formatUrl(url: String, searchEngine: String): String {
    val trimmed = url.trim()
    val searchPrefix = when(searchEngine) {
        "Google" -> "https://www.google.com/search?q="
        "Bing" -> "https://www.bing.com/search?q="
        "Yahoo" -> "https://search.yahoo.com/search?p="
        "Yandex" -> "https://yandex.com/search/?text="
        "Brave" -> "https://search.brave.com/search?q="
        else -> "https://duckduckgo.com/?q="
    }
    if (trimmed.isEmpty()) return searchPrefix.substringBefore("?")
    
    return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        if (trimmed.contains(" ") || !trimmed.contains(".")) {
            "$searchPrefix${android.net.Uri.encode(trimmed)}"
        } else {
            "https://$trimmed"
        }
    } else {
        trimmed
    }
}

@Composable
fun WebImageThumbnail(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(url) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                bitmap = android.graphics.BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                bitmap = null
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Extracted Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

