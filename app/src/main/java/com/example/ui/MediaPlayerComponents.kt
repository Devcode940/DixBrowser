package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

enum class MediaType { VIDEO, IMAGE, AUDIO }

data class SniffedMedia(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val type: MediaType,
    val title: String,
    val extension: String,
    val mimeType: String? = null
)

class MediaPlayerState(val url: String, val title: String, val isVideo: Boolean) {
    var isPlaying by mutableStateOf(true)
    var currentPosition by mutableIntStateOf(0)
    var duration by mutableIntStateOf(0)
    var playbackSpeed by mutableFloatStateOf(1.0f)
    var isMuted by mutableStateOf(false)
    var isLooping by mutableStateOf(false)
    var isPiPMode by mutableStateOf(false)
    var isFullScreen by mutableStateOf(false)
    var pipOffsetX by mutableFloatStateOf(16f)
    var pipOffsetY by mutableFloatStateOf(100f)
}

/**
 * Media Sniffer Drawer / Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSnifferSheet(
    mediaList: List<SniffedMedia>,
    onDismiss: () -> Unit,
    onPlayMedia: (SniffedMedia) -> Unit,
    onDownloadMedia: (SniffedMedia) -> Unit,
    onBatchDownload: (List<SniffedMedia>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Videos, 2: Images, 3: Audio
    val selectedItems = remember { mutableStateListOf<String>() }

    val filteredList = remember(mediaList, selectedTab) {
        when (selectedTab) {
            1 -> mediaList.filter { it.type == MediaType.VIDEO }
            2 -> mediaList.filter { it.type == MediaType.IMAGE }
            3 -> mediaList.filter { it.type == MediaType.AUDIO }
            else -> mediaList
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E1E2E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SavedSearch,
                        contentDescription = null,
                        tint = Color(0xFF89B4FA),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Media Sniffer & Downloader",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            "${mediaList.size} media resources detected on page",
                            fontSize = 12.sp,
                            color = Color(0xFFA6ADC8)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF181825),
                contentColor = Color(0xFF89B4FA),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All (${mediaList.size})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Videos (${mediaList.count { it.type == MediaType.VIDEO }})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Images (${mediaList.count { it.type == MediaType.IMAGE }})", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Audio (${mediaList.count { it.type == MediaType.AUDIO }})", fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Batch Select Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (selectedItems.size == filteredList.size) {
                            selectedItems.clear()
                        } else {
                            selectedItems.clear()
                            selectedItems.addAll(filteredList.map { it.id })
                        }
                    }
                ) {
                    Icon(
                        if (selectedItems.size == filteredList.size && filteredList.isNotEmpty()) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedItems.size == filteredList.size && filteredList.isNotEmpty()) "Deselect All" else "Select All (${filteredList.size})",
                        fontSize = 13.sp
                    )
                }

                if (selectedItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            val itemsToDownload = mediaList.filter { selectedItems.contains(it.id) }
                            onBatchDownload(itemsToDownload)
                            selectedItems.clear()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89B4FA)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Selected (${selectedItems.size})", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // List of sniffed items
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = Color(0xFFA6ADC8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No media found for this category.",
                            color = Color(0xFFA6ADC8),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val isSelected = selectedItems.contains(item.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedItems.remove(item.id) else selectedItems.add(item.id)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF313244) else Color(0xFF181825)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF89B4FA) else Color(0xFF313244)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) selectedItems.add(item.id) else selectedItems.remove(item.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF89B4FA),
                                        uncheckedColor = Color(0xFFA6ADC8)
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Media Type Badge Icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (item.type) {
                                                MediaType.VIDEO -> Color(0xFFF38BA8).copy(alpha = 0.2f)
                                                MediaType.IMAGE -> Color(0xFFA6E3A1).copy(alpha = 0.2f)
                                                MediaType.AUDIO -> Color(0xFFFAB387).copy(alpha = 0.2f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (item.type) {
                                            MediaType.VIDEO -> Icons.Default.VideoLibrary
                                            MediaType.IMAGE -> Icons.Default.Image
                                            MediaType.AUDIO -> Icons.Default.MusicNote
                                        },
                                        contentDescription = null,
                                        tint = when (item.type) {
                                            MediaType.VIDEO -> Color(0xFFF38BA8)
                                            MediaType.IMAGE -> Color(0xFFA6E3A1)
                                            MediaType.AUDIO -> Color(0xFFFAB387)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Title & Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.title.ifBlank { "Media Resource" },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${item.type.name} • .${item.extension.ifBlank { "file" }}",
                                        color = Color(0xFFA6ADC8),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        item.url,
                                        color = Color(0xFF585B70),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Action Buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (item.type == MediaType.VIDEO || item.type == MediaType.AUDIO) {
                                        IconButton(
                                            onClick = { onPlayMedia(item) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF89B4FA).copy(alpha = 0.2f), CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color(0xFF89B4FA),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDownloadMedia(item) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFA6E3A1).copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = "Download",
                                            tint = Color(0xFFA6E3A1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen / Dialog Video & Music Player
 */
@Composable
fun MediaPlayerDialog(
    playerState: MediaPlayerState,
    onDismiss: () -> Unit,
    onEnablePiP: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(showControls, playerState.isPlaying) {
        if (showControls && playerState.isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showControls = !showControls }
        ) {
            // Video / Media Player View
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(Uri.parse(playerState.url))
                        setOnPreparedListener { mp ->
                            mp.isLooping = playerState.isLooping
                            playerState.duration = mp.duration
                            if (playerState.currentPosition > 0) {
                                mp.seekTo(playerState.currentPosition)
                            }
                            if (playerState.isMuted) {
                                mp.setVolume(0f, 0f)
                            }
                            mp.start()
                            playerState.isPlaying = true
                        }
                        setOnCompletionListener {
                            playerState.isPlaying = false
                        }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                    if (playerState.isPlaying && !view.isPlaying) {
                        view.start()
                    } else if (!playerState.isPlaying && view.isPlaying) {
                        view.pause()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Periodic position sync
            LaunchedEffect(playerState.isPlaying) {
                while (playerState.isPlaying) {
                    videoViewRef?.let {
                        playerState.currentPosition = it.currentPosition
                        playerState.duration = it.duration
                    }
                    delay(500)
                }
            }

            // Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                        }

                        Text(
                            playerState.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        )

                        Row {
                            IconButton(onClick = onEnablePiP) {
                                Icon(
                                    Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Floating PiP",
                                    tint = Color(0xFF89B4FA)
                                )
                            }
                        }
                    }

                    // Center Playback Buttons
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newPos = (playerState.currentPosition - 10000).coerceAtLeast(0)
                                videoViewRef?.seekTo(newPos)
                                playerState.currentPosition = newPos
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }

                        IconButton(
                            onClick = {
                                if (playerState.isPlaying) {
                                    videoViewRef?.pause()
                                    playerState.isPlaying = false
                                } else {
                                    videoViewRef?.start()
                                    playerState.isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0xFF89B4FA), CircleShape)
                        ) {
                            Icon(
                                if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val newPos = (playerState.currentPosition + 10000).coerceAtMost(playerState.duration)
                                videoViewRef?.seekTo(newPos)
                                playerState.currentPosition = newPos
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }

                    // Bottom Bar with Progress Slider & Actions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(playerState.currentPosition),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                formatTime(playerState.duration),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = if (playerState.duration > 0) playerState.currentPosition.toFloat() else 0f,
                            onValueChange = { newValue ->
                                playerState.currentPosition = newValue.toInt()
                                videoViewRef?.seekTo(newValue.toInt())
                            },
                            valueRange = 0f..(playerState.duration.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF89B4FA),
                                activeTrackColor = Color(0xFF89B4FA),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Speed button
                            TextButton(onClick = {
                                playerState.playbackSpeed = when (playerState.playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.5f
                                    else -> 1.0f
                                }
                            }) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${playerState.playbackSpeed}x", color = Color.White, fontSize = 12.sp)
                            }

                            // Loop button
                            IconButton(onClick = { playerState.isLooping = !playerState.isLooping }) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (playerState.isLooping) Color(0xFF89B4FA) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-App Floating Picture-in-Picture (PiP) Overlay Window with Controller
 */
@Composable
fun InAppPiPOverlay(
    playerState: MediaPlayerState,
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    var showPiPControls by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(showPiPControls, playerState.isPlaying) {
        if (showPiPControls && playerState.isPlaying) {
            delay(3000)
            showPiPControls = false
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(playerState.pipOffsetX.roundToInt(), playerState.pipOffsetY.roundToInt()) }
            .size(width = 240.dp, height = 150.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    playerState.pipOffsetX += pan.x
                    playerState.pipOffsetY += pan.y
                }
            }
            .clickable { showPiPControls = !showPiPControls }
    ) {
        // Video View surface
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(Uri.parse(playerState.url))
                    setOnPreparedListener { mp ->
                        mp.isLooping = playerState.isLooping
                        if (playerState.currentPosition > 0) {
                            mp.seekTo(playerState.currentPosition)
                        }
                        mp.start()
                        playerState.isPlaying = true
                    }
                    videoViewRef = this
                }
            },
            update = { view ->
                videoViewRef = view
                if (playerState.isPlaying && !view.isPlaying) {
                    view.start()
                } else if (!playerState.isPlaying && view.isPlaying) {
                    view.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Sync position
        LaunchedEffect(playerState.isPlaying) {
            while (playerState.isPlaying) {
                videoViewRef?.let {
                    playerState.currentPosition = it.currentPosition
                }
                delay(500)
            }
        }

        // PiP Floating Overlay Controls
        AnimatedVisibility(
            visible = showPiPControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                // Top Bar in PiP
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Drag",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            playerState.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onExpand,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Expand",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Center Controller
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (playerState.currentPosition - 10000).coerceAtLeast(0)
                            videoViewRef?.seekTo(newPos)
                            playerState.currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "-10s",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (playerState.isPlaying) {
                                videoViewRef?.pause()
                                playerState.isPlaying = false
                            } else {
                                videoViewRef?.start()
                                playerState.isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF89B4FA), CircleShape)
                    ) {
                        Icon(
                            if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPos = (playerState.currentPosition + 10000).coerceAtMost(playerState.duration)
                            videoViewRef?.seekTo(newPos)
                            playerState.currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Forward10,
                            contentDescription = "+10s",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Progress Bar
                LinearProgressIndicator(
                    progress = {
                        if (playerState.duration > 0) {
                            (playerState.currentPosition.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = Color(0xFF89B4FA),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}
