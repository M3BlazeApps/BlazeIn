package mn.blazeapps.blazein.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.data.model.ChatSummary
import mn.blazeapps.blazein.data.model.ChatType
import mn.blazeapps.blazein.data.model.VideoItem
import mn.blazeapps.blazein.ui.viewmodel.TelegramViewModel
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TelegramViewModel,
    onNavigateToSettings: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()
    val hasMoreVideos by viewModel.hasMoreVideos.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }
    val selectedChat = chats.find { it.id == selectedChatId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telegram Videos") },
                actions = {
                    if (selectedChatId != null) {
                        IconButton(onClick = { viewModel.refreshVideos() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Videos")
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // If not logged in, show a banner prompting login
            if (authState !is AuthState.Ready) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Not logged in to Telegram",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Please open Settings to enter your API credentials, phone number, and OTP to log in.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onNavigateToSettings) {
                            Text("Open Settings")
                        }
                    }
                }
                return@Scaffold
            }

            // Chat Selector Dropdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedChat?.title ?: "Select Chat / Channel / Group",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = when (selectedChat?.type) {
                                    ChatType.CHANNEL -> Icons.Default.Campaign
                                    ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> Icons.Default.Groups
                                    ChatType.PRIVATE -> Icons.Default.Person
                                    else -> Icons.AutoMirrored.Filled.Chat
                                },
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        if (chats.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Loading chats...") },
                                onClick = { }
                            )
                        } else {
                            chats.forEach { chat ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (chat.type) {
                                                ChatType.CHANNEL -> Icons.Default.Campaign
                                                ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> Icons.Default.Groups
                                                ChatType.PRIVATE -> Icons.Default.Person
                                                else -> Icons.AutoMirrored.Filled.Chat
                                            },
                                            contentDescription = null
                                        )
                                    },
                                    text = {
                                        Column {
                                            Text(
                                                text = chat.title,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val typeLabel = when (chat.type) {
                                                ChatType.CHANNEL -> "Channel"
                                                ChatType.SUPERGROUP -> "Supergroup"
                                                ChatType.BASIC_GROUP -> "Group"
                                                ChatType.PRIVATE -> "Private Chat"
                                                ChatType.UNKNOWN -> "Chat"
                                            }
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectChat(chat.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Video Content Area
            if (isLoadingVideos) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (selectedChatId == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Select a chat or channel above to view videos",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.VideocamOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No videos found in this chat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Photos, audio, voice notes, and text messages are excluded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${videos.size} videos found",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isLoadingVideos) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Loading...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(videos, key = { it.messageId }) { video ->
                        VideoItemCard(
                            video = video,
                            onClick = {
                                viewModel.selectedVideoForPlayback = video
                                onPlayVideo(video)
                            },
                            onDownloadClick = {
                                viewModel.downloadVideo(video.fileId)
                            },
                            onCancelClick = {
                                viewModel.cancelDownload(video.fileId)
                            }
                        )
                    }

                    if (hasMoreVideos) {
                        item {
                            Button(
                                onClick = { viewModel.loadMoreVideos() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                enabled = !isLoadingVideos
                            ) {
                                if (isLoadingVideos) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading More Videos...")
                                } else {
                                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load More Videos")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (video.thumbnailPath != null && File(video.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(video.thumbnailPath),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (video.durationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationSeconds),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                val title = video.caption.ifBlank { video.fileName }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatFileSize(video.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (video.isDownloading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { video.downloadProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Downloading: ${(video.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    video.isDownloaded -> {
                        IconButton(onClick = onClick) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    video.isDownloading -> {
                        IconButton(onClick = onClick) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Stream Now",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(onClick = onCancelClick) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    else -> {
                        IconButton(onClick = onClick) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Stream Now",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = onDownloadClick) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download for Offline",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Unknown size"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}
