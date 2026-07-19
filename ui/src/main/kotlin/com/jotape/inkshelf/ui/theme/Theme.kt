package com.jotape.inkshelf.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Nota de porte: a versão Android tinha aqui um InkSystemBarStyleController + OverrideInkSystemBars
// para tingir status bar e navigation bar. No desktop não existem essas barras, então a API inteira
// foi removida em vez de virar no-op. Só as duas telas de leitura a usavam.

private fun buildDarkColorScheme(palette: InkPalette) = darkColorScheme(
    primary = palette.accent,
    onPrimary = Color.White,
    primaryContainer = palette.surface,
    onPrimaryContainer = palette.textPrimary,
    secondary = palette.surface2,
    onSecondary = palette.textPrimary,
    secondaryContainer = palette.surface2,
    onSecondaryContainer = palette.textSecondary,
    background = palette.bg,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surface2,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.border,
    outlineVariant = palette.border,
    error = palette.accent,
    onError = Color.White,
)

private fun buildLightColorScheme(palette: InkPalette) = lightColorScheme(
    primary = palette.accent,
    onPrimary = Color.White,
    primaryContainer = palette.surface2,
    onPrimaryContainer = palette.textPrimary,
    secondary = palette.surface2,
    onSecondary = palette.textPrimary,
    secondaryContainer = palette.surface,
    onSecondaryContainer = palette.textSecondary,
    background = palette.bg,
    onBackground = palette.textPrimary,
    surface = palette.surface,
    onSurface = palette.textPrimary,
    surfaceVariant = palette.surface2,
    onSurfaceVariant = palette.textSecondary,
    outline = palette.border,
    outlineVariant = palette.border,
    error = palette.accent,
    onError = Color.White,
)

@Composable
fun InkShelfTheme(
    darkTheme: Boolean = true,
    themeId: String = InkThemePreset.RED.id,
    interfaceDensity: InkInterfaceDensity = InkInterfaceDensity.NORMAL,
    fontScale: InkFontScale = InkFontScale.NORMAL,
    cardSize: InkCardSize = InkCardSize.LARGE,
    itemSpacing: InkItemSpacing = InkItemSpacing.NORMAL,
    headerStyle: InkHeaderStyle = InkHeaderStyle.EXPANDED,
    coverCorners: InkCoverCorners = InkCoverCorners.SOFT,
    customCoverCornersProgress: Float = InkCoverCornerCustomization.DEFAULT_PROGRESS,
    customCoverCornersEnabled: Boolean = false,
    animationMode: InkAnimationMode = InkAnimationMode.FLUID,
    transitionStyle: InkTransitionStyle = InkTransitionStyle.FADE,
    shelfTiltDegrees: Float = InkShelfTilt.DEFAULT_DEGREES,
    adaptiveShelfTiltEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val themePreset = InkThemePreset.fromId(themeId)
    val palette = if (darkTheme) {
        themePreset.darkPalette
    } else {
        themePreset.lightPalette
    }
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(palette)
    } else {
        buildLightColorScheme(palette)
    }
    val typography = remember(fontScale, headerStyle) {
        createInkTypography(
            fontScale = fontScale,
            headerStyle = headerStyle,
        )
    }
    val uiPreferences = remember(
        interfaceDensity,
        fontScale,
        cardSize,
        itemSpacing,
        headerStyle,
        coverCorners,
        customCoverCornersProgress,
        customCoverCornersEnabled,
        animationMode,
        transitionStyle,
        shelfTiltDegrees,
        adaptiveShelfTiltEnabled,
    ) {
        InkUiPreferences(
            density = interfaceDensity,
            fontScale = fontScale,
            cardSize = cardSize,
            itemSpacing = itemSpacing,
            headerStyle = headerStyle,
            coverCorners = coverCorners,
            customCoverCornersProgress = customCoverCornersProgress,
            customCoverCornersEnabled = customCoverCornersEnabled,
            animationMode = animationMode,
            transitionStyle = transitionStyle,
            shelfTiltDegrees = shelfTiltDegrees,
            adaptiveShelfTiltEnabled = adaptiveShelfTiltEnabled,
        )
    }

    CompositionLocalProvider(
        LocalInkPalette provides palette,
        LocalInkUiPreferences provides uiPreferences,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(InkBg)
            ) {
                content()
            }
        }
    }
}
