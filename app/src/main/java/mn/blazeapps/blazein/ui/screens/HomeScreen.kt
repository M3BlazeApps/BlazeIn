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
    onPlayVideo: (VideoItem) -> Unit,
    onToggleTheme: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val authState by viewModel.authState.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val isLoadingChats by viewModel.isLoadingChats.collectAsState()
    val selectedChatId by viewModel.selectedChatId.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val isLoadingVideos by viewModel.isLoadingVideos.collectAsState()
    val hasMoreVideos by viewModel.hasMoreVideos.collectAsState()

    var chatSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var videoSearchQuery by remember { mutableStateOf("") }
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

    val filteredVideos = remember(videoSearchQuery, videos) {
        if (videoSearchQuery.isBlank()) {
            videos
        } else {
            videos.filter {
                it.fileName.contains(videoSearchQuery, ignoreCase = true) ||
                it.caption.contains(videoSearchQuery, ignoreCase = true)
            }
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
                    Image(
                        painter = painterResource(id = R.drawable.ic_blazein_logo),
                        contentDescription = "BlazeIn Logo",
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(13.dp),
                                spotColor = Color(0x606C5CE7),
                                ambientColor = Color(0x606C5CE7)
                            )
                            .clip(RoundedCornerShape(13.dp))
                    )
                },
                title = {
                    Text(
                        text = "BlazeIn",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.6).sp,
                            fontSize = 21.sp
                        ),
                        color = if (isDark) TextLightPrimary else TextDarkPrimary
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
                        color = if (isDark) NativeVioletLight else NativeViolet
                    )
                },
                actions = {
                    // NativeOffice-style Rounded Capsule Action Container
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isDark) CardDark else CardWhite)
                            .border(
                                BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                                RoundedCornerShape(50)
                            )
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(50),
                                spotColor = if (isDark) Color(0x30000000) else Color(0x126C5CE7)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Theme Toggle Button (Moon / Sun)
                        if (onToggleTheme != null) {
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.NightsStay,
                                    contentDescription = "Toggle Theme",
                                    tint = if (isDark) Color(0xFFFFB74D) else TextDarkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Downloads Shortcut Button
                        IconButton(
                            onClick = onNavigateToDownloads,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads",
                                tint = if (isDark) TextLightPrimary else TextDarkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Refresh Videos Button (if chat active)
                        if (selectedChatId != null) {
                            IconButton(
                                onClick = { viewModel.refreshVideos() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Videos",
                                    tint = if (isDark) TextLightPrimary else TextDarkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Settings Button
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Settings",
                                tint = if (isDark) TextLightPrimary else TextDarkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                // If not logged in, show login prompt banner
                if (authState !is AuthState.Ready) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (isDark) Color(0x2A351515) else CardWhite,
                            borderColor = if (isDark) Color.White else BorderLight,
                            borderAlphaTop = 0.45f,
                            borderAlphaBottom = 0.12f
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0x33FF453A) else CatRoseBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = null,
                                        tint = CatRoseIcon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Not logged in to Telegram",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) TextLightPrimary else TextDarkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Enter your API credentials, phone number & OTP in Settings to stream videos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) TextLightSecondary else TextDarkSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onNavigateToSettings,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NativeViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Open Settings", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Chat Search Bar (Capsule Pill Design matching reference screens)
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
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = if (isDark) TextLightMuted else TextDarkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isDark) NativeVioletLight else NativeViolet,
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
                                            color = NativeViolet
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
                                                tint = if (isDark) TextLightSecondary else TextDarkSecondary,
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
                                            tint = if (isDark) NativeVioletLight else NativeViolet,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isDark) TextLightPrimary else TextDarkPrimary,
                                unfocusedTextColor = if (isDark) TextLightPrimary else TextDarkPrimary,
                                focusedContainerColor = if (isDark) CardDark else CardWhite,
                                unfocusedContainerColor = if (isDark) CardDark else CardWhite,
                                focusedBorderColor = NativeViolet,
                                unfocusedBorderColor = if (isDark) BorderDark else BorderLight,
                                cursorColor = NativeViolet
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = if (isDark) Color(0x30000000) else Color(0x106C5CE7)
                                )
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
                                backgroundColor = if (isDark) CardDark else CardWhite,
                                borderAlphaTop = 0.35f,
                                borderAlphaBottom = 0.12f,
                                borderColor = if (isDark) BorderDark else BorderLight,
                                shape = RoundedCornerShape(18.dp),
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
                                        color = if (isDark) TextLightSecondary else TextDarkSecondary
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
                                            color = NativeViolet,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = if (isDark) Color(0x14FFFFFF) else BorderLight,
                                    thickness = 0.5.dp
                                )

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
                                            color = if (isDark) TextLightSecondary else TextDarkSecondary
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
                                                        if (isSelected) {
                                                            if (isDark) PillBgDark else PillBgLight
                                                        } else Color.Transparent
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
                                                            when (chat.type) {
                                                                ChatType.CHANNEL -> CatSkyBg
                                                                ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> CatMintBg
                                                                ChatType.PRIVATE -> CatAmberBg
                                                                else -> CatRoseBg
                                                            }
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
                                                        tint = when (chat.type) {
                                                            ChatType.CHANNEL -> CatSkyIcon
                                                            ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> CatMintIcon
                                                            ChatType.PRIVATE -> CatAmberIcon
                                                            else -> CatRoseIcon
                                                        },
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = chat.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) NativeViolet else if (isDark) TextLightPrimary else TextDarkPrimary,
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
                                                        color = if (isDark) TextLightMuted else TextDarkMuted
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = NativeViolet,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(
                                                color = if (isDark) Color(0x0EFFFFFF) else BorderLight,
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active Chat Status Banner
                        if (!isSearchActive && selectedChat != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSearchActive = true },
                                backgroundColor = if (isDark) CardDark else CardWhite,
                                borderAlphaTop = 0.25f,
                                borderAlphaBottom = 0.08f,
                                borderColor = if (isDark) BorderDark else BorderLight,
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when (selectedChat.type) {
                                                    ChatType.CHANNEL -> CatSkyBg
                                                    ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> CatMintBg
                                                    ChatType.PRIVATE -> CatAmberBg
                                                    else -> CatRoseBg
                                                }
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
                                            tint = when (selectedChat.type) {
                                                ChatType.CHANNEL -> CatSkyIcon
                                                ChatType.SUPERGROUP, ChatType.BASIC_GROUP -> CatMintIcon
                                                ChatType.PRIVATE -> CatAmberIcon
                                                else -> CatRoseIcon
                                            },
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedChat.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) TextLightPrimary else TextDarkPrimary,
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
                                            color = if (isDark) TextLightMuted else TextDarkMuted
                                        )
                                    }
                                    // "Change" Button Capsule
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isDark) PillBgDark else PillBgLight)
                                            .border(
                                                BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = NativeViolet,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Change",
                                            color = NativeViolet,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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
                                color = NativeViolet,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Fetching videos from Telegram...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) TextLightSecondary else TextDarkSecondary
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
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (isDark) PillBgDark else PillBgLight)
                                    .border(
                                        BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                                        RoundedCornerShape(22.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = NativeViolet
                                )
                            }
                            Text(
                                text = "Select a chat or channel above to view videos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) TextLightPrimary else TextDarkPrimary
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
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (isDark) PillBgDark else PillBgLight)
                                    .border(
                                        BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                                        RoundedCornerShape(22.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (isDark) TextLightMuted else TextDarkMuted
                                )
                            }
                            Text(
                                text = "No videos found in this chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) TextLightPrimary else TextDarkPrimary
                            )
                            Text(
                                text = "Photos, audio, voice notes, and text messages are excluded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) TextLightSecondary else TextDarkSecondary
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
                            text = if (videoSearchQuery.isNotBlank()) "${filteredVideos.size} of ${videos.size} videos" else "${videos.size} videos available",
                            icon = Icons.Default.Movie
                        )

                        if (isLoadingVideos) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = NativeViolet
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Updating...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) TextLightSecondary else TextDarkSecondary
                                )
                            }
                        }
                    }

                    // Video Search Bar
                    OutlinedTextField(
                        value = videoSearchQuery,
                        onValueChange = { videoSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 8.dp),
                        placeholder = {
                            Text(
                                text = "Search ${videos.size} videos by name…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) TextLightMuted else TextDarkMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Videos",
                                tint = if (isDark) TextLightMuted else TextDarkMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (videoSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { videoSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Search",
                                        tint = if (isDark) TextLightMuted else TextDarkMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) CardDark else CardWhite,
                            unfocusedContainerColor = if (isDark) CardDark else CardWhite,
                            focusedBorderColor = NativeViolet,
                            unfocusedBorderColor = if (isDark) BorderDark else BorderLight,
                            cursorColor = NativeViolet,
                            focusedTextColor = if (isDark) TextLightPrimary else TextDarkPrimary,
                            unfocusedTextColor = if (isDark) TextLightPrimary else TextDarkPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    if (filteredVideos.isEmpty() && videoSearchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (isDark) TextLightMuted else TextDarkMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No videos matching \"$videoSearchQuery\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isDark) TextLightPrimary else TextDarkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(onClick = { videoSearchQuery = "" }) {
                                    Text("Clear search", color = NativeViolet)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredVideos, key = { it.messageId }) { video ->
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

                            if (hasMoreVideos && videoSearchQuery.isBlank()) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMoreVideos() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDark) PillBgDark else PillBgLight,
                                            contentColor = NativeViolet
                                        ),
                                        border = BorderStroke(1.dp, if (isDark) BorderDark else BorderLight),
                                        shape = RoundedCornerShape(16.dp),
                                        enabled = !isLoadingVideos
                                    ) {
                                        if (isLoadingVideos) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = NativeViolet,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Loading More Videos...", fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = NativeViolet)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Load More Videos", fontWeight = FontWeight.SemiBold, color = NativeViolet)
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
}

@Composable
fun VideoItemCard(
    video: VideoItem,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (isDark) CardDark else CardWhite,
        borderColor = if (isDark) BorderDark else BorderLight,
        borderAlphaTop = 0.22f,
        borderAlphaBottom = 0.08f,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact video icon squircle badge (minimal width so full filename gets maximum space)
            SquircleIconBox(
                size = 40.dp,
                shape = RoundedCornerShape(12.dp),
                brush = Brush.linearGradient(listOf(NativeViolet, ColorBlueVioletLight)),
                shadowColor = Color(0x336C5CE7)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details - full file name across 2-3 lines
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 19.sp,
                        fontSize = 14.sp
                    ),
                    color = if (isDark) TextLightPrimary else TextDarkPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.caption.isNotBlank() && video.caption.trim() != video.fileName.trim()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.caption,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = if (isDark) TextLightSecondary else TextDarkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatFileSize(video.fileSize),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (isDark) TextLightMuted else TextDarkMuted
                    )

                    if (video.durationSeconds > 0) {
                        Text(
                            text = "• ${formatDuration(video.durationSeconds)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = if (isDark) TextLightMuted else TextDarkMuted
                        )
                    }

                    if (video.isDownloaded) {
                        Text(
                            text = "• Offline",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = CatMintIcon,
                            fontWeight = FontWeight.Bold
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
                        color = NativeViolet,
                        trackColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F6C5CE7)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Downloading: ${(video.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = NativeViolet,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = if (isDark) Color(0x40000000) else Color(0x306C5CE7),
                            ambientColor = Color.Transparent
                        )
                        .clip(CircleShape)
                        .background(NativeViolet)
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
                                .background(CatRoseBg)
                                .border(BorderStroke(1.dp, CatRoseIcon.copy(alpha = 0.5f)), CircleShape)
                                .clickable(onClick = onCancelClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Download",
                                tint = CatRoseIcon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    video.isDownloaded -> {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CatMintBg)
                                .border(BorderStroke(1.dp, CatMintIcon.copy(alpha = 0.5f)), CircleShape)
                                .clickable(onClick = onClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Downloaded",
                                tint = CatMintIcon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isDark) PillBgDark else PillBgLight)
                                .border(BorderStroke(1.dp, if (isDark) BorderDark else BorderLight), CircleShape)
                                .clickable(onClick = onDownloadClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = if (isDark) TextLightPrimary else NativeViolet,
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
