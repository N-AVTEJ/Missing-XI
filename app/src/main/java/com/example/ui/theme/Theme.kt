package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = DeepBg,
    onSecondary = DeepBg,
    onTertiary = DeepBg,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceContainer = SurfaceContainerDark
)

@Composable
fun MissingXiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

