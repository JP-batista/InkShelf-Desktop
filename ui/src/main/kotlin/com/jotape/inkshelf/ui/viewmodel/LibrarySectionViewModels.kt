package com.jotape.inkshelf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.inkshelf.data.repository.LibraryRepository
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritosUiState(
    val folders: List<FolderItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
) {
    val isEmpty: Boolean get() = folders.isEmpty() && files.isEmpty()
}

/** Favoritos: pastas e arquivos marcados, em duas listas. */
class FavoritosViewModel(private val repository: LibraryRepository) : ViewModel() {

    val uiState: StateFlow<FavoritosUiState> = combine(
        repository.getFavoriteFolders(),
        repository.getFavoriteFiles(),
    ) { folders, files -> FavoritosUiState(folders, files) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritosUiState())

    fun setFileFavorite(file: FileItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFileFavorite(file.id, isFavorite) }
    }

    fun setFolderFavorite(folder: FolderItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFolderFavorite(folder.id, isFavorite) }
    }
}

data class ContinuarLendoUiState(
    val files: List<FileItem> = emptyList(),
) {
    val isEmpty: Boolean get() = files.isEmpty()
}

/**
 * Continuar lendo: o que foi começado e não terminado.
 *
 * A ordenação por progresso decrescente é decisão desta tela — quem está quase no fim tende a
 * ser o que o usuário quer retomar primeiro.
 */
class ContinuarLendoViewModel(private val repository: LibraryRepository) : ViewModel() {

    val uiState: StateFlow<ContinuarLendoUiState> = repository.getInProgressFiles()
        .map { files -> ContinuarLendoUiState(files.sortedByDescending { it.progress }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContinuarLendoUiState())

    fun setFileFavorite(file: FileItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFileFavorite(file.id, isFavorite) }
    }
}
