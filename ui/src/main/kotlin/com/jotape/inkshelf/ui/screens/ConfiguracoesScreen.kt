package com.jotape.inkshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jotape.inkshelf.ui.theme.InkThemePreset
import com.jotape.inkshelf.ui.theme.LocalInkPalette
import com.jotape.inkshelf.ui.viewmodel.ArmazenamentoUiState
import com.jotape.inkshelf.ui.viewmodel.ConfiguracoesUiState
import kotlin.math.roundToInt

@Composable
fun ConfiguracoesScreen(
    state: ConfiguracoesUiState,
    storage: ArmazenamentoUiState,
    onDarkThemeChange: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onCacheLimitChange: (Int) -> Unit,
    onAddRoot: () -> Unit,
    onRemoveRoot: (String) -> Unit,
    onClearPageCache: () -> Unit,
    onClearAllCache: () -> Unit,
    onRefreshStorage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.bg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineLarge,
            color = palette.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bg2)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )

        Section("Aparência") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Tema escuro", style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
                Switch(checked = state.darkTheme, onCheckedChange = onDarkThemeChange)
            }

            Text("Cor de destaque", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InkThemePreset.entries.forEach { preset ->
                    val swatch = if (state.darkTheme) preset.darkPalette else preset.lightPalette
                    ThemeSwatch(
                        color = swatch.accent,
                        selected = preset.id == state.themeId,
                        contentDescription = preset.displayName,
                        onClick = { onThemeChange(preset.id) },
                    )
                }
            }
        }

        Section("Biblioteca") {
            Text(
                text = "Largura dos cards: ${state.columns}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            // Herdado do mobile como "colunas"; no desktop define a largura mínima do card,
            // já que o número real de colunas acompanha a janela.
            Slider(
                value = state.columns.toFloat(),
                onValueChange = { onColumnsChange(it.roundToInt()) },
                valueRange = 2f..6f,
                steps = 3,
                modifier = Modifier.fillMaxWidth(0.6f),
            )

            Text("Pastas da biblioteca", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            if (state.rootPaths.isEmpty()) {
                Text(
                    "Nenhuma pasta adicionada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textTertiary,
                )
            } else {
                state.rootPaths.forEach { path ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.8f),
                    ) {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onRemoveRoot(path) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remover pasta",
                                tint = palette.textTertiary,
                            )
                        }
                    }
                }
            }
            Button(onClick = onAddRoot) { Text("Adicionar pasta") }
        }

        Section("Armazenamento") {
            StorageLine("Páginas em cache", storage.pageCacheBytes)
            StorageLine("Capas", storage.coverBytes)
            StorageLine("Total do app", storage.totalBytes)

            Text(
                text = "Limite do cache: ${state.cacheLimitMb} MB",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            Slider(
                value = state.cacheLimitMb.toFloat(),
                onValueChange = { onCacheLimitChange(it.roundToInt()) },
                valueRange = 256f..8192f,
                modifier = Modifier.fillMaxWidth(0.6f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClearPageCache) { Text("Limpar páginas") }
                OutlinedButton(onClick = onClearAllCache) { Text("Limpar tudo") }
                OutlinedButton(onClick = onRefreshStorage, enabled = !storage.isRefreshing) {
                    Text("Recalcular")
                }
            }
            Text(
                "Limpar o cache não afeta sua biblioteca, progresso nem favoritos.",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val palette = LocalInkPalette.current
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = palette.accent,
        )
        content()
    }
}

@Composable
private fun ThemeSwatch(
    color: Color,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        color = color,
        shape = CircleShape,
        onClick = onClick,
        modifier = Modifier.size(36.dp),
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun StorageLine(label: String, bytes: Long) {
    val palette = LocalInkPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(0.6f),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        Text(
            text = formatBytes(bytes),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.0f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
