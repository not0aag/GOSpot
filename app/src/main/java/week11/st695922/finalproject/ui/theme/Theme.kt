package week11.st695922.finalproject.ui.theme

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
    primary = GoGreenOnDark,
    onPrimary = GoGreenInk,
    secondary = GoGreenSoftDark,
    onSecondary = GoGreenInk,
    tertiary = GoAmber,
    onTertiary = GoGreenInk,
    error = GoRedOnDark,
    onError = GoGreenInk,
    primaryContainer = GoGreenContainerDark,
    onPrimaryContainer = GoGreenContainer,
    background = SurfaceBgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    outline = DividerDark
)

private val LightColorScheme = lightColorScheme(
    primary = GoGreen,
    secondary = GoGreenDark,
    tertiary = GoAmber,
    error = GoRed,
    primaryContainer = GoGreenContainer,
    background = SurfaceBg,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Divider
)

@Composable
fun GOSpotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color defaults off so the app keeps GOSpot's green brand instead
    // of the device wallpaper palette, matching the fixed green in the mockups.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}