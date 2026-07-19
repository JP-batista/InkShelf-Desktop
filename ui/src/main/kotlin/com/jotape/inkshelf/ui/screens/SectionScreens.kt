package com.jotape.inkshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.ui.components.FileCard
import com.jotape.inkshelf.ui.components.FolderCard
import com.jotape.inkshelf.ui.theme.LocalInkPalette
import com.jotape.inkshelf.ui.viewmodel.ContinuarLendoUiState
import com.jotape.inkshelf.ui.viewmodel.FavoritosUiState

private val MIN_CARD_WIDTH = 180.dp

@Composable
fun FavoritosScreen(
    state: FavoritosUiState,
    onOpenFolder: (FolderItem) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleFileFavorite: (FileItem) -> Unit,
    onToggleFolderFavorite: (FolderItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Column(modifier.fillMaxSize().background(palette.bg)) {
        SectionHeader("Favoritos")

        if (state.isEmpty) {
            EmptySection(
                title = "Nenhum favorito ainda",
                detail = "Segure o clique sobre um item para favoritá-lo.",
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(MIN_CARD_WIDTH),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SubHeader("Pastas") }
                items(state.folders, key = { "folder:${it.id}" }) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onOpenFolder(folder) },
                        onToggleFavorite = { onToggleFolderFavorite(folder) },
                    )
                }
            }
            if (state.files.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SubHeader("Arquivos") }
                items(state.files, key = { "file:${it.id}" }) { file ->
                    FileCard(
                        file = file,
                        onClick = { onOpenFile(file) },
                        onToggleFavorite = { onToggleFileFavorite(file) },
                    )
                }
            }
        }
    }
}

@Composable
fun ContinuarLendoScreen(
    state: ContinuarLendoUiState,
    onOpenFile: (FileItem) -> Unit,
    onToggleFileFavorite: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Column(modifier.fillMaxSize().background(palette.bg)) {
        SectionHeader("Continuar lendo")

        if (state.isEmpty) {
            EmptySection(
                title = "Nada em andamento",
                detail = "O que você começar a ler aparece aqui.",
            )
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(MIN_CARD_WIDTH),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.files, key = { it.id }) { file ->
                FileCard(
                    file = file,
                    onClick = { onOpenFile(file) },
                    onToggleFavorite = { onToggleFileFavorite(file) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val palette = LocalInkPalette.current
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = palette.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.bg2)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
private fun SubHeader(title: String) {
    val palette = LocalInkPalette.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.textTertiary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun EmptySection(title: String, detail: String) {
    val palette = LocalInkPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary)
        }
    }
}
