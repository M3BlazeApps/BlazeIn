package mn.blazeapps.blazein.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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

    val glassTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AppleTextPrimary,
        unfocusedTextColor = AppleTextPrimary,
        focusedContainerColor = Color(0x18FFFFFF),
        unfocusedContainerColor = Color(0x0CFFFFFF),
        disabledContainerColor = Color(0x06FFFFFF),
        focusedBorderColor = AppleCyan,
        unfocusedBorderColor = Color(0x28FFFFFF),
        focusedLabelColor = AppleCyan,
        unfocusedLabelColor = AppleTextSecondary,
        cursorColor = AppleCyan,
        disabledTextColor = AppleTextTertiary,
        disabledBorderColor = Color(0x14FFFFFF),
        disabledLabelColor = AppleTextTertiary
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GlassTopAppBar(
                navigationIcon = {
                    GlassIconButton(
                        onClick = onNavigateBack,
                        size = 40.dp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = AppleTextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = "Settings & Telegram Login",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = AppleTextPrimary
                    )
                },
                subtitle = {
                    Text(
                        text = "BlazeIn Configuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleCyan
                    )
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card in Apple Glass style
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = when (authState) {
                        is AuthState.Ready -> Color(0x22123A25)
                        is AuthState.Error -> Color(0x2E421414)
                        is AuthState.NeedCode -> Color(0x2E3E2E10)
                        is AuthState.NeedPassword -> Color(0x2E2C1A3F)
                        else -> Color(0x1C25344E)
                    },
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
                                .background(
                                    when (authState) {
                                        is AuthState.Ready -> Color(0x3530D158)
                                        is AuthState.Error -> Color(0x35FF453A)
                                        is AuthState.NeedCode -> Color(0x35FF9F0A)
                                        is AuthState.NeedPassword -> Color(0x35BF5AF2)
                                        else -> Color(0x280A84FF)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (authState) {
                                    is AuthState.Ready -> Icons.Default.CheckCircle
                                    is AuthState.Error -> Icons.Default.ErrorOutline
                                    is AuthState.NeedCode -> Icons.Default.Sms
                                    is AuthState.NeedPassword -> Icons.Default.Lock
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = when (authState) {
                                    is AuthState.Ready -> AppleGreen
                                    is AuthState.Error -> AppleRed
                                    is AuthState.NeedCode -> AppleOrange
                                    is AuthState.NeedPassword -> Color(0xFFBF5AF2)
                                    else -> AppleCyan
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Session Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = when (val state = authState) {
                                    is AuthState.Ready -> "✓ Logged in as: ${state.userFirstName.ifBlank { "User" }}\nSession is active & encrypted locally."
                                    is AuthState.NeedParameters -> "Waiting for API ID & API Hash"
                                    is AuthState.NeedPhoneNumber -> "Waiting for Phone Number"
                                    is AuthState.NeedCode -> "OTP Code sent to your Telegram app"
                                    is AuthState.NeedPassword -> "Two-Step Verification (2FA) Password required"
                                    is AuthState.LoggingOut -> "Logging out..."
                                    is AuthState.Closed -> "Session closed / disconnected"
                                    is AuthState.Error -> "Error: ${state.message}"
                                    is AuthState.Initializing -> "Initializing Telegram client..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppleTextSecondary
                            )
                        }
                    }
                }

                // Error display if any
                viewModel.errorMessage?.let { error ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x2E421414),
                        borderAlphaTop = 0.45f
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = AppleRed)
                            Text(
                                text = error,
                                color = AppleRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // API Credentials Section
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x1824334C)
                ) {
                    Text(
                        text = "1. Telegram API Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create your free API credentials at my.telegram.org under 'API development tools'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = viewModel.apiIdInput,
                        onValueChange = { viewModel.apiIdInput = it },
                        label = { Text("API ID") },
                        placeholder = { Text("e.g. 1234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors,
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
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors,
                        enabled = authState !is AuthState.Ready
                    )

                    if (authState is AuthState.NeedParameters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.submitParameters() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Initialize Client", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Phone Number Section
                if (authState !is AuthState.Ready) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x1824334C)
                    ) {
                        Text(
                            text = "2. Phone Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = viewModel.phoneInput,
                            onValueChange = { viewModel.phoneInput = it },
                            label = { Text("Phone Number (with country code)") },
                            placeholder = { Text("+1234567890") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors,
                            enabled = authState is AuthState.NeedPhoneNumber || authState is AuthState.NeedParameters
                        )

                        if (authState is AuthState.NeedPhoneNumber) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.submitPhoneNumber() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppleBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Send Code", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // OTP Code Section
                if (authState is AuthState.NeedCode) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x283E2E10),
                        borderAlphaTop = 0.45f
                    ) {
                        Text(
                            text = "3. Enter OTP Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A verification code has been sent to your other Telegram apps or SMS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = viewModel.otpInput,
                            onValueChange = { viewModel.otpInput = it },
                            label = { Text("Verification Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.submitOtp() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Verify Code", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 2FA Password Section
                if (authState is AuthState.NeedPassword) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x282C1A3F),
                        borderAlphaTop = 0.45f
                    ) {
                        Text(
                            text = "4. Two-Step Verification (2FA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your account has Two-Step Verification enabled. Enter your Cloud Password.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleTextSecondary
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
                            shape = RoundedCornerShape(14.dp),
                            colors = glassTextFieldColors
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.submitPassword() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Submit Password", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Logout / Clear Session
                if (authState is AuthState.Ready) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x1824334C)
                    ) {
                        Text(
                            text = "Session Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You are logged in. The session is saved to encrypted storage and will remain active until you log out.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleTextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x28FF453A),
                                contentColor = AppleRed
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassEffect(
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = Color(0x25FF453A),
                                    borderAlphaTop = 0.40f,
                                    borderAlphaBottom = 0.12f
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AppleRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out / Clear Session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Downloads Storage Management
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0x1824334C),
                    borderAlphaTop = 0.35f,
                    borderAlphaBottom = 0.10f
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x280A84FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = AppleCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloads Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Clear all downloaded videos to free up storage on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDeleteAllConfirmation = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x28FF453A),
                            contentColor = AppleRed
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = Color(0x22FF453A),
                                borderAlphaTop = 0.40f,
                                borderAlphaBottom = 0.12f
                            )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AppleRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All Downloaded Files", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Confirmation Dialog to Delete All Downloads
    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            containerColor = Color(0xFA151A27),
            titleContentColor = AppleTextPrimary,
            textContentColor = AppleTextSecondary,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Delete All Downloads?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently remove all downloaded videos and media from your device storage. This action cannot be undone.")
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
                        containerColor = AppleRed,
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
                    Text("Cancel", color = AppleCyan, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}
