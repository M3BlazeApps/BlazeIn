package mn.blazeapps.blazein.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.ui.viewmodel.TelegramViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TelegramViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Telegram Login") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (authState) {
                        is AuthState.Ready -> MaterialTheme.colorScheme.primaryContainer
                        is AuthState.Error -> MaterialTheme.colorScheme.errorContainer
                        is AuthState.NeedCode -> MaterialTheme.colorScheme.tertiaryContainer
                        is AuthState.NeedPassword -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Session Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (val state = authState) {
                            is AuthState.Ready -> "✓ Logged in as: ${state.userFirstName.ifBlank { "User" }}\nSession is active & persisted across app restarts."
                            is AuthState.NeedParameters -> "Waiting for API ID & API Hash"
                            is AuthState.NeedPhoneNumber -> "Waiting for Phone Number"
                            is AuthState.NeedCode -> "OTP Code sent to your Telegram app"
                            is AuthState.NeedPassword -> "Two-Step Verification (2FA) Password required"
                            is AuthState.LoggingOut -> "Logging out..."
                            is AuthState.Closed -> "Session closed / disconnected"
                            is AuthState.Error -> "Error: ${state.message}"
                            is AuthState.Initializing -> "Initializing Telegram client..."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Error display if any
            viewModel.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // API Credentials Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Telegram API Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create your free API credentials at my.telegram.org under 'API development tools'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = viewModel.apiIdInput,
                        onValueChange = { viewModel.apiIdInput = it },
                        label = { Text("API ID") },
                        placeholder = { Text("e.g. 1234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = authState !is AuthState.Ready
                    )

                    OutlinedTextField(
                        value = viewModel.apiHashInput,
                        onValueChange = { viewModel.apiHashInput = it },
                        label = { Text("API Hash") },
                        placeholder = { Text("e.g. 0123456789abcdef0123456789abcdef") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = authState !is AuthState.Ready
                    )

                    if (authState is AuthState.NeedParameters) {
                        Button(
                            onClick = { viewModel.submitParameters() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Initialize Client")
                        }
                    }
                }
            }

            // Phone Number Section
            if (authState !is AuthState.Ready) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Phone Number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = viewModel.phoneInput,
                            onValueChange = { viewModel.phoneInput = it },
                            label = { Text("Phone Number (with country code)") },
                            placeholder = { Text("+1234567890") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = authState is AuthState.NeedPhoneNumber || authState is AuthState.NeedParameters
                        )

                        if (authState is AuthState.NeedPhoneNumber) {
                            Button(
                                onClick = { viewModel.submitPhoneNumber() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Send Code")
                            }
                        }
                    }
                }
            }

            // OTP Code Section
            if (authState is AuthState.NeedCode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "3. Enter OTP Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A verification code has been sent to your other Telegram apps or SMS.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = viewModel.otpInput,
                            onValueChange = { viewModel.otpInput = it },
                            label = { Text("Verification Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { viewModel.submitOtp() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Verify Code")
                        }
                    }
                }
            }

            // 2FA Password Section
            if (authState is AuthState.NeedPassword) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "4. Two-Step Verification (2FA)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your account has Two-Step Verification enabled. Enter your Cloud Password.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = viewModel.passwordInput,
                            onValueChange = { viewModel.passwordInput = it },
                            label = { Text("2FA Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = { viewModel.submitPassword() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Submit Password")
                        }
                    }
                }
            }

            // Logout / Clear Session
            if (authState is AuthState.Ready) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Session Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You are logged in. The session is saved to encrypted storage and will remain active until you log out or the session expires.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out / Clear Session")
                        }
                    }
                }
            }
        }
    }
}
