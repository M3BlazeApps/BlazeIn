package mn.blazeapps.blazein.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.ui.theme.*
import mn.blazeapps.blazein.ui.viewmodel.DownloadsViewModel
import mn.blazeapps.blazein.ui.viewmodel.TelegramViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TelegramViewModel,
    downloadsViewModel: DownloadsViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }

    // Mock playback preferences from Figma Make
    var notifsEnabled by remember { mutableStateOf(true) }
    var autoPlayEnabled by remember { mutableStateOf(false) }

    val figmaTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = Color(0x186366F1),
        unfocusedContainerColor = Color(0x106366F1),
        disabledContainerColor = Color(0x0AFFFFFF),
        focusedBorderColor = ColorBlueViolet,
        unfocusedBorderColor = Color(0x406366F1),
        focusedLabelColor = ColorBlueVioletSubtle,
        unfocusedLabelColor = Color(0x80A5B4FC),
        cursorColor = ColorBlueVioletLight,
        disabledTextColor = TextMuted,
        disabledBorderColor = Color(0x18FFFFFF),
        disabledLabelColor = TextMuted
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                // Header with gear badge and back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SquircleIconBox(
                            size = 44.dp,
                            shape = RoundedCornerShape(14.dp),
                            brush = Brush.linearGradient(listOf(ColorPurple, ColorOrange)),
                            shadowColor = Color(0x668B5CF6)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    fontSize = 20.sp
                                ),
                                color = Color.White
                            )
                            Text(
                                text = "BlazeIn Configuration",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = ColorOrange
                            )
                        }
                    }

                    GlassIconButton(
                        onClick = onNavigateBack,
                        size = 36.dp,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = GlassBg,
                        borderAlphaTop = 0.25f
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(17.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Session Status Pills (matching Figma Make)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (authState is AuthState.Ready) {
                        val userName = (authState as AuthState.Ready).userFirstName.ifBlank { "User" }
                        PillBadge(
                            text = "Logged in · $userName",
                            icon = Icons.Default.Check,
                            backgroundColor = ColorGreenDim,
                            borderColor = Color(0x5922C55E),
                            contentColor = ColorGreenLight
                        )
                    } else {
                        PillBadge(
                            text = "Not logged in",
                            icon = Icons.Default.Lock,
                            backgroundColor = ColorRedDim,
                            borderColor = Color(0x59EF4444),
                            contentColor = ColorRedLight
                        )
                    }

                    PillBadge(
                        text = "Encrypted locally",
                        backgroundColor = ColorBlueVioletDim,
                        borderColor = Color(0x596366F1),
                        contentColor = ColorBlueVioletSubtle
                    )
                }
            }
        }
    ) { padding ->
        GlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Session Status Card (violet-tinted glass)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0x2E6366F1), Color(0x1F8B5CF6))
                            )
                        )
                        .border(BorderStroke(1.dp, Color(0x456366F1)), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp), spotColor = Color(0x6622C55E))
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when (authState) {
                                        is AuthState.Ready -> Brush.linearGradient(listOf(ColorGreen, Color(0xFF16A34A)))
                                        is AuthState.Error -> Brush.linearGradient(listOf(ColorRed, Color(0xFFB91C1C)))
                                        is AuthState.NeedCode -> Brush.linearGradient(listOf(ColorOrange, ColorOrangeLight))
                                        is AuthState.NeedPassword -> Brush.linearGradient(listOf(ColorPurple, ColorBlueViolet))
                                        else -> Brush.linearGradient(listOf(ColorBlueViolet, ColorBlueVioletLight))
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (authState) {
                                    is AuthState.Ready -> Icons.Default.Check
                                    is AuthState.Error -> Icons.Default.ErrorOutline
                                    is AuthState.NeedCode -> Icons.Default.Sms
                                    is AuthState.NeedPassword -> Icons.Default.Lock
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Session Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (val state = authState) {
                                    is AuthState.Ready -> "✓ Logged in as: ${state.userFirstName.ifBlank { "User" }}"
                                    is AuthState.NeedParameters -> "Waiting for API credentials"
                                    is AuthState.NeedPhoneNumber -> "Waiting for Phone Number"
                                    is AuthState.NeedCode -> "OTP Code sent to your Telegram app"
                                    is AuthState.NeedPassword -> "2FA Password required"
                                    is AuthState.LoggingOut -> "Logging out..."
                                    is AuthState.Closed -> "Session disconnected"
                                    is AuthState.Error -> "Error: ${state.message}"
                                    is AuthState.Initializing -> "Initializing client..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (authState is AuthState.Ready) ColorGreenLight else ColorOrangeLight
                            )
                            Text(
                                text = "Active & encrypted locally.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        // Glowing live indicator dot
                        if (authState is AuthState.Ready) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0xFF4ADE80))
                                    .clip(CircleShape)
                                    .background(ColorGreen)
                            )
                        }
                    }
                }

                // Error message banner if any
                viewModel.errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColorRedDim)
                            .border(BorderStroke(1.dp, Color(0x66EF4444)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = ColorRedLight)
                            Text(text = error, color = ColorRedLight, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 2. Telegram API Credentials Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = GlassBg,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SquircleIconBox(
                            size = 36.dp,
                            shape = RoundedCornerShape(10.dp),
                            brush = Brush.linearGradient(listOf(ColorBlueViolet, ColorBlueVioletLight)),
                            shadowColor = Color(0x666366F1)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Telegram API Credentials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "my.telegram.org → API development tools",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = viewModel.apiIdInput,
                        onValueChange = { viewModel.apiIdInput = it },
                        label = { Text("API ID") },
                        placeholder = { Text("e.g. 1234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = figmaTextFieldColors,
                        enabled = authState !is AuthState.Ready
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.apiHashInput,
                        onValueChange = { viewModel.apiHashInput = it },
                        label = { Text("API Hash") },
                        placeholder = { Text("e.g. 0123456789abcdef0123456789abcdef") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = figmaTextFieldColors,
                        enabled = authState !is AuthState.Ready
                    )

                    if (authState is AuthState.NeedParameters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.submitParameters() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorBlueViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Initialize Client", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 3. Phone Number Section
                if (authState !is AuthState.Ready) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = GlassBg,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = "Phone Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter your mobile phone number with country code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.phoneInput,
                            onValueChange = { viewModel.phoneInput = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1234567890") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = figmaTextFieldColors,
                            enabled = authState is AuthState.NeedPhoneNumber || authState is AuthState.NeedParameters
                        )

                        if (authState is AuthState.NeedPhoneNumber) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.submitPhoneNumber() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorBlueViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Send Code", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 4. OTP Code Section
                if (authState is AuthState.NeedCode) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = GlassBg,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = "Enter Verification Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A verification code has been sent to your other Telegram apps or SMS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.otpInput,
                            onValueChange = { viewModel.otpInput = it },
                            label = { Text("Verification Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = figmaTextFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.submitOtp() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Verify Code", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 5. 2FA Password Section
                if (authState is AuthState.NeedPassword) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = GlassBg,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = "Two-Step Verification (2FA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your account has Two-Step Verification enabled. Enter your Cloud Password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.passwordInput,
                            onValueChange = { viewModel.passwordInput = it },
                            label = { Text("2FA Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = figmaTextFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.submitPassword() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Submit Password", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 6. Playback Preferences Card (Figma Make)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = GlassBg,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SquircleIconBox(
                            size = 36.dp,
                            shape = RoundedCornerShape(10.dp),
                            brush = Brush.linearGradient(listOf(ColorOrange, ColorOrangeLight)),
                            shadowColor = Color(0x66F97316)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Playback Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notifications Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
                            Text("Download & stream alerts", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = notifsEnabled,
                            onCheckedChange = { notifsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorBlueViolet,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0x1FFFFFFF)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0x12FFFFFF), thickness = 0.5.dp)

                    // Auto-play Next Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Auto-play Next", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color.White)
                            Text("Continue to next video", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Switch(
                            checked = autoPlayEnabled,
                            onCheckedChange = { autoPlayEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorOrange,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0x1FFFFFFF)
                            )
                        )
                    }
                }

                // 7. Downloads Storage Card (Figma Make)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = GlassBg,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SquircleIconBox(
                            size = 36.dp,
                            shape = RoundedCornerShape(10.dp),
                            brush = Brush.linearGradient(listOf(ColorPurple, ColorBlueViolet)),
                            shadowColor = Color(0x668B5CF6)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloads Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Clear downloaded videos to free device space.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        // Clear button matching Figma Make
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorOrangeDim)
                                .border(BorderStroke(1.dp, Color(0x66F97316)), RoundedCornerShape(10.dp))
                                .clickable { showDeleteAllConfirmation = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Clear",
                                color = ColorOrangeLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 8. Session Management / Danger Card (Figma Make)
                if (authState is AuthState.Ready) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0x24F97316), Color(0x12FB923C))
                                )
                            )
                            .border(BorderStroke(1.dp, Color(0x45F97316)), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ColorOrangeDim)
                                        .border(BorderStroke(1.dp, Color(0x66F97316)), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ColorOrangeLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Session Management",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Encrypted · stays active until logout",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.logout() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorRedDim,
                                    contentColor = ColorRedLight
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, Color(0x66EF4444)), RoundedCornerShape(14.dp))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = ColorRedLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Log Out / Clear Session", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 9. About BlazeIn Card (Figma Make)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = GlassBg,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SquircleIconBox(
                            size = 44.dp,
                            shape = RoundedCornerShape(14.dp),
                            brush = Brush.linearGradient(listOf(ColorBlueViolet, ColorOrange)),
                            shadowColor = Color(0x556366F1)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BlazeIn v2.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Telegram Video Streaming",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        PillBadge(
                            text = "v2.0.0",
                            backgroundColor = ColorBlueVioletDim,
                            borderColor = Color(0x596366F1),
                            contentColor = ColorBlueVioletSubtle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation Dialog to Delete All Downloads
    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            containerColor = Color(0xF2080B1A),
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(BorderStroke(1.dp, Color(0x336366F1)), RoundedCornerShape(20.dp)),
            title = {
                Text(
                    text = "Delete All Downloads?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently remove all downloaded videos from your device storage. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        downloadsViewModel.deleteAllVideos { count ->
                            Toast.makeText(
                                context,
                                if (count > 0) "Deleted $count downloaded file(s)" else "No downloaded files found to delete",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDeleteAllConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllConfirmation = false }
                ) {
                    Text("Cancel", color = ColorBlueVioletSubtle, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}
