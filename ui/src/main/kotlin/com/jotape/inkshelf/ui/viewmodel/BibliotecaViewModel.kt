package com.jotape.inkshelf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.inkshelf.data.cover.CoverGenerator
import com.jotape.inkshelf.data.repository.LibraryRepository
import com.jotape.inkshelf.data.repository.LibrarySettingsKeys
import com.jotape.inkshelf.data.scanner.ScanState
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.model.LibraryViewMode
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Um degrau da trilha de navegação ("Biblioteca › DC › Batman"). */
data class BreadcrumbItem(val folderId: String, val title: String)

data class BibliotecaUiState(
    val currentFolderId: String = ROOT,
    val folders: List<FolderItem> = emptyList(),
    val files: List<FileItem> = emptyList(),
    val breadcrumbs: List<BreadcrumbItem> = listOf(BreadcrumbItem(ROOT, "Biblioteca")),
    val columns: Int = LibrarySettingsKeys.DEFAULT_GRID_COLUMNS,
    val viewMode: LibraryViewMode = LibrarySettingsKeys.DEFAULT_VIEW_MODE,
    val hasRootPath: Boolean = false,
    val isScanning: Boolean = false,
    val isInitializing: Boolean = true,
) {
    val isEmpty: Boolean get() = folders.isEmpty() && files.isEmpty()

    companion object {
        const val ROOT = "root"
    }
}

/**
 * Estado da tela de biblioteca: onde o usuário está na árvore, o que há nessa pasta e as
 * preferências de exibição.
 *
 * Nota de porte: no Android era um `AndroidViewModel`, porque precisava do `Application` para
 * criar o repositório e ler `R.string`. Aqui o repositório chega por injeção e os rótulos são
 * literais, então é um `ViewModel` comum — o mesmo da biblioteca multiplataforma.
 */
class BibliotecaViewModel(
    private val repository: LibraryRepository,
    private val coverGenerator: CoverGenerator,
) : ViewModel() {

    private val currentFolderId = MutableStateFlow(BibliotecaUiState.ROOT)
    private val breadcrumbs = MutableStateFlow(
        listOf(BreadcrumbItem(BibliotecaUiState.ROOT, "Biblioteca")),
    )
    private val initializing = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content = currentFolderId.flatMapLatest { folderId ->
        combine(
            repository.getFolders(folderId),
            repository.getFiles(folderId),
        ) { folders, files -> folders to files }
    }

    val uiState: StateFlow<BibliotecaUiState> = combine(
        content,
        combine(currentFolderId, breadcrumbs) { id, crumbs -> id to crumbs },
        combine(repository.getColumnsFlow(), repository.getViewModeFlow()) { c, v -> c to v },
        combine(repository.getRootPathsFlow(), ScanState.isScanning) { roots, scanning ->
            roots to scanning
        },
        initializing,
    ) { (folders, files), (folderId, crumbs), (columns, viewMode), (roots, scanning), init ->
        BibliotecaUiState(
            currentFolderId = folderId,
            folders = folders,
            files = files,
            breadcrumbs = crumbs,
            columns = columns,
            viewMode = viewMode,
            hasRootPath = roots.isNotEmpty(),
            isScanning = scanning,
            isInitializing = init,
        )
    }.stateIn(
        scope = viewModelScope,
        // WhileSubscribed(5s) e não Eagerly: sem assinante a consulta ao banco para, mas uma
        // troca rápida de tela não refaz tudo do zero.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BibliotecaUiState(),
    )

    init {
        viewModelScope.launch {
            // Só depois de saber se existe raiz configurada a tela pode decidir entre mostrar a
            // biblioteca e convidar o usuário a escolher uma pasta. Sem isto, a tela pisca o
            // convite antes dos dados chegarem.
            repository.getRootPathsOnce()
            initializing.value = false
        }
    }

    fun openFolder(folder: FolderItem) {
        currentFolderId.value = folder.id
        breadcrumbs.value = breadcrumbs.value + BreadcrumbItem(folder.id, folder.title)
    }

    /** Volta para um degrau específico da trilha (clique no meio do caminho). */
    fun navigateToBreadcrumb(item: BreadcrumbItem) {
        val index = breadcrumbs.value.indexOfFirst { it.folderId == item.folderId }
        if (index < 0) return
        breadcrumbs.value = breadcrumbs.value.take(index + 1)
        currentFolderId.value = item.folderId
    }

    /** Sobe um nível. Devolve `false` quando já está na raiz — a janela usa isso para o Voltar. */
    fun navigateUp(): Boolean {
        val crumbs = breadcrumbs.value
        if (crumbs.size <= 1) return false
        val parent = crumbs[crumbs.size - 2]
        breadcrumbs.value = crumbs.dropLast(1)
        currentFolderId.value = parent.folderId
        return true
    }

    /** Adiciona uma pasta à biblioteca e a varre. */
    fun addLibraryRoot(root: File) {
        viewModelScope.launch {
            ScanState.setScanning(true)
            try {
                repository.addRootPath(root.absolutePath)
                repository.addRootScan(root)
            } finally {
                ScanState.setScanning(false)
            }
            // Fora do bloco de varredura de propósito: a biblioteca já pode ser navegada
            // enquanto as capas ainda estão sendo geradas.
            coverGenerator.generateMissing()
        }
    }

    /** Revarre todas as raízes, conciliando o banco com o que está no disco agora. */
    fun rescanAll() {
        viewModelScope.launch {
            ScanState.setScanning(true)
            try {
                val roots = repository.getRootPathsOnce().map(::File).filter { it.isDirectory }
                if (roots.isNotEmpty()) repository.rescanMultiple(roots)
            } finally {
                ScanState.setScanning(false)
            }
            coverGenerator.generateMissing()
        }
    }

    fun setFileFavorite(file: FileItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFileFavorite(file.id, isFavorite) }
    }

    fun setFolderFavorite(folder: FolderItem, isFavorite: Boolean) {
        viewModelScope.launch { repository.setFolderFavorite(folder.id, isFavorite) }
    }
}
