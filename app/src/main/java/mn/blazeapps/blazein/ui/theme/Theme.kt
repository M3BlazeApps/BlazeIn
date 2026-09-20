package mn.blazeapps.blazein.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ColorBlueViolet,
    onPrimary = Color.White,
    primaryContainer = ColorBlueVioletDim,
    onPrimaryContainer = ColorBlueVioletSubtle,
    secondary = ColorOrange,
    onSecondary = Color.White,
    secondaryContainer = ColorOrangeDim,
    onSecondaryContainer = ColorOrangeLight,
    tertiary = ColorPurple,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgDeep,
    onSurface = TextPrimary,
    surfaceVariant = GlassBg,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = ColorRed,
    errorContainer = ColorRedDim,
    onErrorContainer = ColorRedLight
)

private val LightColorScheme = DarkColorScheme

@Composable
fun BlazeInTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}