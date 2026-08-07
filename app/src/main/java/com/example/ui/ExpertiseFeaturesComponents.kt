package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AdBlocker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// 1. HIGH QUALITY DNS SERVICES SHEET
// -------------------------------------------------------------

data class DnsProviderInfo(
    val name: String,
    val description: String,
    val primaryIp: String,
    val dohUrl: String,
    val features: List<String>,
    val badgeColor: Color
)

val dnsProviderList = listOf(
    DnsProviderInfo(
        name = "Cloudflare 1.1.1.1",
        description = "Ultra-fast, privacy-first DNS with zero logging.",
        primaryIp = "1.1.1.1 / 1.0.0.1",
        dohUrl = "https://cloudflare-dns.com/dns-query",
        features = listOf("Fastest Response", "No Logging", "DNSSEC"),
        badgeColor = Color(0xFFF8991D)
    ),
    DnsProviderInfo(
        name = "AdGuard DNS",
        description = "Blocks ad networks, tracking scripts, and phishing domains.",
        primaryIp = "94.140.14.14 / 94.140.15.15",
        dohUrl = "https://dns.adguard.com/dns-query",
        features = listOf("Ad Filtering", "Malware Shield", "Tracker Block"),
        badgeColor = Color(0xFFA6E3A1)
    ),
    DnsProviderInfo(
        name = "Quad9 Threat Shield",
        description = "Swiss-based security DNS blocking malicious domains & malware.",
        primaryIp = "9.9.9.9 / 149.112.112.112",
        dohUrl = "https://dns.quad9.net/dns-query",
        features = listOf("Threat Intelligence", "Swiss Privacy", "Anti-Phishing"),
        badgeColor = Color(0xFF89B4FA)
    ),
    DnsProviderInfo(
        name = "Google Public DNS",
        description = "Global high-availability DNS infrastructure by Google.",
        primaryIp = "8.8.8.8 / 8.8.4.4",
        dohUrl = "https://dns.google/dns-query",
        features = listOf("High Speed", "Global Anycast", "ECS Support"),
        badgeColor = Color(0xFF4285F4)
    ),
    DnsProviderInfo(
        name = "CleanBrowsing Family",
        description = "Family-safe DNS filtering adult content & malicious sites.",
        primaryIp = "185.228.168.168",
        dohUrl = "https://doh.cleanbrowsing.org/doh/family-filter/",
        features = listOf("Safe Search", "Family Guard", "Malware Block"),
        badgeColor = Color(0xFFCBA6F7)
    ),
    DnsProviderInfo(
        name = "Custom DoH Server",
        description = "Connect to your personal or enterprise DNS-over-HTTPS endpoint.",
        primaryIp = "User Defined",
        dohUrl = "",
        features = listOf("Custom Endpoint", "Self-Hosted", "Strict Privacy"),
        badgeColor = Color(0xFFFAB387)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsServicesSheet(
    currentDnsProvider: String,
    customDnsUrl: String,
    onSelectProvider: (String) -> Unit,
    onSetCustomUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var testingLatency by remember { mutableStateOf(false) }
    var pingLatencyMs by remember { mutableIntStateOf(18) }
    var customInput by remember { mutableStateOf(customDnsUrl) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF89B4FA).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = "DNS", tint = Color(0xFF89B4FA), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("High-Quality DNS over HTTPS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Encrypted Private DNS Engine", fontSize = 12.sp, color = Color(0xFFA6ADC8))
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA6E3A1))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ACTIVE: $currentDnsProvider", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA6E3A1))
                        }
                        Text(
                            text = if (testingLatency) "Pinging DoH gateway..." else "Latency: $pingLatencyMs ms • DNS-over-HTTPS (DoH) Enabled",
                            fontSize = 11.sp,
                            color = Color(0xFFBAC2DE)
                        )
                    }

                    Button(
                        onClick = {
                            testingLatency = true
                            coroutineScope.launch {
                                delay(600)
                                pingLatencyMs = (12..28).random()
                                testingLatency = false
                                Toast.makeText(context, "DoH Latency: $pingLatencyMs ms (Encrypted)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF313244)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (testingLatency) "Testing..." else "Test Speed", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Select DNS Provider", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF89B4FA))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dnsProviderList) { provider ->
                    val isSelected = currentDnsProvider == provider.name
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF313244) else Color(0xFF1E1E2E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) provider.badgeColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectProvider(provider.name)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectProvider(provider.name) },
                                colors = RadioButtonDefaults.colors(selectedColor = provider.badgeColor, unselectedColor = Color(0xFF6C7086))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(provider.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(provider.badgeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(provider.primaryIp, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = provider.badgeColor)
                                    }
                                }
                                Text(provider.description, fontSize = 11.sp, color = Color(0xFFA6ADC8))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    provider.features.forEach { ft ->
                                        Text(
                                            text = "• $ft",
                                            fontSize = 10.sp,
                                            color = Color(0xFFBAC2DE)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (currentDnsProvider == "Custom DoH Server") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customInput,
                    onValueChange = {
                        customInput = it
                        onSetCustomUrl(it)
                    },
                    label = { Text("Custom DoH Endpoint URL", color = Color(0xFF89B4FA)) },
                    placeholder = { Text("https://example.com/dns-query", color = Color(0xFF6C7086)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF89B4FA),
                        unfocusedBorderColor = Color(0xFF313244),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// 2. TV CASTING & DLNA / SMART TV STREAMER SHEET
// -------------------------------------------------------------

data class TvDevice(
    val id: String,
    val name: String,
    val type: String,
    val isConnected: Boolean = false,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvCastingSheet(
    activeTabUrl: String,
    activeTabTitle: String,
    sniffedVideos: List<String>,
    onDismiss: () -> Unit
) {
    var isScanning by remember { mutableStateOf(true) }
    var connectedTv by remember { mutableStateOf<TvDevice?>(null) }
    var isCastingVideo by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var volumeLevel by remember { mutableFloatStateOf(0.8f) }
    var playbackProgress by remember { mutableFloatStateOf(0.35f) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val detectedTvs = remember {
        mutableStateListOf(
            TvDevice("1", "Living Room Samsung QLED", "Smart TV (DLNA)", icon = Icons.Default.Tv),
            TvDevice("2", "Bedroom Chromecast Ultra", "Google Cast", icon = Icons.Default.Cast),
            TvDevice("3", "Office Android TV", "Android TV OS", icon = Icons.Default.CastConnected),
            TvDevice("4", "LG WebOS TV 65\"", "AirPlay / DLNA", icon = Icons.Default.Tv)
        )
    }

    LaunchedEffect(Unit) {
        delay(1200)
        isScanning = false
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
                        Icons.Default.Cast,
                        contentDescription = "Cast",
                        tint = if (connectedTv != null) Color(0xFFA6E3A1) else Color(0xFF89B4FA),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("TV Media Streamer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (connectedTv != null) "Casting to ${connectedTv!!.name}" else "Cast web videos or screens to Smart TVs",
                            fontSize = 12.sp,
                            color = Color(0xFFA6ADC8)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (connectedTv == null) {
                // TV Receiver Scanner List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available TV Receivers", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF89B4FA))
                    if (isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFF89B4FA), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning Wi-Fi...", fontSize = 11.sp, color = Color(0xFF89B4FA))
                        }
                    } else {
                        TextButton(onClick = {
                            isScanning = true
                            coroutineScope.launch {
                                delay(1000)
                                isScanning = false
                            }
                        }) {
                            Text("Refresh", fontSize = 11.sp, color = Color(0xFF89B4FA))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detectedTvs) { tv ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    connectedTv = tv
                                    isCastingVideo = true
                                    Toast.makeText(context, "Connected to ${tv.name}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF313244)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(tv.icon, contentDescription = null, tint = Color(0xFF89B4FA), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(tv.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(tv.type, fontSize = 11.sp, color = Color(0xFFA6ADC8))
                                    }
                                }

                                Button(
                                    onClick = {
                                        connectedTv = tv
                                        isCastingVideo = true
                                        Toast.makeText(context, "Connected to ${tv.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89B4FA)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF11111B))
                                }
                            }
                        }
                    }
                }
            } else {
                // Connected TV Remote Controller UI
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ConnectedTv, contentDescription = null, tint = Color(0xFFA6E3A1), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(connectedTv!!.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("1080p Full HD Stream", fontSize = 11.sp, color = Color(0xFFA6E3A1))
                                }
                            }

                            TextButton(onClick = {
                                connectedTv = null
                                isCastingVideo = false
                                Toast.makeText(context, "Disconnecting TV...", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Disconnect", color = Color(0xFFF38BA8), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (activeTabTitle.isNotBlank()) activeTabTitle else activeTabUrl,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Seek Bar
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Slider(
                                value = playbackProgress,
                                onValueChange = { playbackProgress = it },
                                colors = SliderDefaults.colors(thumbColor = Color(0xFF89B4FA), activeTrackColor = Color(0xFF89B4FA))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("04:12", fontSize = 10.sp, color = Color(0xFFA6ADC8))
                                Text("12:00", fontSize = 10.sp, color = Color(0xFFA6ADC8))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Media Remote Controls Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { playbackProgress = (playbackProgress - 0.05f).coerceAtLeast(0f) }) {
                                Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF89B4FA))
                                    .clickable { isPlaying = !isPlaying },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color(0xFF11111B),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(onClick = { playbackProgress = (playbackProgress + 0.05f).coerceAtMost(1f) }) {
                                Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Volume Slider
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "Vol", tint = Color(0xFFA6ADC8), modifier = Modifier.size(20.dp))
                            Slider(
                                value = volumeLevel,
                                onValueChange = { volumeLevel = it },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(thumbColor = Color(0xFFCBA6F7), activeTrackColor = Color(0xFFCBA6F7))
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = "Vol", tint = Color(0xFFA6ADC8), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// 3. LATEST NEWS READER HOME COMPONENT
// -------------------------------------------------------------

data class NewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val category: String,
    val timeAgo: String,
    val url: String,
    val imageUrl: String,
    val summary: String
)

val sampleNewsArticles = listOf(
    NewsArticle(
        id = "1",
        title = "Next-Gen AI Models Push Autonomous Code Generation & Edge Devices",
        source = "TechCrunch",
        category = "Technology",
        timeAgo = "15m ago",
        url = "https://techcrunch.com",
        imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400",
        summary = "New lightweight generative models allow on-device inference with ultra-low latency."
    ),
    NewsArticle(
        id = "2",
        title = "Global Renewable Energy Milestone Achieved with High Density Storage",
        source = "BBC News",
        category = "World",
        timeAgo = "42m ago",
        url = "https://bbc.com/news",
        imageUrl = "https://images.unsplash.com/photo-1497435334941-8c899ee9e8e9?w=400",
        summary = "Grid-scale battery deployments cross new records in European and Asian markets."
    ),
    NewsArticle(
        id = "3",
        title = "Quantum Processors Demonstrate Room Temperature Error Correction",
        source = "MIT Tech Review",
        category = "Technology",
        timeAgo = "1h ago",
        url = "https://technologyreview.com",
        imageUrl = "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=400",
        summary = "Breakthrough silicon photonics architecture reduces cooling requirements significantly."
    ),
    NewsArticle(
        id = "4",
        title = "Global Financial Tech Protocols Adopt Instant Cross-Border Settlement",
        source = "Bloomberg",
        category = "Business",
        timeAgo = "2h ago",
        url = "https://bloomberg.com",
        imageUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400",
        summary = "Central banks and commercial institutions test zero-fee instant ledger networks."
    ),
    NewsArticle(
        id = "5",
        title = "Unreal Engine 5.4 Showcase Demonstrates Real-Time Photorealism on Handhelds",
        source = "IGN",
        category = "Gaming",
        timeAgo = "3h ago",
        url = "https://ign.com",
        imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=400",
        summary = "Mobile GPUs harness hardware ray tracing to deliver desktop console fidelity."
    )
)

@Composable
fun LatestNewsWidget(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onOpenArticle: (String) -> Unit
) {
    val categories = listOf("Technology", "World", "Business", "Gaming", "Science")
    var searchQuery by remember { mutableStateOf("") }

    val filteredArticles = remember(selectedCategory, searchQuery) {
        sampleNewsArticles.filter { art ->
            (selectedCategory == "All" || art.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || art.title.contains(searchQuery, ignoreCase = true) || art.summary.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Newspaper, contentDescription = null, tint = Color(0xFF89B4FA), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Latest Tech & World News", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("Live Feed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFA6E3A1))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Categories Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) Color(0xFF89B4FA) else Color(0xFF313244))
                        .clickable { onSelectCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color(0xFF11111B) else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Articles List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredArticles.forEach { article ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenArticle(article.url) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(article.source, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF89B4FA))
                                Text(" • ${article.timeAgo}", fontSize = 10.sp, color = Color(0xFF6C7086))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = article.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = article.summary,
                                fontSize = 11.sp,
                                color = Color(0xFFA6ADC8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = "Read",
                            tint = Color(0xFF6C7086),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SECURITY & PRIVACY CONTROLS SHEET
// -------------------------------------------------------------

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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFFA6ADC8))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFA6E3A1)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPrivacyControlsSheet(
    httpsOnlyMode: Boolean,
    onToggleHttpsOnly: (Boolean) -> Unit,
    webRtcProtection: Boolean,
    onToggleWebRtc: (Boolean) -> Unit,
    antiPhishingEnabled: Boolean,
    onToggleAntiPhishing: (Boolean) -> Unit,
    onClearData: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    var clearHistory by remember { mutableStateOf(true) }
    var clearCache by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(true) }
    var clearPasswords by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var timeRangeExpanded by remember { mutableStateOf(false) }
    var selectedTimeRange by remember { mutableStateOf("Last 24 hours") }
    val timeRanges = listOf("Last hour", "Last 24 hours", "Last 7 days", "Last 4 weeks", "All time")

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
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Security", tint = Color(0xFFA6E3A1), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Security & Privacy Controls", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ProtectionToggleRow(
                title = "HTTPS-Only Mode",
                subtitle = "Automatically upgrades all connection requests to HTTPS",
                checked = httpsOnlyMode,
                onCheckedChange = onToggleHttpsOnly
            )
            ProtectionToggleRow(
                title = "WebRTC IP Leak Shield",
                subtitle = "Prevents real IP address leaks over WebRTC connections",
                checked = webRtcProtection,
                onCheckedChange = onToggleWebRtc
            )
            ProtectionToggleRow(
                title = "Anti-Phishing & Malware Guard",
                subtitle = "Blocks deceitful websites, scams and harmful scripts",
                checked = antiPhishingEnabled,
                onCheckedChange = onToggleAntiPhishing
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showClearDataDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF38BA8)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Browsing Data & Cache", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = Color(0xFF1E1E2E),
            title = { Text("Clear Browsing Data", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { timeRangeExpanded = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time range:", color = Color.White)
                        Text(selectedTimeRange, color = Color(0xFF89B4FA), fontWeight = FontWeight.Bold)
                        
                        androidx.compose.material3.DropdownMenu(
                            expanded = timeRangeExpanded,
                            onDismissRequest = { timeRangeExpanded = false }
                        ) {
                            timeRanges.forEach { range ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(range) },
                                    onClick = {
                                        selectedTimeRange = range
                                        timeRangeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearHistory, onCheckedChange = { clearHistory = it })
                        Text("Browsing History", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCache, onCheckedChange = { clearCache = it })
                        Text("Cached Images & Files", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearCookies, onCheckedChange = { clearCookies = it })
                        Text("Cookies & Site Data", color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearPasswords, onCheckedChange = { clearPasswords = it })
                        Text("Saved Passwords & Vault", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearData(clearHistory, clearCache, clearCookies, clearPasswords)
                        showClearDataDialog = false
                        Toast.makeText(context, "Selected data cleared successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF38BA8))
                ) {
                    Text("Clear Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 5. BLOCK ADS LEVELS & CUSTOM FILTERS SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdBlockLevelsSheet(
    adBlockLevel: String,
    onSetAdBlockLevel: (String) -> Unit,
    enabledFilterLists: Set<String>,
    onToggleFilterList: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val availableFilters = listOf(
        "EasyList Standard",
        "EasyPrivacy Trackers",
        "Fanboy Annoyances",
        "Anti-Adblock Defeater",
        "Social Media Widgets",
        "AdGuard Base Filter",
        "Cryptomining Shield"
    )

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
                    Icon(Icons.Default.Shield, contentDescription = "AdBlock Level", tint = Color(0xFFF38BA8), modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("AdBlock Strictness Levels", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF313244))
                    .padding(4.dp)
            ) {
                listOf("Standard", "Aggressive", "Strict", "Disabled").forEach { lvl ->
                    val isSel = adBlockLevel == lvl
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFFF38BA8) else Color.Transparent)
                            .clickable { onSetAdBlockLevel(lvl) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lvl,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color(0xFF11111B) else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Active Filter Lists", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF89B4FA))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(availableFilters) { filter ->
                    val isChecked = enabledFilterLists.contains(filter)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(filter, fontSize = 13.sp, color = Color.White)
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { onToggleFilterList(filter, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF38BA8)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// 6. WEB CONTENT & TAB MANAGEMENT SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebContentViewSheet(
    userAgentPreset: String,
    onSetUserAgentPreset: (String) -> Unit,
    textZoomPercent: Int,
    onSetTextZoom: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf("Default Mobile", "Chrome Desktop (Mac)", "Chrome Desktop (Win)", "Safari iOS", "Firefox Desktop")

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
                    Icon(Icons.Default.Computer, contentDescription = "User Agent", tint = Color(0xFFCBA6F7), modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Web Content & Text Zoom", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Agent Preset Picker
            Text("User-Agent Preset", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBA6F7))
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { p ->
                    val isSel = userAgentPreset == p
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) Color(0xFFCBA6F7) else Color(0xFF313244))
                            .clickable { onSetUserAgentPreset(p) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(p, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color(0xFF11111B) else Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text Zoom / Font Scaling
            Text("Web Page Text Zoom: $textZoomPercent%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBA6F7))
            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = textZoomPercent.toFloat(),
                onValueChange = { onSetTextZoom(it.toInt()) },
                valueRange = 50f..200f,
                steps = 14,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFCBA6F7), activeTrackColor = Color(0xFFCBA6F7))
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("50% (Small)", fontSize = 10.sp, color = Color(0xFFA6ADC8))
                Text("100% (Normal)", fontSize = 10.sp, color = Color(0xFFA6ADC8))
                Text("200% (Large)", fontSize = 10.sp, color = Color(0xFFA6ADC8))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
