package com.example.prokject2_tracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlueDark80,
    secondary = BlueGreyDark80,
    tertiary = CyanDark80,
    background = DarkNavyBackground,
    surface = DarkNavySurface,
    surfaceVariant = DarkNavySurfaceVariant,
    onBackground = DarkNavyOnBackground,
    onSurface = DarkNavyOnBackground,
    outline = DarkNavyOutline,
    primaryContainer = DarkNavyPrimaryContainer,
    onPrimaryContainer = DarkNavyOnPrimaryContainer,
    secondaryContainer = DarkNavySecondaryContainer,
    onSecondaryContainer = DarkNavyOnSecondaryContainer,
    tertiaryContainer = DarkNavyTertiaryContainer,
    onTertiaryContainer = DarkNavyOnTertiaryContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Cyan40,
    background = LightBlueBackground,
    surface = LightBlueBackground,
    surfaceVariant = LightBlueSurfaceVariant,
    primaryContainer = LightBluePrimaryContainer,
    onPrimaryContainer = LightBlueOnPrimaryContainer,
    secondaryContainer = LightBlueSecondaryContainer,
    onSecondaryContainer = LightBlueOnSecondaryContainer,
    tertiaryContainer = LightBlueTertiaryContainer,
    onTertiaryContainer = LightBlueOnTertiaryContainer,
)

@Composable
fun Prokject2_TrackerTheme(
    // Always dark by design, regardless of the device's system light/dark setting.
    darkTheme: Boolean = true,
    // Dynamic (Material You) color would override our blue palette with the device wallpaper's
    // colors on Android 12+ — off by default so the app consistently looks dark/blue as designed.
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