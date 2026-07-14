package com.example.reroplero.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AppGreen,
    onPrimary = OnAppGreen,
    secondary = AppGreen,
    onSecondary = OnAppGreen,
    tertiary = AppGreen,
    onTertiary = OnAppGreen,

    background = AppBackground,
    onBackground = AppOnDark,

    surface = AppBackground,
    onSurface = AppOnDark,

    // Chips, text-field containers, dialogs and the navigation bar all read
    // their background from one of these surface roles.
    surfaceVariant = AppSurface,
    onSurfaceVariant = AppOnDarkMuted,
    surfaceContainer = AppSurface,
    surfaceContainerLow = AppSurface,
    surfaceContainerHigh = AppSurfaceHigh,
    surfaceContainerHighest = AppSurfaceHigh,

    // Selected chips / the FAB.
    secondaryContainer = AppGreen,
    onSecondaryContainer = OnAppGreen,

    outline = AppOutline,
    outlineVariant = AppOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun ReroPleroTheme(
    // ReroPlero is a dark-only app: its palette is dark whatever the system says.
    darkTheme: Boolean = true,
    // Dynamic color would let the user's wallpaper override the palette below.
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