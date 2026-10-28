package mn.blazeapps.blazein

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mn.blazeapps.blazein.data.UserPreferences
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.data.model.VideoItem
import mn.blazeapps.blazein.ui.screens.DownloadsScreen
import mn.blazeapps.blazein.ui.screens.HomeScreen
import mn.blazeapps.blazein.ui.screens.SettingsScreen
import mn.blazeapps.blazein.ui.screens.VideoPlayerScreen
import mn.blazeapps.blazein.ui.theme.BlazeInTheme
import mn.blazeapps.blazein.ui.viewmodel.DownloadsViewModel
import mn.blazeapps.blazein.ui.viewmodel.TelegramViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val userPrefs = remember { UserPreferences(context) }
            var isDarkTheme by remember { mutableStateOf(userPrefs.isDarkTheme(false)) }

            BlazeInTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelegramApp(
                        onToggleTheme = {
                            isDarkTheme = !isDarkTheme
                            userPrefs.themeMode = if (isDarkTheme) "dark" else "light"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TelegramApp(
    viewModel: TelegramViewModel = viewModel(),
    downloadsViewModel: DownloadsViewModel = viewModel(),
    onToggleTheme: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToDownloads = {
                    downloadsViewModel.loadVideos()
                    navController.navigate("downloads")
                },
                onPlayVideo = { video ->
                    navController.navigate("player")
                },
                onToggleTheme = onToggleTheme
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                downloadsViewModel = downloadsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onToggleTheme = onToggleTheme
            )
        }
        
        composable("downloads") {
            DownloadsScreen(
                viewModel = downloadsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPlayVideo = { file ->
                    val pseudoId = kotlin.math.abs(file.absolutePath.hashCode()).coerceAtLeast(1)
                    viewModel.selectedVideoForPlayback = VideoItem(
                        messageId = 0,
                        chatId = 0,
                        fileId = pseudoId,
                        fileName = file.name,
                        caption = file.nameWithoutExtension,
                        durationSeconds = 0,
                        fileSize = file.length(),
                        width = 0,
                        height = 0,
                        localPath = file.absolutePath,
                        isDownloaded = true,
                        downloadProgress = 1f,
                        isDownloading = false
                    )
                    navController.navigate("player")
                },
                onToggleTheme = onToggleTheme
            )
        }

        composable("player") {
            VideoPlayerScreen(
                video = viewModel.selectedVideoForPlayback,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
