package mn.blazeapps.blazein.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = NativeViolet,
    onPrimary = Color.White,
    primaryContainer = PillBgLight,
    onPrimaryContainer = NativeViolet,
    secondary = CatAmberIcon,
    onSecondary = Color.White,
    secondaryContainer = CatAmberBg,
    onSecondaryContainer = CatAmberIcon,
    tertiary = CatRoseIcon,
    background = PastelBgBottom,
    onBackground = TextDarkPrimary,
    surface = CardWhite,
    onSurface = TextDarkPrimary,
    surfaceVariant = PillBgLight,
    onSurfaceVariant = TextDarkSecondary,
    outline = BorderLight,
    error = ColorRed,
    errorContainer = CatRoseBg,
    onErrorContainer = ColorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = NativeVioletLight,
    onPrimary = Color.White,
    primaryContainer = PillBgDark,
    onPrimaryContainer = NativeVioletLight,
    secondary = ColorOrangeLight,
    onSecondary = Color.White,
    secondaryContainer = ColorOrangeDim,
    onSecondaryContainer = ColorOrangeLight,
    tertiary = ColorPurple,
    background = DarkBgBottom,
    onBackground = TextLightPrimary,
    surface = CardDark,
    onSurface = TextLightPrimary,
    surfaceVariant = PillBgDark,
    onSurfaceVariant = TextLightSecondary,
    outline = BorderDark,
    error = ColorRed,
    errorContainer = ColorRedDim,
    onErrorContainer = ColorRedLight
)

@Composable
fun BlazeInTheme(
    darkTheme: Boolean = false, // Defaults to Light mode matching NativeOffice
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
