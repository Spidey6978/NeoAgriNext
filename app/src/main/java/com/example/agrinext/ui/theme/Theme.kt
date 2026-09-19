package com.example.agrinext.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define the Green Tones for Accents/SVGs
private val AgriDarkGreen = Color(0xFF2E7D32)
private val AgriDarkestGreen = Color(0xFF1B5E20)

// UPDATED: User-provided green for Dark Mode accents
private val AgriLightGrass = Color(0xFF69E384)

// Define Neutrals for Dark Mode (Grey Backgrounds)
private val DarkBackground = Color(0xFF121212) // Standard Dark Grey
private val DarkSurface = Color(0xFF1E1E1E)    // Slightly lighter Grey for cards/bottom bar

private val DarkColorScheme = darkColorScheme(
    primary = AgriLightGrass, // SVGs and Active Icons will use this Light Grass Green
    onPrimary = Color.Black,
    primaryContainer = AgriDarkGreen, // Containers use the darker green
    onPrimaryContainer = AgriLightGrass,
    secondary = AgriDarkestGreen,
    tertiary = AgriDarkGreen,

    background = DarkBackground, // Main screen background is Dark Grey
    onBackground = Color.White,  // FONTS ARE WHITE

    surface = DarkSurface,       // Distinct from background for BottomBar
    onSurface = Color.White,     // FONTS ARE WHITE

    surfaceVariant = DarkSurface, // Keep cards distinct from background
    onSurfaceVariant = AgriLightGrass // Unselected icons/minor details get the green tint
)

private val LightColorScheme = lightColorScheme(
    primary = AgriDarkGreen,
    secondary = AgriDarkestGreen,
    tertiary = AgriLightGrass,
    background = AgriBackground,
    surface = White,             // Distinct White surface for BottomBar
    onSurface = AgriDarkestGreen, // Very dark green for text/icons instead of pure black
    surfaceVariant = AgriWhiteGreen,
    onSurfaceVariant = AgriDarkGreen, // Unselected icons can pick this up if themed
    primaryContainer = AgriCardDark,
    secondaryContainer = AgriCardLight
)

@Composable
fun AgriNextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We default to false here to ensure the Green Theme is used instead of the user's wallpaper colors
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)

            // Set status bar color to match background
            window.statusBarColor = colorScheme.background.toArgb()

            // Set Navigation Bar Color (Matches surfaceVariant/bottom bar color)
            // This makes the system navigation area blend with your custom bottom bar
            window.navigationBarColor = colorScheme.surfaceVariant.toArgb()

            // Control Icon Colors (Light icons for Dark Mode, Dark icons for Light Mode)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}