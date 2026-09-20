package mn.blazeapps.blazein.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    onNavigateToDownloads: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val isLoadingChats by viewModel.isLoadingChats.collectAsState()
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()
    val hasMoreVideos by viewModel.hasMoreVideos.collectAsState()

    var chatSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedChat = chats.find { it.id == selectedChatId }

    val filteredChats = remember(chatSearchQuery, chats) {
        if (chatSearchQuery.isBlank()) {
            chats
        } else {
            chats.filter { it.title.contains(chatSearchQuery, ignoreCase = true) }
        }
    }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        focusManager.clearFocus()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(
                leadingBrandIcon = {
                    SquircleIconBox(
                        size = 44.dp,
                        shape = RoundedCornerShape(14.dp),
                        brush = Brush.linearGradient(
                            listOf(ColorBlueViolet, ColorOrange)
                        ),
                        shadowColor = Color(0x666366F1)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "BlazeIn Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "BlazeIn",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            fontSize = 20.sp
                        ),
                        color = Color.White
                    )
                },
                subtitle = {
                    Text(
                        text = "telegram video streaming",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = ColorBlueVioletLight
                    )
                },
                actions = {
                    GlassIconButton(
                        onClick = onNavigateToDownloads,
                        size = 36.dp,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = GlassBg,
                        borderAlphaTop = 0.24f
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Downloads",
                            modifier = Modifier.size(17.dp),
                            tint = Color.White
                        )
                    }
                    if (selectedChatId != null) {
                        GlassIconButton(
                            onClick = { viewModel.refreshVideos() },
                            size = 36.dp,
                            shape = RoundedCornerShape(12.dp),
                            containerColor = GlassBg,
                            borderAlphaTop = 0.24f
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Videos",
                                modifier = Modifier.size(17.dp),
                                tint = Color.White
                            )
                        }
                    }
                    GlassIconButton(
                        onClick = onNavigateToSettings,
                        size = 36.dp,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = GlassBg,
                        borderAlphaTop = 0.24f
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(17.dp),
                            tint = Color.White
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
                    // Figma Make Auto-complete Chat Search Bar & Suggestions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = chatSearchQuery,
                            onValueChange = {
                                chatSearchQuery = it
                                isSearchActive = true
                            },
                            placeholder = {
                                Text(
                                    text = if (isLoadingChats) {
                                        if (chats.isEmpty()) "Syncing chats..." else "Syncing chats (${chats.size} loaded)..."
                                    } else if (chats.isNotEmpty()) {
                                        "Search ${chats.size} chats & channels…"
                                    } else {
                                        "Search chats & channels…"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = ColorBlueVioletLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (isLoadingChats) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = ColorBlueVioletLight
                                        )
                                    }
                                    if (chatSearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { chatSearchQuery = "" },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear Search",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            isSearchActive = !isSearchActive
                                            if (!isSearchActive) {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSearchActive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isSearchActive) "Collapse Suggestions" else "Expand Suggestions",
                                            tint = ColorBlueVioletLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = InputBg,
                                unfocusedContainerColor = InputBg,
                                focusedBorderColor = ColorBlueViolet,
                                unfocusedBorderColor = InputBorder,
                                cursorColor = ColorBlueVioletLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        isSearchActive = true
                                    }
                                }
                        )

                        // Auto-complete Suggestions Card
                        if (isSearchActive) {
                            Spacer(modifier = Modifier.height(8.dp))
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp),
                                backgroundColor = Color(0xF20B0F22),
                                borderAlphaTop = 0.35f,
                                borderAlphaBottom = 0.12f,
                                borderColor = ColorBlueViolet,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (chatSearchQuery.isBlank()) {
                                            "All Chats (${filteredChats.size})"
                                        } else {
                                            "Matches (${filteredChats.size})"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    TextButton(
                                        onClick = {
                                            isSearchActive = false
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Done",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ColorBlueVioletLight,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0x14FFFFFF), thickness = 0.5.dp)

                                if (filteredChats.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp, horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isLoadingChats && chats.isEmpty()) {
                                                "Loading Telegram chats..."
                                            } else {
                                                "No chats found matching \"$chatSearchQuery\""
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(filteredChats, key = { it.id }) { chat ->
                                            val isSelected = chat.id == selectedChatId
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.selectChat(chat.id)
                                                        isSearchActive = false
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                    .background(
                                                        if (isSelected) ColorBlueVioletDim else Color.Transparent
                                                    )
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) Brush.linearGradient(listOf(ColorBlueViolet, ColorBlueVioletLight)) else Brush.linearGradient(listOf(Color(0x20FFFFFF), Color(0x10FFFFFF)))
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (chat.type) {
                                                            ChatType.CHANNEL -> Icons.Default.Campaign
                                                            ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> Icons.Default.Groups
                                                            ChatType.PRIVATE -> Icons.Default.Person
                                                            else -> Icons.AutoMirrored.Filled.Chat
                                                        },
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else TextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = chat.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) ColorBlueVioletSubtle else TextPrimary,
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
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMuted
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = ColorBlueVioletLight,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = Color(0x0EFFFFFF), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }

                        // Active Chat Status Banner (Figma Make Channel Selector)
                        if (!isSearchActive && selectedChat != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSearchActive = true },
                                backgroundColor = GlassBg,
                                borderAlphaTop = 0.25f,
                                borderAlphaBottom = 0.08f,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    listOf(ColorBlueViolet, ColorBlueVioletLight)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (selectedChat.type) {
                                                ChatType.CHANNEL -> Icons.Default.Campaign
                                                ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> Icons.Default.Groups
                                                ChatType.PRIVATE -> Icons.Default.Person
                                                else -> Icons.AutoMirrored.Filled.Chat
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedChat.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val typeLabel = when (selectedChat.type) {
                                            ChatType.CHANNEL -> "Channel"
                                            ChatType.SUPERGROUP -> "Supergroup"
                                            ChatType.BASIC_GROUP -> "Group"
                                            ChatType.PRIVATE -> "Private Chat"
                                            ChatType.UNKNOWN -> "Chat"
                                        }
                                        val countLabel = if (videos.isNotEmpty()) " · ${videos.size} videos" else ""
                                        Text(
                                            text = "$typeLabel$countLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    // Figma Make "Change" Button
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ColorBlueVioletDim)
                                            .border(BorderStroke(1.dp, Color(0x666366F1)), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = ColorBlueVioletSubtle,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Change",
                                            color = ColorBlueVioletSubtle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
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
                        PillBadge(
                            text = "${videos.size} videos available",
                            icon = Icons.Default.Movie
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
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassBg,
        borderAlphaTop = 0.22f,
        borderAlphaBottom = 0.08f,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Figma Make Thumbnail (88dp x 62dp with 12dp rounded corners)
            Box(
                modifier = Modifier
                    .width(88.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(0x406366F1), Color(0x26F97316))
                        )
                    )
                    .border(BorderStroke(1.dp, Color(0x18FFFFFF)), RoundedCornerShape(12.dp)),
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x806366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }

                if (video.durationSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 5.dp, bottom = 5.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationSeconds),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val title = video.caption.ifBlank { video.fileName }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatFileSize(video.fileSize),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMuted
                    )

                    if (video.isDownloaded) {
                        Text(
                            text = "• Offline",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = ColorGreenLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (video.isDownloading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { video.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ColorOrange,
                        trackColor = Color(0x33FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Downloading: ${(video.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorOrangeLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Figma Make Action Buttons (Electric Orange Play Button & Glass Download Button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Button (btn-orange)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = Color(0x66F97316),
                            ambientColor = Color(0x33F97316)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(ColorOrange, ColorOrangeLight)
                            )
                        )
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Download / Status Button
                when {
                    video.isDownloading -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ColorRedDim)
                                .border(BorderStroke(1.dp, Color(0x66EF4444)), CircleShape)
                                .clickable(onClick = onCancelClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                tint = ColorRedLight,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    video.isDownloaded -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ColorGreenDim)
                                .border(BorderStroke(1.dp, Color(0x6622C55E)), CircleShape)
                                .clickable(onClick = onClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Downloaded",
                                tint = ColorGreenLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GlassBg)
                                .border(BorderStroke(1.dp, Color(0x666366F1)), CircleShape)
                                .clickable(onClick = onDownloadClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
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

