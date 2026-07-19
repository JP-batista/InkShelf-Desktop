package com.jotape.inkshelf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.inkshelf.data.repository.LibraryRepository
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BuscaUiState(
    val query: String = "",
    val folders: List<FolderItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val isEmpty: Boolean get() = folders.isEmpty() && files.isEmpty()
    val totalCount: Int get() = folders.size + files.size
}

/**
 * Busca por nome e por metadados de ComicInfo (série, autor, editora, sinopse — a consulta do
 * repositório cobre todos).
 */
class BuscaViewModel(private val repository: LibraryRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    val currentQuery: StateFlow<String> = query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<BuscaUiState> = query
        // Sem o atraso, cada tecla digitada dispara duas consultas com LIKE '%...%' sobre a
        // biblioteca inteira — que não usa índice e varre a tabela.
        .debounce(DEBOUNCE_MS)
        .flatMapLatest { text ->
            if (text.isBlank()) {
                flowOf(BuscaUiState(query = text))
            } else {
                combine(
                    repository.searchFolders(text),
                    repository.searchFiles(text),
                ) { folders, files -> BuscaUiState(text, folders, files) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuscaUiState())

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun clear() {
        query.value = ""
    }

    fun setFileFavorite(file: FileItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFileFavorite(file.id, isFavorite) }
    }

    fun setFolderFavorite(folder: FolderItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFolderFavorite(folder.id, isFavorite) }
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
    }
}
