package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AdBlocker
import com.example.BlockCategory
import com.example.BlockedRequestLog
import com.example.LanguageOption
import com.example.TranslationLanguages
import com.example.TranslationService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardSheet(
    currentUrl: String,
    adBlockEnabled: Boolean,
    onToggleAdBlock: (Boolean) -> Unit,
    fingerprintProtectionEnabled: Boolean,
    onToggleFingerprintProtection: (Boolean) -> Unit,
    thirdPartyCookiesEnabled: Boolean,
    onToggleThirdPartyCookies: (Boolean) -> Unit,
    javaScriptEnabled: Boolean,
    onToggleJavaScript: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val domain = remember(currentUrl) {
        try {
            android.net.Uri.parse(currentUrl).host ?: currentUrl
        } catch (e: Exception) {
            "Current Web Page"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF181825),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF89B4FA).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = Color(0xFF89B4FA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy Protection",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = domain,
                            fontSize = 12.sp,
                            color = Color(0xFFA6ADC8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Total Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${AdBlocker.blockedCount}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFA6E3A1)
                    )
                    Text(
                        text = "Total Trackers & Ads Blocked This Session",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFBAC2DE),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Category Counters Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadgeItem(
                            count = AdBlocker.blockedAdsCount,
                            label = "Ads",
                            icon = Icons.Default.Block,
                            color = Color(0xFFF38BA8)
                        )
                        StatBadgeItem(
                            count = AdBlocker.blockedTrackersCount,
                            label = "Trackers",
                            icon = Icons.Default.Radar,
                            color = Color(0xFFFAB387)
                        )
                        StatBadgeItem(
                            count = AdBlocker.blockedSocialCount,
                            label = "Social Pixels",
                            icon = Icons.Default.Share,
                            color = Color(0xFF89B4FA)
                        )
                        StatBadgeItem(
                            count = AdBlocker.blockedScriptsCount,
                            label = "Scripts",
                            icon = Icons.Default.Code,
                            color = Color(0xFFCBA6F7)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            PrivacyStatsBarChart()
            
            Spacer(modifier = Modifier.height(16.dp))

            // Protection Controls
            Text(
                text = "Protection Controls",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF89B4FA)
            )
            Spacer(modifier = Modifier.height(8.dp))

            ProtectionToggleRow(
                title = "Ad & Tracker Blocker",
                subtitle = "Blocks ad networks, analytics & tracking pixels",
                checked = adBlockEnabled,
                onCheckedChange = onToggleAdBlock
            )
            ProtectionToggleRow(
                title = "Fingerprint Protection",
                subtitle = "Disables device canvas & audio fingerprinting probes",
                checked = fingerprintProtectionEnabled,
                onCheckedChange = onToggleFingerprintProtection
            )
            ProtectionToggleRow(
                title = "Block 3rd-Party Cookies",
                subtitle = "Prevents cross-site behavioral tracking cookies",
                checked = thirdPartyCookiesEnabled,
                onCheckedChange = onToggleThirdPartyCookies
            )
            ProtectionToggleRow(
                title = "JavaScript Engine",
                subtitle = "Enable or disable JS execution for current pages",
                checked = javaScriptEnabled,
                onCheckedChange = onToggleJavaScript
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Block Log (${AdBlocker.blockedLogs.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF89B4FA)
                )
                if (AdBlocker.blockedLogs.isNotEmpty()) {
                    TextButton(onClick = { AdBlocker.resetStats() }) {
                        Text("Reset Stats", color = Color(0xFFF38BA8), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (AdBlocker.blockedLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (adBlockEnabled) "No ad requests detected on this page yet." else "Ad blocker is paused.",
                        color = Color(0xFF6C7086),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(AdBlocker.blockedLogs.reversed()) { log ->
                        BlockedLogItem(log)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StatBadgeItem(
    count: Int,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = Color(0xFFA6ADC8))
    }
}

@Composable
private fun ProtectionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFA6ADC8))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFA6E3A1),
                uncheckedThumbColor = Color(0xFFBAC2DE),
                uncheckedTrackColor = Color(0xFF313244)
            )
        )
    }
}

@Composable
private fun BlockedLogItem(log: BlockedRequestLog) {
    val timeStr = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF313244).copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.domain,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = log.category.displayName,
                fontSize = 10.sp,
                color = when (log.category) {
                    BlockCategory.AD_NETWORK -> Color(0xFFF38BA8)
                    BlockCategory.ANALYTICS -> Color(0xFFFAB387)
                    BlockCategory.SOCIAL_PIXEL -> Color(0xFF89B4FA)
                    BlockCategory.SCRIPT_FINGERPRINT -> Color(0xFFCBA6F7)
                }
            )
        }
        Text(text = timeStr, fontSize = 10.sp, color = Color(0xFF6C7086))
    }
}

// -------------------------------------------------------------
// TRANSLATION SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSheet(
    currentUrl: String,
    defaultTargetLang: String,
    onTranslatePage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedTargetLang by remember { mutableStateOf(defaultTargetLang) }
    var customTextToTranslate by remember { mutableStateOf("") }
    var translatedResult by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var expandedLangMenu by remember { mutableStateOf(false) }

    val currentTargetLangOption = remember(selectedTargetLang) {
        TranslationLanguages.supportedLanguages.find { it.code == selectedTargetLang }
            ?: TranslationLanguages.supportedLanguages.first()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF181825),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GTranslate,
                        contentDescription = "Translate",
                        tint = Color(0xFF89B4FA),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Web & Text Translator",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Language Selector Bar
            Text(text = "Target Language", fontSize = 13.sp, color = Color(0xFF89B4FA), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF313244))
                    .clickable { expandedLangMenu = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentTargetLangOption.flag, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentTargetLangOption.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                }

                DropdownMenu(
                    expanded = expandedLangMenu,
                    onDismissRequest = { expandedLangMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E2E))
                ) {
                    TranslationLanguages.supportedLanguages.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = option.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = option.name, color = Color.White)
                                }
                            },
                            onClick = {
                                selectedTargetLang = option.code
                                expandedLangMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Page Translation Button
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Page Translation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "Translate the entire webpage into ${currentTargetLangOption.name} using free Web Translate engine.",
                        fontSize = 12.sp,
                        color = Color(0xFFA6ADC8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val targetUrl = TranslationService.getWebPageTranslationUrl(currentUrl, selectedTargetLang)
                            onTranslatePage(targetUrl)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89B4FA)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF11111B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate Current Web Page", color = Color(0xFF11111B), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Text / Selection Translation Tool
            Text(text = "Quick Text Translator (Free MyMemory API)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = customTextToTranslate,
                onValueChange = {
                    customTextToTranslate = it
                    errorMessage = null
                },
                placeholder = { Text("Paste or enter text here to translate...", color = Color(0xFF6C7086)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF89B4FA),
                    unfocusedBorderColor = Color(0xFF313244),
                    focusedContainerColor = Color(0xFF1E1E2E),
                    unfocusedContainerColor = Color(0xFF1E1E2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val clip = clipboardManager.getText()
                    if (clip != null) {
                        customTextToTranslate = clip.text
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF89B4FA))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste Text", color = Color(0xFF89B4FA), fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        if (customTextToTranslate.isNotBlank()) {
                            isTranslating = true
                            errorMessage = null
                            coroutineScope.launch {
                                val res = TranslationService.translateText(
                                    text = customTextToTranslate,
                                    targetLang = selectedTargetLang
                                )
                                isTranslating = false
                                res.onSuccess {
                                    translatedResult = it
                                }.onFailure {
                                    errorMessage = "Translation failed: ${it.localizedMessage}"
                                }
                            }
                        }
                    },
                    enabled = customTextToTranslate.isNotBlank() && !isTranslating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6E3A1)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Translate Text", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = Color(0xFFF38BA8), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            if (translatedResult.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF313244)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Translation (${currentTargetLangOption.name}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA6E3A1)
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(translatedResult))
                                    android.widget.Toast.makeText(context, "Copied translation", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = translatedResult, fontSize = 14.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// NAV & TOOLBAR CUSTOMIZATION SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarCustomizationSheet(
    urlBarPosition: String,
    onSetUrlBarPosition: (String) -> Unit,
    showShieldIcon: Boolean,
    onToggleShieldIcon: (Boolean) -> Unit,
    showTranslateIcon: Boolean,
    onToggleTranslateIcon: (Boolean) -> Unit,
    showPasswordsIcon: Boolean,
    onTogglePasswordsIcon: (Boolean) -> Unit,
    showMediaSnifferIcon: Boolean,
    onToggleMediaSnifferIcon: (Boolean) -> Unit,
    showHomeIcon: Boolean,
    onToggleHomeIcon: (Boolean) -> Unit,
    showTabsIcon: Boolean,
    onToggleTabsIcon: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF181825),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Customize",
                        tint = Color(0xFFCBA6F7),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Customize Navigation Bar",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // URL Bar Position Selector
            Text(text = "URL Bar Position", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBA6F7))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF313244))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (urlBarPosition == "Bottom") Color(0xFFCBA6F7) else Color.Transparent)
                        .clickable { onSetUrlBarPosition("Bottom") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.South,
                            contentDescription = null,
                            tint = if (urlBarPosition == "Bottom") Color(0xFF11111B) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bottom Bar",
                            fontWeight = FontWeight.Bold,
                            color = if (urlBarPosition == "Bottom") Color(0xFF11111B) else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (urlBarPosition == "Top") Color(0xFFCBA6F7) else Color.Transparent)
                        .clickable { onSetUrlBarPosition("Top") }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.North,
                            contentDescription = null,
                            tint = if (urlBarPosition == "Top") Color(0xFF11111B) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Top Bar",
                            fontWeight = FontWeight.Bold,
                            color = if (urlBarPosition == "Top") Color(0xFF11111B) else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Toolbar Quick Action Icons Toggles
            Text(text = "Quick Action Icons on URL Bar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBA6F7))
            Spacer(modifier = Modifier.height(8.dp))

            ProtectionToggleRow(
                title = "Privacy Protection Shield",
                subtitle = "Access real-time blocked ads & trackers dashboard",
                checked = showShieldIcon,
                onCheckedChange = onToggleShieldIcon
            )
            ProtectionToggleRow(
                title = "Translator Tool Icon",
                subtitle = "Translate pages or selected text instantly",
                checked = showTranslateIcon,
                onCheckedChange = onToggleTranslateIcon
            )
            ProtectionToggleRow(
                title = "Password Manager Vault",
                subtitle = "Quick access to saved credentials & auto-fill",
                checked = showPasswordsIcon,
                onCheckedChange = onTogglePasswordsIcon
            )
            ProtectionToggleRow(
                title = "Media Sniffer Radar",
                subtitle = "Detect audio & video media streams on active page",
                checked = showMediaSnifferIcon,
                onCheckedChange = onToggleMediaSnifferIcon
            )
            ProtectionToggleRow(
                title = "Home Button",
                subtitle = "Return to start page with custom search & shortcuts",
                checked = showHomeIcon,
                onCheckedChange = onToggleHomeIcon
            )
            ProtectionToggleRow(
                title = "Tabs Overview Counter",
                subtitle = "View and switch active browser tabs",
                checked = showTabsIcon,
                onCheckedChange = onToggleTabsIcon
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CookieSettingsSheet(
    currentDomain: String,
    cookiePreferences: List<com.example.data.CookiePreference>,
    onSavePreference: (String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val currentPref = cookiePreferences.find { it.domain == currentDomain }
    var allowFirstParty by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentPref?.allowFirstParty ?: true) }
    var allowThirdParty by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentPref?.allowThirdParty ?: false) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF181825),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Default.Cookie, contentDescription = "Cookie", tint = Color(0xFFF38BA8), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cookie Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                androidx.compose.material3.IconButton(onClick = onDismiss) {
                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Text(text = "Preferences for: $currentDomain", color = Color(0xFF89B4FA), fontWeight = FontWeight.SemiBold)
            
            Spacer(modifier = Modifier.height(16.dp))

            ProtectionToggleRow(
                title = "Allow First-Party Cookies",
                subtitle = "Required for logins and core site functionality",
                checked = allowFirstParty,
                onCheckedChange = { allowFirstParty = it }
            )
            
            ProtectionToggleRow(
                title = "Allow Third-Party Cookies",
                subtitle = "Allows cross-site tracking and embedded widgets",
                checked = allowThirdParty,
                onCheckedChange = { allowThirdParty = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.material3.Button(
                onClick = {
                    onSavePreference(currentDomain, allowFirstParty, allowThirdParty)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF89B4FA))
            ) {
                Text("Save Preferences", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
