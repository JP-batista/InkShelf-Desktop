package com.jotape.inkshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.ui.components.FileCard
import com.jotape.inkshelf.ui.components.FolderCard
import com.jotape.inkshelf.ui.theme.LocalInkPalette
import com.jotape.inkshelf.ui.viewmodel.BibliotecaUiState
import com.jotape.inkshelf.ui.viewmodel.BreadcrumbItem

/**
 * Tela principal: navega a árvore de pastas da biblioteca e abre arquivos.
 *
 * Recebe estado e callbacks em vez de alcançar o ViewModel por dentro — assim a tela é
 * renderizável isoladamente e não depende de injeção para ser exercitada.
 */
@Composable
fun BibliotecaScreen(
    state: BibliotecaUiState,
    onOpenFolder: (FolderItem) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleFileFavorite: (FileItem) -> Unit,
    onToggleFolderFavorite: (FolderItem) -> Unit,
    onBreadcrumbClick: (BreadcrumbItem) -> Unit,
    onPickLibraryFolder: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Column(modifier = modifier.fillMaxSize().background(palette.bg)) {
        LibraryTopBar(
            state = state,
            onBreadcrumbClick = onBreadcrumbClick,
            onPickLibraryFolder = onPickLibraryFolder,
            onRescan = onRescan,
        )

        when {
            state.isInitializing -> CenteredMessage { CircularProgressIndicator(color = palette.accent) }

            !state.hasRootPath -> EmptyLibraryInvite(onPickLibraryFolder = onPickLibraryFolder)

            state.isEmpty && state.isScanning ->
                CenteredMessage {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = palette.accent)
                        Text(
                            "Varrendo a biblioteca…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textSecondary,
                        )
                    }
                }

            state.isEmpty ->
                CenteredMessage {
                    Text(
                        "Nada por aqui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary,
                    )
                }

            else -> LibraryGrid(
                state = state,
                onOpenFolder = onOpenFolder,
                onOpenFile = onOpenFile,
                onToggleFileFavorite = onToggleFileFavorite,
                onToggleFolderFavorite = onToggleFolderFavorite,
            )
        }
    }
}

@Composable
private fun LibraryGrid(
    state: BibliotecaUiState,
    onOpenFolder: (FolderItem) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleFileFavorite: (FileItem) -> Unit,
    onToggleFolderFavorite: (FolderItem) -> Unit,
) {
    // Adaptive e não Fixed: a janela do desktop é redimensionável, então o número de colunas
    // acompanha a largura em vez de ficar preso à preferência do mobile (que vale como largura
    // mínima do card).
    val minCardWidth = (760 / state.columns).dp

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCardWidth),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.folders, key = { "folder:${it.id}" }) { folder ->
            FolderCard(
                folder = folder,
                onClick = { onOpenFolder(folder) },
                onToggleFavorite = { onToggleFolderFavorite(folder) },
            )
        }
        items(state.files, key = { "file:${it.id}" }) { file ->
            FileCard(
                file = file,
                onClick = { onOpenFile(file) },
                onToggleFavorite = { onToggleFileFavorite(file) },
            )
        }
    }
}

@Composable
private fun LibraryTopBar(
    state: BibliotecaUiState,
    onBreadcrumbClick: (BreadcrumbItem) -> Unit,
    onPickLibraryFolder: () -> Unit,
    onRescan: () -> Unit,
) {
    val palette = LocalInkPalette.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.bg2)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Breadcrumbs(
            items = state.breadcrumbs,
            onClick = onBreadcrumbClick,
            modifier = Modifier.weight(1f),
        )

        if (state.isScanning) {
            CircularProgressIndicator(
                color = palette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        }

        IconButton(onClick = onRescan, enabled = state.hasRootPath && !state.isScanning) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Atualizar biblioteca",
                tint = palette.textSecondary,
            )
        }
        IconButton(onClick = onPickLibraryFolder, enabled = !state.isScanning) {
            Icon(
                Icons.Default.CreateNewFolder,
                contentDescription = "Adicionar pasta",
                tint = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun Breadcrumbs(
    items: List<BreadcrumbItem>,
    onClick: (BreadcrumbItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLast) palette.textPrimary else palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable(enabled = !isLast) { onClick(item) }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            if (!isLast) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = palette.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryInvite(onPickLibraryFolder: () -> Unit) {
    val palette = LocalInkPalette.current

    CenteredMessage {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Sua biblioteca está vazia",
                style = MaterialTheme.typography.headlineLarge,
                color = palette.textPrimary,
            )
            Text(
                "Escolha a pasta onde estão seus quadrinhos e livros.",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textSecondary,
            )
            Button(onClick = onPickLibraryFolder) { Text("Escolher pasta") }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
