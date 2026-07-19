package com.jotape.inkshelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jotape.inkshelf.ui.theme.InkAccent
import com.jotape.inkshelf.ui.theme.InkBg2
import com.jotape.inkshelf.ui.theme.InkBorder
import com.jotape.inkshelf.ui.theme.InkShelfTheme
import com.jotape.inkshelf.ui.theme.InkSurface
import com.jotape.inkshelf.ui.theme.InkTextPrimary
import com.jotape.inkshelf.ui.theme.InkTextSecondary
import com.jotape.inkshelf.ui.theme.InkTextTertiary
import com.jotape.inkshelf.ui.theme.InkThemePreset

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1100.dp, 760.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "InkShelf",
    ) {
        var preset by remember { mutableStateOf(InkThemePreset.RED) }
        var darkTheme by remember { mutableStateOf(true) }

        InkShelfTheme(darkTheme = darkTheme, themeId = preset.id) {
            ThemeGallery(
                preset = preset,
                darkTheme = darkTheme,
                onPresetChange = { preset = it },
                onDarkThemeChange = { darkTheme = it },
            )
        }
    }
}

/**
 * Tela temporária da Fase 0: existe só para provar que o tema portado do Android (8 presets,
 * paletas claro/escuro, tipografia Bebas Neue + DM Sans) renderiza no desktop. Será substituída
 * pela navegação real na Fase 3.
 */
@Composable
private fun ThemeGallery(
    preset: InkThemePreset,
    darkTheme: Boolean,
    onPresetChange: (InkThemePreset) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("INKSHELF", style = MaterialTheme.typography.headlineLarge, color = InkTextPrimary)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tema escuro", style = MaterialTheme.typography.bodyLarge, color = InkTextSecondary)
            Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InkThemePreset.entries.forEach { entry ->
                FilterChip(
                    selected = entry == preset,
                    onClick = { onPresetChange(entry) },
                    label = { Text(entry.displayName) },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("TIPOGRAFIA", style = MaterialTheme.typography.titleSmall, color = InkTextSecondary)
                Text("headlineLarge — Bebas Neue", style = MaterialTheme.typography.headlineLarge, color = InkTextPrimary)
                Text("bodyLarge — DM Sans Regular", style = MaterialTheme.typography.bodyLarge, color = InkTextPrimary)
                Text("bodyMedium — DM Sans Medium", style = MaterialTheme.typography.bodyMedium, color = InkTextPrimary)
                Text("labelSmall — DM Sans Medium 9sp", style = MaterialTheme.typography.labelSmall, color = InkTextTertiary)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                "accent" to InkAccent,
                "surface" to InkSurface,
                "bg2" to InkBg2,
                "border" to InkBorder,
            ).forEach { (name, color) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(color = color, shape = CircleShape, modifier = Modifier.size(56.dp)) {}
                    Text(name, style = MaterialTheme.typography.labelSmall, color = InkTextSecondary)
                }
            }
        }
    }
}
