package mn.blazeapps.blazein.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = AppleGlassBackground,
    onPrimaryContainer = AppleTextPrimary,
    secondary = AppleCyan,
    onSecondary = Color.Black,
    secondaryContainer = AppleGlassCardBackground,
    onSecondaryContainer = AppleTextPrimary,
    tertiary = AppleIndigo,
    background = AppleCanvasDark,
    onBackground = AppleTextPrimary,
    surface = AppleCanvasDark,
    onSurface = AppleTextPrimary,
    surfaceVariant = AppleGlassCardBackground,
    onSurfaceVariant = AppleTextSecondary,
    outline = AppleGlassBorder,
    error = AppleRed,
    errorContainer = Color(0x33FF453A),
    onErrorContainer = Color(0xFFFF8E87)
)

private val LightColorScheme = DarkColorScheme

@Composable
fun BlazeInTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor = false by default to maintain the curated Apple Glassy look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}