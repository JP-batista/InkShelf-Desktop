package com.jotape.inkshelf.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class InkPalette(
    val bg: Color,
    val bg2: Color,
    val bg3: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
)

enum class InkThemePreset(
    val id: String,
    val displayName: String,
    val darkPalette: InkPalette,
    val lightPalette: InkPalette,
) {
    RED(
        id = "red",
        displayName = "Vermelho",
        darkPalette = InkPalette(
            bg = Color(0xFF0D0D0F),
            bg2 = Color(0xFF141418),
            bg3 = Color(0xFF1C1C22),
            surface = Color(0xFF1E1E26),
            surface2 = Color(0xFF26262F),
            border = Color(0x12FFFFFF),
            accent = Color(0xFFE63946),
            textPrimary = Color(0xFFF0F0F5),
            textSecondary = Color(0xFF9090A8),
            textTertiary = Color(0xFF5A5A72),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFF6F2ED),
            bg2 = Color(0xFFF0E8DF),
            bg3 = Color(0xFFFFFBF7),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE8DED2),
            border = Color(0x1A111111),
            accent = Color(0xFFD62839),
            textPrimary = Color(0xFF19191F),
            textSecondary = Color(0xFF5F5F72),
            textTertiary = Color(0xFF8A8A9D),
        ),
    ),
    BLUE(
        id = "blue",
        displayName = "Azul",
        darkPalette = InkPalette(
            bg = Color(0xFF0C1117),
            bg2 = Color(0xFF121A23),
            bg3 = Color(0xFF18222E),
            surface = Color(0xFF1C2935),
            surface2 = Color(0xFF233342),
            border = Color(0x163A86FF),
            accent = Color(0xFF3A86FF),
            textPrimary = Color(0xFFEAF3FF),
            textSecondary = Color(0xFF9DB3CC),
            textTertiary = Color(0xFF6C8097),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFF3F8FF),
            bg2 = Color(0xFFE8F0FC),
            bg3 = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFDCE8F7),
            border = Color(0x1A245A8C),
            accent = Color(0xFF2563EB),
            textPrimary = Color(0xFF172033),
            textSecondary = Color(0xFF596A87),
            textTertiary = Color(0xFF8393AD),
        ),
    ),
    GREEN(
        id = "green",
        displayName = "Verde",
        darkPalette = InkPalette(
            bg = Color(0xFF0D1411),
            bg2 = Color(0xFF131C18),
            bg3 = Color(0xFF1A2620),
            surface = Color(0xFF1D2A24),
            surface2 = Color(0xFF24332C),
            border = Color(0x162FBF71),
            accent = Color(0xFF2FBF71),
            textPrimary = Color(0xFFEAF7F0),
            textSecondary = Color(0xFF9BB9A8),
            textTertiary = Color(0xFF688170),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFF3FBF5),
            bg2 = Color(0xFFE6F3EA),
            bg3 = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFD9ECDE),
            border = Color(0x1A2D7C54),
            accent = Color(0xFF1F9D5C),
            textPrimary = Color(0xFF18261D),
            textSecondary = Color(0xFF536C5A),
            textTertiary = Color(0xFF7B9482),
        ),
    ),
    ORANGE(
        id = "orange",
        displayName = "Laranja",
        darkPalette = InkPalette(
            bg = Color(0xFF17100C),
            bg2 = Color(0xFF201613),
            bg3 = Color(0xFF2A1D17),
            surface = Color(0xFF30211B),
            surface2 = Color(0xFF3A291F),
            border = Color(0x16FF8A3D),
            accent = Color(0xFFFF8A3D),
            textPrimary = Color(0xFFFFF0E7),
            textSecondary = Color(0xFFD2AD94),
            textTertiary = Color(0xFF9A745F),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFFFF5EE),
            bg2 = Color(0xFFFDEBDE),
            bg3 = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFF6DDCC),
            border = Color(0x1AA85E2B),
            accent = Color(0xFFE76F00),
            textPrimary = Color(0xFF321B10),
            textSecondary = Color(0xFF7E5A45),
            textTertiary = Color(0xFFA07B64),
        ),
    ),
    PURPLE(
        id = "purple",
        displayName = "Roxo",
        darkPalette = InkPalette(
            bg = Color(0xFF120D17),
            bg2 = Color(0xFF19121F),
            bg3 = Color(0xFF22192A),
            surface = Color(0xFF281E31),
            surface2 = Color(0xFF32273D),
            border = Color(0x169B5DE5),
            accent = Color(0xFF9B5DE5),
            textPrimary = Color(0xFFF6EDFF),
            textSecondary = Color(0xFFB79BCF),
            textTertiary = Color(0xFF856B9A),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFFAF5FF),
            bg2 = Color(0xFFF0E5FA),
            bg3 = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE8D7F4),
            border = Color(0x1A6A4099),
            accent = Color(0xFF7C3AED),
            textPrimary = Color(0xFF271534),
            textSecondary = Color(0xFF705784),
            textTertiary = Color(0xFF967DAA),
        ),
    ),
    GRAPHITE(
        id = "graphite",
        displayName = "Grafite",
        darkPalette = InkPalette(
            bg = Color(0xFF0F1012),
            bg2 = Color(0xFF16181B),
            bg3 = Color(0xFF1E2227),
            surface = Color(0xFF20252B),
            surface2 = Color(0xFF2A3037),
            border = Color(0x169CA3AF),
            accent = Color(0xFF7D8A99),
            textPrimary = Color(0xFFF1F4F7),
            textSecondary = Color(0xFFA8B1BB),
            textTertiary = Color(0xFF707983),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFF4F5F6),
            bg2 = Color(0xFFECEEF1),
            bg3 = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE2E6EB),
            border = Color(0x1A424A53),
            accent = Color(0xFF4B5563),
            textPrimary = Color(0xFF181C20),
            textSecondary = Color(0xFF626B76),
            textTertiary = Color(0xFF8A929C),
        ),
    ),
    SEPIA(
        id = "sepia",
        displayName = "Sepia",
        darkPalette = InkPalette(
            bg = Color(0xFF17120D),
            bg2 = Color(0xFF211A13),
            bg3 = Color(0xFF2B2219),
            surface = Color(0xFF31261D),
            surface2 = Color(0xFF3B2F24),
            border = Color(0x16D0A56B),
            accent = Color(0xFFC58A48),
            textPrimary = Color(0xFFF9EEDF),
            textSecondary = Color(0xFFD1B79A),
            textTertiary = Color(0xFF9A7E61),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFFBF4E8),
            bg2 = Color(0xFFF4E8D7),
            bg3 = Color(0xFFFFFCF7),
            surface = Color(0xFFFFFDF9),
            surface2 = Color(0xFFECDCC6),
            border = Color(0x1A8E6840),
            accent = Color(0xFF9C5E1A),
            textPrimary = Color(0xFF2E2216),
            textSecondary = Color(0xFF74614B),
            textTertiary = Color(0xFF9B876F),
        ),
    ),
    MONO(
        id = "mono",
        displayName = "Preto e branco",
        darkPalette = InkPalette(
            bg = Color(0xFF0B0B0B),
            bg2 = Color(0xFF121212),
            bg3 = Color(0xFF1B1B1B),
            surface = Color(0xFF1F1F1F),
            surface2 = Color(0xFF2A2A2A),
            border = Color(0x16FFFFFF),
            accent = Color(0xFF6F6F6F),
            textPrimary = Color(0xFFF5F5F5),
            textSecondary = Color(0xFFB1B1B1),
            textTertiary = Color(0xFF797979),
        ),
        lightPalette = InkPalette(
            bg = Color(0xFFF6F6F6),
            bg2 = Color(0xFFEDEDED),
            bg3 = Color(0xFFFFFFFF),
            surface = Color(0xFFFFFFFF),
            surface2 = Color(0xFFE3E3E3),
            border = Color(0x1A111111),
            accent = Color(0xFF222222),
            textPrimary = Color(0xFF111111),
            textSecondary = Color(0xFF555555),
            textTertiary = Color(0xFF888888),
        ),
    );

    companion object {
        fun fromId(id: String?): InkThemePreset =
            entries.firstOrNull { it.id == id } ?: RED
    }
}

internal val LocalInkPalette = staticCompositionLocalOf { InkThemePreset.RED.darkPalette }

val InkBg: Color
    @Composable get() = LocalInkPalette.current.bg

val InkBg2: Color
    @Composable get() = LocalInkPalette.current.bg2

val InkBg3: Color
    @Composable get() = LocalInkPalette.current.bg3

val InkSurface: Color
    @Composable get() = LocalInkPalette.current.surface

val InkSurface2: Color
    @Composable get() = LocalInkPalette.current.surface2

val InkBorder: Color
    @Composable get() = LocalInkPalette.current.border

val InkAccent: Color
    @Composable get() = LocalInkPalette.current.accent

val InkTextPrimary: Color
    @Composable get() = LocalInkPalette.current.textPrimary

val InkTextSecondary: Color
    @Composable get() = LocalInkPalette.current.textSecondary

val InkTextTertiary: Color
    @Composable get() = LocalInkPalette.current.textTertiary
