package mn.blazeapps.blazein.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import mn.blazeapps.blazein.R
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.data.model.ChatSummary
import mn.blazeapps.blazein.data.model.ChatType
import mn.blazeapps.blazein.data.model.VideoItem
import mn.blazeapps.blazein.ui.theme.*
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
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(
                leadingBrandIcon = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x28FFFFFF))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_blazein_logo),
                            contentDescription = "BlazeIn Logo",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "BlazeIn",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AppleTextPrimary
                    )
                },
                subtitle = {
                    Text(
                        text = "telegram video streaming",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        color = AppleCyan
                    )
                },
                actions = {
                    if (selectedChatId != null) {
                        GlassIconButton(
                            onClick = { viewModel.refreshVideos() },
                            size = 40.dp
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Videos",
                                modifier = Modifier.size(20.dp),
                                tint = AppleTextPrimary
                            )
                        }
                    }
                    GlassIconButton(
                        onClick = onNavigateToSettings,
                        size = 40.dp
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(20.dp),
                            tint = AppleTextPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        GlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // If not logged in, show an Apple glassy banner prompting login
                if (authState !is AuthState.Ready) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0x2A351515),
                            borderAlphaTop = 0.45f,
                            borderAlphaBottom = 0.12f
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33FF453A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = AppleRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Not logged in to Telegram",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Enter your API credentials, phone number & OTP in Settings to stream videos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppleTextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppleBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Open Settings", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Apple Glassy Chat Selector Dropdown
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
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2E0A84FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (selectedChat?.type) {
                                            ChatType.CHANNEL -> Icons.Default.Campaign
                                            ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> Icons.Default.Groups
                                            ChatType.PRIVATE -> Icons.Default.Person
                                            else -> Icons.AutoMirrored.Filled.Chat
                                        },
                                        contentDescription = null,
                                        tint = AppleCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AppleTextPrimary,
                                unfocusedTextColor = if (selectedChat != null) AppleTextPrimary else AppleTextSecondary,
                                focusedContainerColor = Color(0x22263650),
                                unfocusedContainerColor = Color(0x1C223048),
                                focusedBorderColor = AppleCyan,
                                unfocusedBorderColor = Color(0x2EFFFFFF),
                                cursorColor = AppleCyan
                            ),
                            supportingText = if (selectedChat != null) {
                                {
                                    val typeLabel = when (selectedChat.type) {
                                        ChatType.CHANNEL -> "Channel"
                                        ChatType.SUPERGROUP -> "Supergroup"
                                        ChatType.BASIC_GROUP -> "Group"
                                        ChatType.PRIVATE -> "Private Chat"
                                        ChatType.UNKNOWN -> "Chat"
                                    }
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleCyan
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .heightIn(max = 400.dp)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.10f))
                                        )
                                    ),
                                    RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Color(0xF5141926)
                        ) {
                            if (chats.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Loading chats...", color = AppleTextSecondary) },
                                    onClick = { dropdownExpanded = false }
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
                                                contentDescription = null,
                                                tint = AppleCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        text = {
                                            Column {
                                                Text(
                                                    text = chat.title,
                                                    fontWeight = FontWeight.Medium,
                                                    color = AppleTextPrimary,
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
                                                    color = AppleTextTertiary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectChat(chat.id)
                                            dropdownExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AppleBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Fetching videos from Telegram...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleTextSecondary
                            )
                        }
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x18FFFFFF))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = AppleCyan
                                )
                            }
                            Text(
                                text = "Select a chat or channel above to view videos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = AppleTextSecondary
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
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x18FFFFFF))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = AppleTextSecondary
                                )
                            }
                            Text(
                                text = "No videos found in this chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary
                            )
                            Text(
                                text = "Photos, audio, voice notes, and text messages are excluded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleTextSecondary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassChip(
                            text = "${videos.size} videos available",
                            icon = Icons.Default.Movie,
                            backgroundColor = Color(0x28FFFFFF),
                            contentColor = AppleTextPrimary
                        )

                        if (isLoadingVideos) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = AppleCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Updating...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleTextSecondary
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                                        .padding(vertical = 10.dp)
                                        .glassEffect(
                                            shape = RoundedCornerShape(16.dp),
                                            backgroundColor = Color(0x240A84FF),
                                            borderAlphaTop = 0.40f,
                                            borderAlphaBottom = 0.12f
                                        ),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = !isLoadingVideos
                                ) {
                                    if (isLoadingVideos) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Loading More Videos...", fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Icon(Icons.Default.ExpandMore, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Load More Videos", fontWeight = FontWeight.SemiBold)
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
}

@Composable
fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color(0x1A25344E),
        borderAlphaTop = 0.35f,
        borderAlphaBottom = 0.08f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon with Glass Duration Badge
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x1F162030))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), RoundedCornerShape(14.dp)),
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
                        modifier = Modifier.size(42.dp),
                        tint = AppleCyan
                    )
                }

                if (video.durationSeconds > 0) {
                    GlassChip(
                        text = formatDuration(video.durationSeconds),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        backgroundColor = Color(0xB3000000)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val title = video.caption.ifBlank { video.fileName }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    ),
                    color = AppleTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatFileSize(video.fileSize),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleTextSecondary
                    )

                    if (video.isDownloaded) {
                        Text(
                            text = "• Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (video.isDownloading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { video.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = AppleCyan,
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Downloading: ${(video.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons in Apple Glass style
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when {
                    video.isDownloaded -> {
                        GlassIconButton(
                            onClick = onClick,
                            size = 42.dp,
                            containerColor = Color(0x350A84FF),
                            borderAlphaTop = 0.5f,
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play Offline",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    video.isDownloading -> {
                        GlassIconButton(
                            onClick = onClick,
                            size = 38.dp,
                            containerColor = Color(0x350A84FF),
                            borderAlphaTop = 0.5f,
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Stream Now",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        GlassIconButton(
                            onClick = onCancelClick,
                            size = 36.dp,
                            containerColor = Color(0x2BFF453A),
                            borderAlphaTop = 0.4f,
                            contentColor = AppleRed
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    else -> {
                        GlassIconButton(
                            onClick = onClick,
                            size = 42.dp,
                            containerColor = Color(0x350A84FF),
                            borderAlphaTop = 0.5f,
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Stream Now",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        GlassIconButton(
                            onClick = onDownloadClick,
                            size = 36.dp,
                            containerColor = Color(0x1AFFFFFF),
                            borderAlphaTop = 0.35f,
                            contentColor = AppleCyan
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = "Download for Offline",
                                modifier = Modifier.size(18.dp)
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

