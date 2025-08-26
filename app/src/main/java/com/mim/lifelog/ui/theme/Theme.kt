package com.mim.lifelog.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // Added for Color.White
import androidx.compose.ui.platform.LocalContext
// Import new custom colors
import com.mim.lifelog.ui.theme.AppBlack
import com.mim.lifelog.ui.theme.BrightPurple

private val DarkColorScheme = darkColorScheme(
    primary = BrightPurple,
    secondary = BrightPurple,
    background = AppBlack,
    surface = AppBlack,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
    // tertiary = Pink80 // Retain or remove if not needed
)

private val LightColorScheme = lightColorScheme(
    primary = BrightPurple,
    secondary = BrightPurple,
    background = AppBlack,
    surface = AppBlack,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
    // tertiary = Pink40 // Retain or remove if not needed

    /* Other default colors to override
    background = Color(0xFFFFFBFE), // Previous background
    surface = Color(0xFFFFFBFE), // Previous surface
    onPrimary = Color.White, // Already White, but explicitly set
    onSecondary = Color.White, // Already White, but explicitly set
    onTertiary = Color.White, // If tertiary is used, this might need adjustment
    onBackground = Color(0xFF1C1B1F), // Previous onBackground
    onSurface = Color(0xFF1C1B1F), // Previous onSurface
    */
)

@Composable
fun LifeLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
        colorScheme = colorScheme, // Corrected to use the determined colorScheme
        typography = Typography,
        content = content
    )
}