package mn.blazeapps.blazein

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mn.blazeapps.blazein.data.model.AuthState
import mn.blazeapps.blazein.ui.screens.HomeScreen
import mn.blazeapps.blazein.ui.screens.SettingsScreen
import mn.blazeapps.blazein.ui.screens.VideoPlayerScreen
import mn.blazeapps.blazein.ui.theme.BlazeInTheme
import mn.blazeapps.blazein.ui.viewmodel.TelegramViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlazeInTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelegramApp()
                }
            }
        }
    }
}

@Composable
fun TelegramApp(viewModel: TelegramViewModel = viewModel()) {
    val navController = rememberNavController()
    val authState by viewModel.authState.collectAsState()

    // Automatically navigate to home when auth succeeds, or to settings if needed
    LaunchedEffect(authState) {
        if (authState is AuthState.Ready) {
            // If on settings screen and logged in, can stay or navigate to home
        }
    }

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
                onPlayVideo = { video ->
                    navController.navigate("player")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
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