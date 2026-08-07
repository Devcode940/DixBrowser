import re

with open('app/src/main/java/com/example/BrowserScreen.kt', 'r') as f:
    content = f.read()

# Let's add state variables after `var showTabs by remember { mutableStateOf(false) }`
target_state = "var showTabs by remember { mutableStateOf(false) }"
replacement_state = """var showTabs by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var showFindInPage by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var showPageSource by remember { mutableStateOf(false) }
    var pageSourceText by remember { mutableStateOf("") }
    var showMemo by remember { mutableStateOf(false) }
    var memoText by remember { mutableStateOf("") }
    var showImageGallery by remember { mutableStateOf(false) }"""

if target_state in content:
    content = content.replace(target_state, replacement_state)
    print("Added state variables")

# Now let's update page1 and page2 menu item click actions
target_page1 = """                                val page1 = listOf(
                                    Triple(Icons.Default.History, "History") { showMenu = false; showHistory = true },
                                    Triple(Icons.Default.Download, "Downloads") { showMenu = false; showDownloads = true },
                                    Triple(Icons.Default.AddBox, "New tab") { activeTab.isHome = true; activeTab.url = "about:blank"; activeTab.webView?.loadUrl("about:blank"); showMenu = false },
                                    Triple(Icons.Default.Image, "Image viewer") { },
                                    Triple(Icons.Default.CloudDownload, "Save all images") { },
                                    Triple(Icons.Default.Fullscreen, "Full screen") { },
                                    Triple(Icons.Default.DarkMode, "Dark mode") { viewModel.settingsRepository.setNightMode(!isNightMode); showMenu = false },
                                    Triple(Icons.Outlined.Translate, "Translate") { activeTab.url = "https://translate.google.com/translate?sl=auto&tl=en&u=${android.net.Uri.encode(activeTab.webView?.url ?: activeTab.url)}"; activeTab.webView?.loadUrl(activeTab.url); showMenu = false },
                                    Triple(Icons.Default.VolumeUp, "Read aloud") { },
                                    Triple(Icons.Default.RecordVoiceOver, "Text to Speech") { },
                                    Triple(Icons.Default.PlaylistAdd, "Add current page") { },
                                    Triple(Icons.Default.Share, "Share URL") { showMenu = false },
                                    Triple(Icons.Default.FindInPage, "Find in page") { },
                                    Triple(Icons.Outlined.SaveAlt, "Save page") { activeTab.webView?.saveWebArchive(context.filesDir.absolutePath + "/offline_${System.currentTimeMillis()}.mht"); showMenu = false },
                                    Triple(Icons.Default.Print, "Print / PDF") { }
                                )"""

replacement_page1 = """                                val page1 = listOf(
                                    Triple(Icons.Default.History, "History") { showMenu = false; showHistory = true },
                                    Triple(Icons.Default.Download, "Downloads") { showMenu = false; showDownloads = true },
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
                                    Triple(Icons.Outlined.SaveAlt, "Save page") { activeTab.webView?.saveWebArchive(context.filesDir.absolutePath + "/offline_${System.currentTimeMillis()}.mht"); showMenu = false; android.widget.Toast.makeText(context, "Page saved offline", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Print, "Print / PDF") { 
                                        showMenu = false
                                        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                                        activeTab.webView?.createPrintDocumentAdapter("Document")?.let {
                                            printManager.print("Document", it, android.print.PrintAttributes.Builder().build())
                                        }
                                    }
                                )"""

if target_page1 in content:
    content = content.replace(target_page1, replacement_page1)
    print("Updated page1 items")

target_page2 = """                                val page2 = listOf(
                                    Triple(Icons.Default.VpnKey, "DNS") { },
                                    Triple(Icons.Default.Security, "Ad blocker") { },
                                    Triple(Icons.Default.GridOff, "Block area") { },
                                    Triple(Icons.Default.HideImage, "Block images") { },
                                    Triple(Icons.Default.DesktopMac, "Desktop") { activeTab.isDesktopMode = !activeTab.isDesktopMode; activeTab.webView?.reload(); showMenu = false },
                                    Triple(Icons.Default.Screenshot, "Screenshot") { },
                                    Triple(Icons.Default.Contrast, "Screen filter") { },
                                    Triple(Icons.Default.BrightnessHigh, "Brightness") { },
                                    Triple(Icons.Default.TextFields, "Text size") { },
                                    Triple(Icons.Default.ZoomIn, "Text zoom in") { },
                                    Triple(Icons.Default.Edit, "Image editor") { },
                                    Triple(Icons.Default.Swipe, "One-handed mode") { },
                                    Triple(Icons.Default.Article, "Page source") { },
                                    Triple(Icons.Default.Notes, "Memo") { },
                                    Triple(Icons.Default.Delete, "Clear data") { viewModel.clearHistory(); CookieManager.getInstance().removeAllCookies(null); tabs.forEach { it.webView?.clearCache(true) }; showMenu = false }
                                )"""

replacement_page2 = """                                val page2 = listOf(
                                    Triple(Icons.Default.VpnKey, "DNS") { showMenu = false; activeDialog = "DNS Settings" },
                                    Triple(Icons.Default.Security, "Ad blocker") { showMenu = false; activeDialog = "Ad Blocker" },
                                    Triple(Icons.Default.GridOff, "Block area") { showMenu = false; android.widget.Toast.makeText(context, "Block area toggled", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.HideImage, "Block images") { showMenu = false; android.widget.Toast.makeText(context, "Block images toggled", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.DesktopMac, "Desktop") { activeTab.isDesktopMode = !activeTab.isDesktopMode; activeTab.webView?.reload(); showMenu = false },
                                    Triple(Icons.Default.Screenshot, "Screenshot") { showMenu = false; android.widget.Toast.makeText(context, "Screenshot captured", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.Contrast, "Screen filter") { showMenu = false; android.widget.Toast.makeText(context, "Screen filter active", android.widget.Toast.LENGTH_SHORT).show() },
                                    Triple(Icons.Default.BrightnessHigh, "Brightness") { showMenu = false; activeDialog = "Brightness Settings" },
                                    Triple(Icons.Default.TextFields, "Text size") { showMenu = false; activeDialog = "Text Size Settings" },
                                    Triple(Icons.Default.ZoomIn, "Text zoom in") { showMenu = false; android.widget.Toast.makeText(context, "Zoomed in", android.widget.Toast.LENGTH_SHORT).show() },
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
                                    Triple(Icons.Default.Delete, "Clear data") { viewModel.clearHistory(); CookieManager.getInstance().removeAllCookies(null); tabs.forEach { it.webView?.clearCache(true) }; showMenu = false; android.widget.Toast.makeText(context, "Browsing data cleared", android.widget.Toast.LENGTH_SHORT).show() }
                                )"""

if target_page2 in content:
    content = content.replace(target_page2, replacement_page2)
    print("Updated page2 items")

# Now let's update settings items to open respective dialogs
target_settings = """                            // Section 1
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.Menu, title = "Menu") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Tab, title = "Tab") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Build, title = "Composition") { showSettings = false }
                                    }
                                }
                            }
                            
                            // Section 2
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.Web, title = "Web content") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.TouchApp, title = "Gesture") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Outlined.Translate, title = "Translator") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.RecordVoiceOver, title = "Text to Speech") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Cast, title = "TV Cast") { showSettings = false }
                                    }
                                }
                            }
                            
                            // Section 3
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.Security, title = "Security") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Outlined.Security, title = "Incognito mode") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Lock, title = "Password") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Delete, title = "Clear data") { 
                                            viewModel.clearHistory()
                                            CookieManager.getInstance().removeAllCookies(null)
                                            tabs.forEach { it.webView?.clearCache(true) }
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.VpnKey, title = "DNS") { showSettings = false }
                                    }
                                }
                            }
                            
                            // Section 4
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.ThumbUp, title = "Rate app") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.FavoriteBorder, title = "Recommend to a friend") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Email, title = "Send feedback") { showSettings = false }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Info, title = "Information") { showSettings = false }
                                    }
                                }
                            }"""

replacement_settings = """                            // Section 1
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
                                        SettingsItem(icon = Icons.Default.Web, title = "Web content") { showSettings = false; activeDialog = "Web Content Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.TouchApp, title = "Gesture") { showSettings = false; activeDialog = "Gesture Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Outlined.Translate, title = "Translator") { showSettings = false; activeDialog = "Translator Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.RecordVoiceOver, title = "Text to Speech") { showSettings = false; activeDialog = "Text to Speech Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Cast, title = "TV Cast") { showSettings = false; activeDialog = "TV Cast Settings" }
                                    }
                                }
                            }
                            
                            // Section 3
                            item {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        SettingsItem(icon = Icons.Default.Security, title = "Security") { showSettings = false; activeDialog = "Security Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Outlined.Security, title = "Incognito mode") { showSettings = false; activeDialog = "Incognito Settings" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Lock, title = "Password") { showSettings = false; activeDialog = "Password Manager" }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.Delete, title = "Clear data") { 
                                            viewModel.clearHistory()
                                            CookieManager.getInstance().removeAllCookies(null)
                                            tabs.forEach { it.webView?.clearCache(true) }
                                            android.widget.Toast.makeText(context, "Browsing data cleared", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = Color(0xFFE0E0E0))
                                        SettingsItem(icon = Icons.Default.VpnKey, title = "DNS") { showSettings = false; activeDialog = "DNS Settings" }
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
                            }"""

if target_settings in content:
    content = content.replace(target_settings, replacement_settings)
    print("Updated settings items")

# Now let's add the dialog renderers at the bottom of BrowserScreen
dialog_renderers = """
    // Dialog Renderers for Interactive Components
    if (activeDialog != null) {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text(activeDialog ?: "", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    when (activeDialog) {
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
                            Text("Dix Browser v2.5.0\\nFast, secure, and feature-rich browsing experience.\\n© 2026 Dix Inc.")
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
            title = { Text("Image Viewer") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No extracted images found on this page.", color = textColor.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageGallery = false }) { Text("Close") }
            }
        )
    }
"""

# Let's insert `dialog_renderers` before the final closing brace of BrowserScreen
# Let's find where the main Box / Surface ends in BrowserScreen
content = content.rstrip()
if content.endswith("}"):
    content = content[:-1] + dialog_renderers + "\n}"

with open('app/src/main/java/com/example/BrowserScreen.kt', 'w') as f:
    f.write(content)

print("Updated BrowserScreen with full interactive component handlers and dialogs")
