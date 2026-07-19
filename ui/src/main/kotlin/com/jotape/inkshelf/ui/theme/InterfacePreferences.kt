package com.jotape.inkshelf.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlin.math.roundToInt

enum class InkInterfaceDensity(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val screenHorizontalPadding: Dp,
    val sectionSpacing: Dp,
    val settingsRowVerticalPadding: Dp,
    val cardTextSpacing: Dp,
) {
    COMPACT(
        id = "compact",
        displayName = "Compacta",
        shortLabel = "Comp",
        screenHorizontalPadding = 12.dp,
        sectionSpacing = 16.dp,
        settingsRowVerticalPadding = 10.dp,
        cardTextSpacing = 4.dp,
    ),
    NORMAL(
        id = "normal",
        displayName = "Normal",
        shortLabel = "Normal",
        screenHorizontalPadding = 14.dp,
        sectionSpacing = 20.dp,
        settingsRowVerticalPadding = 12.dp,
        cardTextSpacing = 6.dp,
    ),
    COMFORTABLE(
        id = "comfortable",
        displayName = "Confortável",
        shortLabel = "Conforto",
        screenHorizontalPadding = 16.dp,
        sectionSpacing = 24.dp,
        settingsRowVerticalPadding = 14.dp,
        cardTextSpacing = 8.dp,
    ),
    ;

    companion object {
        fun fromId(id: String?): InkInterfaceDensity =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

enum class InkFontScale(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val scale: Float,
) {
    SMALL(
        id = "small",
        displayName = "Pequena",
        shortLabel = "Peq",
        scale = 0.92f,
    ),
    NORMAL(
        id = "normal",
        displayName = "Normal",
        shortLabel = "Normal",
        scale = 1f,
    ),
    LARGE(
        id = "large",
        displayName = "Grande",
        shortLabel = "Grande",
        scale = 1.1f,
    ),
    ;

    companion object {
        fun fromId(id: String?): InkFontScale =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

enum class InkCardSize(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val gridInset: Dp,
    val listThumbnailWidth: Dp,
    val listItemPadding: Dp,
    val coverCornerRadius: Dp,
    val listCornerRadius: Dp,
    val stackOffsetX: Dp,
    val stackOffsetY: Dp,
    val titleMaxLines: Int,
) {
    SMALL(
        id = "small",
        displayName = "Menores",
        shortLabel = "Peq",
        gridInset = 8.dp,
        listThumbnailWidth = 64.dp,
        listItemPadding = 8.dp,
        coverCornerRadius = 3.dp,
        listCornerRadius = 10.dp,
        stackOffsetX = 3.dp,
        stackOffsetY = 5.dp,
        titleMaxLines = 2,
    ),
    MEDIUM(
        id = "medium",
        displayName = "Médios",
        shortLabel = "Médio",
        gridInset = 4.dp,
        listThumbnailWidth = 72.dp,
        listItemPadding = 10.dp,
        coverCornerRadius = 4.dp,
        listCornerRadius = 12.dp,
        stackOffsetX = 4.dp,
        stackOffsetY = 7.dp,
        titleMaxLines = 2,
    ),
    LARGE(
        id = "large",
        displayName = "Grandes",
        shortLabel = "Grande",
        gridInset = 0.dp,
        listThumbnailWidth = 84.dp,
        listItemPadding = 12.dp,
        coverCornerRadius = 6.dp,
        listCornerRadius = 14.dp,
        stackOffsetX = 5.dp,
        stackOffsetY = 9.dp,
        titleMaxLines = 3,
    ),
    ;

    companion object {
        fun fromId(id: String?): InkCardSize =
            entries.firstOrNull { it.id == id } ?: LARGE
    }
}

enum class InkItemSpacing(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val gridHorizontalSpacing: Dp,
    val gridVerticalSpacing: Dp,
    val listSpacing: Dp,
    val contentVerticalPadding: Dp,
    val controlSpacing: Dp,
) {
    TIGHT(
        id = "tight",
        displayName = "Apertado",
        shortLabel = "Aperto",
        gridHorizontalSpacing = 8.dp,
        gridVerticalSpacing = 12.dp,
        listSpacing = 8.dp,
        contentVerticalPadding = 6.dp,
        controlSpacing = 4.dp,
    ),
    NORMAL(
        id = "normal",
        displayName = "Normal",
        shortLabel = "Normal",
        gridHorizontalSpacing = 10.dp,
        gridVerticalSpacing = 16.dp,
        listSpacing = 10.dp,
        contentVerticalPadding = 8.dp,
        controlSpacing = 6.dp,
    ),
    WIDE(
        id = "wide",
        displayName = "Amplo",
        shortLabel = "Amplo",
        gridHorizontalSpacing = 14.dp,
        gridVerticalSpacing = 20.dp,
        listSpacing = 14.dp,
        contentVerticalPadding = 10.dp,
        controlSpacing = 8.dp,
    ),
    ;

    companion object {
        fun fromId(id: String?): InkItemSpacing =
            entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

enum class InkHeaderStyle(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val titleScale: Float,
    val topPadding: Dp,
    val bottomPadding: Dp,
) {
    COMPACT(
        id = "compact",
        displayName = "Compacto",
        shortLabel = "Compacto",
        titleScale = 0.92f,
        topPadding = 10.dp,
        bottomPadding = 4.dp,
    ),
    EXPANDED(
        id = "expanded",
        displayName = "Expandido",
        shortLabel = "Expandido",
        titleScale = 1f,
        topPadding = 14.dp,
        bottomPadding = 6.dp,
    ),
    ;

    companion object {
        fun fromId(id: String?): InkHeaderStyle =
            entries.firstOrNull { it.id == id } ?: EXPANDED
    }
}

enum class InkCoverCorners(
    val id: String,
    val displayName: String,
    val shortLabel: String,
) {
    STRAIGHT(
        id = "straight",
        displayName = "Reta",
        shortLabel = "Reta",
    ),
    SOFT(
        id = "soft",
        displayName = "Suave",
        shortLabel = "Suave",
    ),
    ROUNDED(
        id = "rounded",
        displayName = "Arredondada",
        shortLabel = "Arred",
    ),
    ;

    fun coverRadius(baseRadius: Dp): Dp = when (this) {
        STRAIGHT -> 0.dp
        SOFT -> baseRadius
        ROUNDED -> baseRadius + 4.dp
    }

    fun thumbnailRadius(baseRadius: Dp): Dp = when (this) {
        STRAIGHT -> 0.dp
        SOFT -> baseRadius + 4.dp
        ROUNDED -> baseRadius + 8.dp
    }

    fun shelfRadius(baseRadius: Dp): Dp = when (this) {
        STRAIGHT -> 0.dp
        SOFT -> baseRadius + 3.dp
        ROUNDED -> baseRadius + 6.dp
    }

    companion object {
        fun fromId(id: String?): InkCoverCorners =
            entries.firstOrNull { it.id == id } ?: SOFT
    }
}

object InkCoverCornerCustomization {
    const val MIN_PROGRESS = 0f
    const val MAX_PROGRESS = 1f
    const val DEFAULT_PROGRESS = 0.5f

    private val MAX_COVER_RADIUS = 18.dp
    private val MAX_THUMBNAIL_RADIUS = 22.dp
    private val MAX_SHELF_RADIUS = 20.dp

    fun normalize(value: Float): Float = value.coerceIn(MIN_PROGRESS, MAX_PROGRESS)

    fun presetPreviewProgress(corners: InkCoverCorners): Float = when (corners) {
        InkCoverCorners.STRAIGHT -> 0f
        InkCoverCorners.SOFT -> 0.5f
        InkCoverCorners.ROUNDED -> 1f
    }

    fun coverRadius(
        baseRadius: Dp,
        corners: InkCoverCorners,
        customEnabled: Boolean,
        customProgress: Float,
    ): Dp = if (customEnabled) {
        lerp(0.dp, MAX_COVER_RADIUS, normalize(customProgress))
    } else {
        corners.coverRadius(baseRadius)
    }

    fun thumbnailRadius(
        baseRadius: Dp,
        corners: InkCoverCorners,
        customEnabled: Boolean,
        customProgress: Float,
    ): Dp = if (customEnabled) {
        lerp(0.dp, MAX_THUMBNAIL_RADIUS, normalize(customProgress))
    } else {
        corners.thumbnailRadius(baseRadius)
    }

    fun shelfRadius(
        baseRadius: Dp,
        corners: InkCoverCorners,
        customEnabled: Boolean,
        customProgress: Float,
    ): Dp = if (customEnabled) {
        lerp(0.dp, MAX_SHELF_RADIUS, normalize(customProgress))
    } else {
        corners.shelfRadius(baseRadius)
    }
}

enum class InkAnimationMode(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    private val durationScale: Float,
    val allowsDecorativeMotion: Boolean,
) {
    FLUID(
        id = "fluid",
        displayName = "Fluidas",
        shortLabel = "Fluidas",
        durationScale = 1f,
        allowsDecorativeMotion = true,
    ),
    REDUCED(
        id = "reduced",
        displayName = "Reduzidas",
        shortLabel = "Reduz",
        durationScale = 0.45f,
        allowsDecorativeMotion = false,
    ),
    OFF(
        id = "off",
        displayName = "0",
        shortLabel = "0",
        durationScale = 0f,
        allowsDecorativeMotion = false,
    ),
    ;

    val allowsTransitions: Boolean
        get() = durationScale > 0f

    fun durationMillis(baseDuration: Int): Int =
        if (!allowsTransitions) 0 else (baseDuration * durationScale).roundToInt().coerceAtLeast(1)

    companion object {
        fun fromId(id: String?): InkAnimationMode =
            entries.firstOrNull { it.id == id } ?: FLUID
    }
}

/**
 * Estilo da transição entre telas do app. Aplicado de forma unificada no
 * NavHost (telas de topo) e na navegação interna das Configurações. A duração
 * e o "ligar/desligar" continuam governados por [InkAnimationMode]; este enum
 * define apenas o *tipo* de movimento. Ver `InkScreenTransitions.kt`.
 */
enum class InkTransitionStyle(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String,
) {
    FADE(
        id = "fade",
        displayName = "Esmaecer",
        shortLabel = "Fade",
        description = "Crossfade simples, sem movimento nem direção (padrão)",
    ),
    SHARED_AXIS(
        id = "shared_axis",
        displayName = "Eixo horizontal",
        shortLabel = "Eixo H",
        description = "Desliza suavemente na horizontal com fade",
    ),
    SLIDE(
        id = "slide",
        displayName = "Deslizar",
        shortLabel = "Deslizar",
        description = "A tela inteira desliza para o lado (estilo push/pop)",
    ),
    VERTICAL(
        id = "vertical",
        displayName = "Eixo vertical",
        shortLabel = "Eixo V",
        description = "Desliza suavemente na vertical com fade",
    ),
    FADE_THROUGH(
        id = "fade_through",
        displayName = "Esmaecer + escala",
        shortLabel = "Fade+",
        description = "Fade com leve zoom de entrada (Material)",
    ),
    ZOOM(
        id = "zoom",
        displayName = "Zoom",
        shortLabel = "Zoom",
        description = "A nova tela cresce sobre a anterior com fade",
    ),
    ;

    companion object {
        fun fromId(id: String?): InkTransitionStyle =
            entries.firstOrNull { it.id == id } ?: FADE
    }
}

enum class InkFolderCardStyle(
    val id: String,
    val displayName: String,
    val description: String,
) {
    STACK(
        id = "stack",
        displayName = "Pilha",
        description = "Tiras empilhadas acima da capa principal",
    ),
    ROTATED(
        id = "rotated",
        displayName = "Rotacionado",
        description = "Cards de fundo levemente inclinados atrás da capa",
    ),
    ;

    companion object {
        fun fromId(id: String?): InkFolderCardStyle =
            entries.firstOrNull { it.id == id } ?: STACK
    }
}

enum class InkHeroCardStyle(
    val id: String,
    val displayName: String,
    val description: String,
) {
    SPINE(
        id = "spine",
        displayName = "Lombada",
        description = "Capa inclinada em destaque, saindo levemente pela borda direita",
    ),
    CLASSIC(
        id = "classic",
        displayName = "Clássico",
        description = "Texto à esquerda com miniatura da capa à direita",
    ),
    ;

    companion object {
        fun fromId(id: String?): InkHeroCardStyle =
            entries.firstOrNull { it.id == id } ?: SPINE
    }
}

enum class InkShelfTilt(
    val minDegrees: Float,
    val maxDegrees: Float,
) {
    DEFAULT(
        minDegrees = 0f,
        maxDegrees = 75f,
    ),
    ;

    companion object {
        const val DEFAULT_DEGREES = 25f

        fun normalize(value: Float): Float =
            value.coerceIn(DEFAULT.minDegrees, DEFAULT.maxDegrees)
    }
}

@Immutable
data class InkUiPreferences(
    val density: InkInterfaceDensity,
    val fontScale: InkFontScale,
    val cardSize: InkCardSize,
    val itemSpacing: InkItemSpacing,
    val headerStyle: InkHeaderStyle,
    val coverCorners: InkCoverCorners,
    val customCoverCornersProgress: Float,
    val customCoverCornersEnabled: Boolean,
    val animationMode: InkAnimationMode,
    val transitionStyle: InkTransitionStyle,
    val shelfTiltDegrees: Float,
    val adaptiveShelfTiltEnabled: Boolean,
) {
    val screenHorizontalPadding: Dp
        get() = density.screenHorizontalPadding

    val headerHorizontalPadding: Dp
        get() = density.screenHorizontalPadding + 4.dp

    /**
     * Uniform header height shared by every screen so the top bar never changes
     * size or shifts the title position when navigating between screens. The 48dp
     * base fits both a headline title and a 48dp icon button / search field.
     */
    val headerHeight: Dp
        get() = headerStyle.topPadding + 48.dp + headerStyle.bottomPadding

    val coverCornerRadius: Dp
        get() = InkCoverCornerCustomization.coverRadius(
            baseRadius = cardSize.coverCornerRadius,
            corners = coverCorners,
            customEnabled = customCoverCornersEnabled,
            customProgress = customCoverCornersProgress,
        )

    val thumbnailCornerRadius: Dp
        get() = InkCoverCornerCustomization.thumbnailRadius(
            baseRadius = cardSize.coverCornerRadius,
            corners = coverCorners,
            customEnabled = customCoverCornersEnabled,
            customProgress = customCoverCornersProgress,
        )

    val shelfCoverCornerRadius: Dp
        get() = InkCoverCornerCustomization.shelfRadius(
            baseRadius = cardSize.coverCornerRadius,
            corners = coverCorners,
            customEnabled = customCoverCornersEnabled,
            customProgress = customCoverCornersProgress,
        )
}

internal val LocalInkUiPreferences = staticCompositionLocalOf {
    InkUiPreferences(
        density = InkInterfaceDensity.NORMAL,
        fontScale = InkFontScale.NORMAL,
        cardSize = InkCardSize.LARGE,
        itemSpacing = InkItemSpacing.NORMAL,
        headerStyle = InkHeaderStyle.EXPANDED,
        coverCorners = InkCoverCorners.SOFT,
        customCoverCornersProgress = InkCoverCornerCustomization.DEFAULT_PROGRESS,
        customCoverCornersEnabled = false,
        animationMode = InkAnimationMode.FLUID,
        transitionStyle = InkTransitionStyle.FADE,
        shelfTiltDegrees = InkShelfTilt.DEFAULT_DEGREES,
        adaptiveShelfTiltEnabled = false,
    )
}

val InkUi: InkUiPreferences
    @Composable get() = LocalInkUiPreferences.current
