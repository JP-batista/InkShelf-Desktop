package com.jotape.inkshelf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.inkshelf.data.repository.LibraryRepository
import com.jotape.inkshelf.data.repository.LibrarySettingsKeys
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfiguracoesUiState(
    val darkTheme: Boolean = LibrarySettingsKeys.DEFAULT_DARK_MODE,
    val themeId: String = LibrarySettingsKeys.DEFAULT_THEME_ID,
    val columns: Int = LibrarySettingsKeys.DEFAULT_GRID_COLUMNS,
    val rootPaths: List<String> = emptyList(),
    val cacheLimitMb: Int = LibrarySettingsKeys.DEFAULT_CACHE_LIMIT_MB,
)

/** Tamanhos em disco, recalculados sob demanda (percorrer o cache não é de graça). */
data class ArmazenamentoUiState(
    val pageCacheBytes: Long = 0L,
    val coverBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isRefreshing: Boolean = false,
)

class ConfiguracoesViewModel(private val repository: LibraryRepository) : ViewModel() {

    val uiState: StateFlow<ConfiguracoesUiState> = combine(
        repository.getDarkModeFlow(),
        repository.getThemeIdFlow(),
        repository.getColumnsFlow(),
        repository.getRootPathsFlow(),
        repository.getCacheLimitMbFlow(),
    ) { dark, theme, columns, roots, cacheLimit ->
        ConfiguracoesUiState(dark, theme, columns, roots, cacheLimit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConfiguracoesUiState())

    private val _storage = MutableStateFlow(ArmazenamentoUiState())
    val storage: StateFlow<ArmazenamentoUiState> = _storage.asStateFlow()

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _storage.value = _storage.value.copy(isRefreshing = true)
            val pages = repository.getPageCacheSize()
            val covers = repository.getTotalCoverSize()
            val total = repository.getTotalAppStorageSize()
            _storage.value = ArmazenamentoUiState(pages, covers, total, isRefreshing = false)
        }
    }

    fun setDarkTheme(enabled: Boolean) = setSetting(LibrarySettingsKeys.SETTING_DARK_MODE, enabled.toString())

    fun setThemeId(themeId: String) = setSetting(LibrarySettingsKeys.SETTING_THEME_ID, themeId)

    fun setColumns(columns: Int) {
        val safe = columns.coerceIn(
            LibrarySettingsKeys.MIN_GRID_COLUMNS,
            LibrarySettingsKeys.MAX_GRID_COLUMNS,
        )
        setSetting(LibrarySettingsKeys.SETTING_COLUMNS, safe.toString())
    }

    fun setCacheLimitMb(mb: Int) {
        viewModelScope.launch { repository.setCacheLimitMb(mb.coerceAtLeast(0)) }
    }

    /** Remove uma raiz da biblioteca e revarre o que sobrou. */
    fun removeRoot(path: String) {
        viewModelScope.launch {
            repository.removeRootPath(path)
            val remaining = repository.getRootPathsOnce().map(::File).filter { it.isDirectory }
            // Sem raiz nenhuma não há o que conciliar; limpar aqui evita deixar a biblioteca
            // exibindo itens de uma pasta que o usuário acabou de remover.
            if (remaining.isEmpty()) repository.clearAll() else repository.rescanMultiple(remaining)
            refreshStorage()
        }
    }

    fun clearPageCache() {
        viewModelScope.launch {
            repository.clearPageCache()
            refreshStorage()
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            repository.clearAllCache()
            refreshStorage()
        }
    }

    private fun setSetting(key: String, value: String) {
        viewModelScope.launch { repository.setSetting(key, value) }
    }
}
