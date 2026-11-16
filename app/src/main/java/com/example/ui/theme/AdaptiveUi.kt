package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ScreenWidthClass {
    ULTRA_COMPACT, // < 350dp (small phones / split screen)
    COMPACT,       // 350dp .. 419dp (standard phones)
    MEDIUM,        // 420dp .. 599dp (large phones / phablets)
    EXPANDED       // >= 600dp (tablets / unfolded foldables / desktop)
}

@Immutable
data class AdaptiveUiSpecs(
    val widthClass: ScreenWidthClass = ScreenWidthClass.COMPACT,
    val horizontalPadding: Dp = 14.dp,
    val verticalPadding: Dp = 10.dp,
    val cardPadding: Dp = 14.dp,
    val sectionSpacing: Dp = 12.dp,
    val itemSpacing: Dp = 8.dp,
    val contentMaxWidth: Dp = 640.dp,
    val fontScaleMultiplier: Float = 1f,
    val useStackedHeaders: Boolean = false,
    val useNavigationRail: Boolean = false
)

val LocalAdaptiveSpecs = staticCompositionLocalOf { AdaptiveUiSpecs() }

fun computeAdaptiveUiSpecs(maxWidth: Dp, systemFontScale: Float): AdaptiveUiSpecs {
    val widthClass = when {
        maxWidth < 350.dp -> ScreenWidthClass.ULTRA_COMPACT
        maxWidth < 420.dp -> ScreenWidthClass.COMPACT
        maxWidth < 600.dp -> ScreenWidthClass.MEDIUM
        else -> ScreenWidthClass.EXPANDED
    }

    val fontMultiplier = when {
        widthClass == ScreenWidthClass.ULTRA_COMPACT && systemFontScale > 1.15f -> 0.88f
        widthClass == ScreenWidthClass.ULTRA_COMPACT -> 0.92f
        systemFontScale > 1.3f -> 0.90f
        else -> 1f
    }

    return AdaptiveUiSpecs(
        widthClass = widthClass,
        horizontalPadding = when (widthClass) {
            ScreenWidthClass.ULTRA_COMPACT -> 10.dp
            ScreenWidthClass.COMPACT -> 14.dp
            ScreenWidthClass.MEDIUM -> 16.dp
            ScreenWidthClass.EXPANDED -> 24.dp
        },
        verticalPadding = when (widthClass) {
            ScreenWidthClass.ULTRA_COMPACT -> 8.dp
            ScreenWidthClass.COMPACT -> 10.dp
            ScreenWidthClass.MEDIUM -> 12.dp
            ScreenWidthClass.EXPANDED -> 16.dp
        },
        cardPadding = when (widthClass) {
            ScreenWidthClass.ULTRA_COMPACT -> 12.dp
            ScreenWidthClass.COMPACT -> 14.dp
            ScreenWidthClass.MEDIUM -> 16.dp
            ScreenWidthClass.EXPANDED -> 20.dp
        },
        sectionSpacing = when (widthClass) {
            ScreenWidthClass.ULTRA_COMPACT -> 10.dp
            ScreenWidthClass.COMPACT -> 12.dp
            ScreenWidthClass.MEDIUM -> 14.dp
            ScreenWidthClass.EXPANDED -> 16.dp
        },
        itemSpacing = when (widthClass) {
            ScreenWidthClass.ULTRA_COMPACT -> 6.dp
            ScreenWidthClass.COMPACT -> 8.dp
            ScreenWidthClass.MEDIUM -> 10.dp
            ScreenWidthClass.EXPANDED -> 12.dp
        },
        contentMaxWidth = if (widthClass == ScreenWidthClass.EXPANDED) 720.dp else 640.dp,
        fontScaleMultiplier = fontMultiplier,
        useStackedHeaders = widthClass == ScreenWidthClass.ULTRA_COMPACT ||
            (widthClass == ScreenWidthClass.COMPACT && systemFontScale > 1.2f),
        useNavigationRail = widthClass == ScreenWidthClass.EXPANDED
    )
}

fun Typography.scaled(factor: Float): Typography {
    if (factor == 1f) return this
    return Typography(
        displayLarge = displayLarge.copy(fontSize = displayLarge.fontSize * factor, lineHeight = displayLarge.lineHeight * factor),
        displayMedium = displayMedium.copy(fontSize = displayMedium.fontSize * factor, lineHeight = displayMedium.lineHeight * factor),
        displaySmall = displaySmall.copy(fontSize = displaySmall.fontSize * factor, lineHeight = displaySmall.lineHeight * factor),
        headlineLarge = headlineLarge.copy(fontSize = headlineLarge.fontSize * factor, lineHeight = headlineLarge.lineHeight * factor),
        headlineMedium = headlineMedium.copy(fontSize = headlineMedium.fontSize * factor, lineHeight = headlineMedium.lineHeight * factor),
        headlineSmall = headlineSmall.copy(fontSize = headlineSmall.fontSize * factor, lineHeight = headlineSmall.lineHeight * factor),
        titleLarge = titleLarge.copy(fontSize = titleLarge.fontSize * factor, lineHeight = titleLarge.lineHeight * factor),
        titleMedium = titleMedium.copy(fontSize = titleMedium.fontSize * factor, lineHeight = titleMedium.lineHeight * factor),
        titleSmall = titleSmall.copy(fontSize = titleSmall.fontSize * factor, lineHeight = titleSmall.lineHeight * factor),
        bodyLarge = bodyLarge.copy(fontSize = bodyLarge.fontSize * factor, lineHeight = bodyLarge.lineHeight * factor),
        bodyMedium = bodyMedium.copy(fontSize = bodyMedium.fontSize * factor, lineHeight = bodyMedium.lineHeight * factor),
        bodySmall = bodySmall.copy(fontSize = bodySmall.fontSize * factor, lineHeight = bodySmall.lineHeight * factor),
        labelLarge = labelLarge.copy(fontSize = labelLarge.fontSize * factor, lineHeight = labelLarge.lineHeight * factor),
        labelMedium = labelMedium.copy(fontSize = labelMedium.fontSize * factor, lineHeight = labelMedium.lineHeight * factor),
        labelSmall = labelSmall.copy(fontSize = labelSmall.fontSize * factor, lineHeight = labelSmall.lineHeight * factor)
    )
}
