package com.lessai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF006C4C),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF89F8C7),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF002114),
    secondary = androidx.compose.ui.graphics.Color(0xFF4D6357),
    tertiary = androidx.compose.ui.graphics.Color(0xFF3D6473),
    background = androidx.compose.ui.graphics.Color(0xFFF5FBF5),
    surface = androidx.compose.ui.graphics.Color(0xFFF5FBF5),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDCE5DC)
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6CDBAC),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003826),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF005138),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF89F8C7),
    secondary = androidx.compose.ui.graphics.Color(0xFFB2CCB7),
    tertiary = androidx.compose.ui.graphics.Color(0xFFA5C9DA),
    background = androidx.compose.ui.graphics.Color(0xFF0F1F17),
    surface = androidx.compose.ui.graphics.Color(0xFF0F1F17),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF36403A)
)

@Composable
fun LessAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}