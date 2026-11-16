package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3A57),
    onPrimaryContainer = Color(0xFFBBE6FF),
    secondary = EmeraldVerified,
    onSecondary = Color(0xFF002116),
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = Color(0xFF6EE7B7),
    tertiary = AmberModerate,
    onTertiary = Color(0xFF2B1900),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = NavyBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = NavySurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = NavyOutlineDark,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = SkyLightContainer,
    onPrimaryContainer = OnSkyLightContainer,
    secondary = EmeraldVerified,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = AmberModerate,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF92400E),
    background = IceBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = WhiteSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = IceSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateOutlineLight,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = Color(0xFF991B1B)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TeleProxyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
