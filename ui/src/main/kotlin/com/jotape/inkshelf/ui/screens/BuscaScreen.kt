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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.ui.components.FileCard
import com.jotape.inkshelf.ui.components.FolderCard
import com.jotape.inkshelf.ui.theme.LocalInkPalette
import com.jotape.inkshelf.ui.viewmodel.BuscaUiState

@Composable
fun BuscaScreen(
    state: BuscaUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleFileFavorite: (FileItem) -> Unit,
    onToggleFolderFavorite: (FolderItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInkPalette.current
    val focusRequester = remember { FocusRequester() }

    // Quem abre a busca quer digitar; pedir um clique no campo antes seria atrito à toa.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier.fillMaxSize().background(palette.bg)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Buscar por título, série, autor, editora…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            keyboardActions = KeyboardActions(onSearch = {}),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .focusRequester(focusRequester),
        )

        when {
            !state.hasQuery -> CenteredHint("Digite para buscar na sua biblioteca.")

            state.isEmpty -> CenteredHint("Nada encontrado para \"${state.query}\".")

            else -> {
                Text(
                    text = if (state.totalCount == 1) "1 resultado" else "${state.totalCount} resultados",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.folders.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "PASTAS",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textTertiary,
                            )
                        }
                        items(state.folders, key = { "folder:${it.id}" }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onOpenFolder(folder) },
                                onToggleFavorite = { onToggleFolderFavorite(folder) },
                            )
                        }
                    }
                    if (state.files.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "ARQUIVOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textTertiary,
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
            }
        }
    }
}

@Composable
private fun CenteredHint(text: String) {
    val palette = LocalInkPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = palette.textTertiary)
    }
}
