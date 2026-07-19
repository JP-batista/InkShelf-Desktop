package com.jotape.inkshelf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.ui.screens.BibliotecaScreen
import com.jotape.inkshelf.ui.screens.BuscaScreen
import com.jotape.inkshelf.ui.screens.ConfiguracoesScreen
import com.jotape.inkshelf.ui.screens.ContinuarLendoScreen
import com.jotape.inkshelf.ui.screens.FavoritosScreen
import com.jotape.inkshelf.ui.screens.LeitorScreen
import com.jotape.inkshelf.ui.theme.LocalInkPalette
import com.jotape.inkshelf.ui.viewmodel.BibliotecaViewModel
import com.jotape.inkshelf.ui.viewmodel.BuscaViewModel
import com.jotape.inkshelf.ui.viewmodel.ConfiguracoesViewModel
import com.jotape.inkshelf.ui.viewmodel.ContinuarLendoViewModel
import com.jotape.inkshelf.ui.viewmodel.FavoritosViewModel
import com.jotape.inkshelf.ui.viewmodel.LeitorViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Seções de topo do app.
 *
 * Nota de porte: no celular estas seções ficavam numa barra inferior. No desktop a convenção é
 * uma barra lateral — a janela é larga e baixa, então gastar altura com navegação tira espaço
 * justamente de onde as capas aparecem.
 */
private enum class InkSection(val label: String, val icon: ImageVector) {
    BIBLIOTECA("Biblioteca", Icons.AutoMirrored.Filled.MenuBook),
    CONTINUAR("Continuar", Icons.Default.PlayArrow),
    FAVORITOS("Favoritos", Icons.Default.Favorite),
    BUSCA("Buscar", Icons.Default.Search),
    CONFIGURACOES("Ajustes", Icons.Default.Settings),
}

@Composable
fun InkShelfApp(
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
) {
    var section by remember { mutableStateOf(InkSection.BIBLIOTECA) }

    val biblioteca = koinViewModel<BibliotecaViewModel>()
    val favoritos = koinViewModel<FavoritosViewModel>()
    val continuar = koinViewModel<ContinuarLendoViewModel>()
    val busca = koinViewModel<BuscaViewModel>()
    val configuracoes = koinViewModel<ConfiguracoesViewModel>()
    val leitor = koinViewModel<LeitorViewModel>()

    val leitorState by leitor.uiState.collectAsState()

    // O leitor cobre a janela inteira em vez de ocupar uma seção: ler é uma atividade de tela
    // cheia, e a barra lateral só roubaria espaço da página.
    if (leitorState.isOpen) {
        LeitorScreen(
            state = leitorState,
            isFullscreen = isFullscreen,
            onClose = {
                if (isFullscreen) onToggleFullscreen()
                leitor.close()
            },
            onNextPage = leitor::nextPage,
            onPreviousPage = leitor::previousPage,
            onFirstPage = leitor::firstPage,
            onLastPage = leitor::lastPage,
            onToggleFullscreen = onToggleFullscreen,
            onViewportChanged = leitor::setViewport,
        )
        return
    }

    val onOpenFile: (String) -> Unit = { fileId -> leitor.open(fileId) }

    // Abrir uma pasta a partir de Favoritos leva o usuário para a Biblioteca já dentro dela —
    // caso contrário o clique pareceria não fazer nada.
    val openFolderInLibrary: (FolderItem) -> Unit = { folder ->
        biblioteca.openFolder(folder)
        section = InkSection.BIBLIOTECA
    }

    Row(Modifier.fillMaxSize()) {
        SectionRail(current = section, onSelect = { section = it })

        when (section) {
            InkSection.BIBLIOTECA -> {
                val state by biblioteca.uiState.collectAsState()
                BibliotecaScreen(
                    state = state,
                    onOpenFolder = biblioteca::openFolder,
                    onOpenFile = { onOpenFile(it.id) },
                    onToggleFileFavorite = { biblioteca.setFileFavorite(it, !it.isFavorite) },
                    onToggleFolderFavorite = { biblioteca.setFolderFavorite(it, !it.isFavorite) },
                    onBreadcrumbClick = biblioteca::navigateToBreadcrumb,
                    onPickLibraryFolder = {
                        FolderPicker.chooseDirectory()?.let(biblioteca::addLibraryRoot)
                    },
                    onRescan = biblioteca::rescanAll,
                )
            }

            InkSection.CONTINUAR -> {
                val state by continuar.uiState.collectAsState()
                ContinuarLendoScreen(
                    state = state,
                    onOpenFile = { onOpenFile(it.id) },
                    onToggleFileFavorite = { continuar.setFileFavorite(it, !it.isFavorite) },
                )
            }

            InkSection.FAVORITOS -> {
                val state by favoritos.uiState.collectAsState()
                FavoritosScreen(
                    state = state,
                    onOpenFolder = openFolderInLibrary,
                    onOpenFile = { onOpenFile(it.id) },
                    onToggleFileFavorite = { favoritos.setFileFavorite(it, !it.isFavorite) },
                    onToggleFolderFavorite = { favoritos.setFolderFavorite(it, !it.isFavorite) },
                )
            }

            InkSection.BUSCA -> {
                val state by busca.uiState.collectAsState()
                val query by busca.currentQuery.collectAsState()
                BuscaScreen(
                    state = state,
                    query = query,
                    onQueryChange = busca::onQueryChange,
                    onClear = busca::clear,
                    onOpenFolder = openFolderInLibrary,
                    onOpenFile = { onOpenFile(it.id) },
                    onToggleFileFavorite = { busca.setFileFavorite(it, !it.isFavorite) },
                    onToggleFolderFavorite = { busca.setFolderFavorite(it, !it.isFavorite) },
                )
            }

            InkSection.CONFIGURACOES -> {
                val state by configuracoes.uiState.collectAsState()
                val storage by configuracoes.storage.collectAsState()
                ConfiguracoesScreen(
                    state = state,
                    storage = storage,
                    onDarkThemeChange = configuracoes::setDarkTheme,
                    onThemeChange = configuracoes::setThemeId,
                    onColumnsChange = configuracoes::setColumns,
                    onCacheLimitChange = configuracoes::setCacheLimitMb,
                    onAddRoot = {
                        FolderPicker.chooseDirectory()?.let(biblioteca::addLibraryRoot)
                    },
                    onRemoveRoot = configuracoes::removeRoot,
                    onClearPageCache = configuracoes::clearPageCache,
                    onClearAllCache = configuracoes::clearAllCache,
                    onRefreshStorage = configuracoes::refreshStorage,
                )
            }
        }
    }
}

@Composable
private fun SectionRail(current: InkSection, onSelect: (InkSection) -> Unit) {
    val palette = LocalInkPalette.current

    NavigationRail(
        containerColor = palette.bg2,
        modifier = Modifier.fillMaxHeight().width(96.dp),
        header = {
            Text(
                text = "INKSHELF",
                style = MaterialTheme.typography.titleSmall,
                color = palette.accent,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            InkSection.entries.forEach { entry ->
                NavigationRailItem(
                    selected = entry == current,
                    onClick = { onSelect(entry) },
                    icon = { Icon(entry.icon, contentDescription = entry.label) },
                    label = {
                        Text(entry.label, style = MaterialTheme.typography.labelSmall)
                    },
                )
            }
        }
    }
}
