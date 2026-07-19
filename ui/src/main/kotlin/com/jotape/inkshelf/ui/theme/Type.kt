package com.jotape.inkshelf.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * No Android as fontes vinham de `R.font.*`. Aqui elas são lidas do classpath
 * (`ui/src/main/resources/font/`) com a sobrecarga de `Font` do Compose Desktop, que aceita bytes.
 *
 * Isso mantém [BebasNeue]/[DmSans] como `val` de topo — e, por consequência, [createInkTypography]
 * como função comum. A alternativa (compose-resources) exigiria torná-las `@Composable` e
 * propagar isso por todas as telas.
 */
private fun loadFont(fileName: String, weight: FontWeight): androidx.compose.ui.text.font.Font {
    val bytes = checkNotNull(
        object {}.javaClass.getResourceAsStream("/font/$fileName")?.use { it.readBytes() }
    ) { "Fonte não encontrada no classpath: /font/$fileName" }
    return Font(identity = fileName, data = bytes, weight = weight, style = FontStyle.Normal)
}

val BebasNeue = FontFamily(loadFont("bebas_neue.ttf", FontWeight.Normal))
val DmSans = FontFamily(
    loadFont("dm_sans_light.ttf",    FontWeight.Light),
    loadFont("dm_sans_regular.ttf",  FontWeight.Normal),
    loadFont("dm_sans_medium.ttf",   FontWeight.Medium),
    loadFont("dm_sans_semibold.ttf", FontWeight.SemiBold),
)

val InkTypography = Typography(
    headlineLarge = TextStyle(       // Títulos app bar (INKSHELF, etc.)
        fontFamily  = BebasNeue,
        fontWeight  = FontWeight.Normal,
        fontSize    = 28.sp,
        letterSpacing = 2.sp,
    ),
    bodyLarge = TextStyle(           // Corpo, labels de menu
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
    ),
    bodyMedium = TextStyle(          // Settings rows, ctx menu
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize   = 13.sp,
    ),
    bodySmall = TextStyle(           // Títulos de cards
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
    ),
    labelMedium = TextStyle(         // Subtítulos de settings
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
    ),
    labelSmall = TextStyle(          // Nav bar labels, sub dos cards
        fontFamily = DmSans,
        fontWeight = FontWeight.Medium,
        fontSize   = 9.sp,
        letterSpacing = 0.3.sp,
    ),
    titleSmall = TextStyle(          // Seção label (uppercase)
        fontFamily    = DmSans,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 10.sp,
        letterSpacing = 1.5.sp,
    ),
)

fun createInkTypography(
    fontScale: InkFontScale = InkFontScale.NORMAL,
    headerStyle: InkHeaderStyle = InkHeaderStyle.EXPANDED,
): Typography {
    val textScale = fontScale.scale
    return InkTypography.copy(
        headlineLarge = InkTypography.headlineLarge.scaleBy(textScale * headerStyle.titleScale),
        bodyLarge = InkTypography.bodyLarge.scaleBy(textScale),
        bodyMedium = InkTypography.bodyMedium.scaleBy(textScale),
        bodySmall = InkTypography.bodySmall.scaleBy(textScale),
        labelMedium = InkTypography.labelMedium.scaleBy(textScale),
        labelSmall = InkTypography.labelSmall.scaleBy(textScale),
        titleSmall = InkTypography.titleSmall.scaleBy(textScale),
    )
}

private fun TextStyle.scaleBy(factor: Float): TextStyle = copy(
    fontSize = fontSize.scaleBy(factor),
    lineHeight = lineHeight.scaleBy(factor),
    letterSpacing = letterSpacing.scaleBy(factor),
)

private fun TextUnit.scaleBy(factor: Float): TextUnit =
    if (isSpecified) this * factor else this
