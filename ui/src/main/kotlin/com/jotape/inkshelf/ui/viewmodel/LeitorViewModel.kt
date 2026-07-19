package com.jotape.inkshelf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotape.inkshelf.data.reader.PageExtractor
import com.jotape.inkshelf.data.reader.ReaderPage
import com.jotape.inkshelf.data.repository.LibraryRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LeitorUiState(
    val fileId: String? = null,
    val title: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val currentImage: File? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val isOpen: Boolean get() = fileId != null
    val hasNext: Boolean get() = currentPage < pageCount - 1
    val hasPrevious: Boolean get() = currentPage > 0
    /** Rótulo "3 / 24", em base 1 — o índice interno é base 0. */
    val pageLabel: String get() = if (pageCount > 0) "${currentPage + 1} / $pageCount" else ""
}

/**
 * Estado do leitor: qual arquivo está aberto, em que página, e a imagem já extraída dela.
 *
 * A extração acontece fora da thread de UI e a página só troca quando a imagem está pronta, para
 * que virar página nunca mostre um quadro em branco.
 */
class LeitorViewModel(
    private val repository: LibraryRepository,
    private val pageExtractor: PageExtractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeitorUiState())
    val uiState: StateFlow<LeitorUiState> = _uiState.asStateFlow()

    private var pages: List<ReaderPage> = emptyList()
    private var pageLoadJob: Job? = null
    private var progressSaveJob: Job? = null

    /** Tamanho alvo de renderização — a janela informa o seu ao abrir. */
    private var viewportWidth = 1600
    private var viewportHeight = 1200

    fun setViewport(width: Int, height: Int) {
        if (width > 0) viewportWidth = width
        if (height > 0) viewportHeight = height
    }

    fun open(fileId: String) {
        if (_uiState.value.fileId == fileId) return

        _uiState.value = LeitorUiState(fileId = fileId, isLoading = true)

        viewModelScope.launch {
            val file = repository.getFileById(fileId)
            val source = File(fileId)

            if (file == null || !source.isFile) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Arquivo não encontrado no disco.")
                }
                return@launch
            }

            val loaded = withContext(Dispatchers.IO) { pageExtractor.getPages(fileId, source) }
            if (loaded.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = file.title,
                        error = "Não foi possível ler as páginas deste arquivo.",
                    )
                }
                return@launch
            }

            pages = loaded

            // A contagem real de páginas só é conhecida aqui, na primeira abertura — a varredura
            // não abre os arquivos. Gravar mantém o progresso da biblioteca correto.
            if (file.pages != loaded.size) repository.updatePages(fileId, loaded.size)

            // Retoma de onde parou, mas nunca além do fim (o arquivo pode ter encolhido).
            val startPage = file.currentPage.coerceIn(0, loaded.lastIndex)

            _uiState.update {
                it.copy(title = file.title, pageCount = loaded.size, currentPage = startPage)
            }
            loadPage(startPage)
        }
    }

    fun close() {
        pageLoadJob?.cancel()
        progressSaveJob?.cancel()
        // Grava na hora: fechar é o momento em que o usuário espera que a posição fique salva.
        _uiState.value.fileId?.let { fileId ->
            val page = _uiState.value.currentPage
            viewModelScope.launch { persistProgress(fileId, page) }
        }
        pages = emptyList()
        _uiState.value = LeitorUiState()
    }

    fun goToPage(index: Int) {
        val target = index.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        if (pages.isEmpty() || target == _uiState.value.currentPage) return
        _uiState.update { it.copy(currentPage = target) }
        loadPage(target)
        scheduleProgressSave(target)
    }

    fun nextPage() = goToPage(_uiState.value.currentPage + 1)

    fun previousPage() = goToPage(_uiState.value.currentPage - 1)

    fun firstPage() = goToPage(0)

    fun lastPage() = goToPage(pages.lastIndex)

    private fun loadPage(index: Int) {
        val page = pages.getOrNull(index) ?: return

        pageLoadJob?.cancel()
        pageLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val image = withContext(Dispatchers.IO) {
                pageExtractor.getPageImage(page, viewportWidth, viewportHeight)
            }

            _uiState.update {
                // Guarda contra corrida: se o usuário já virou de novo, este resultado é velho.
                if (it.currentPage != index) it
                else it.copy(currentImage = image, isLoading = false)
            }

            // Vizinhas em segundo plano, para que virar a página seja instantâneo.
            withContext(Dispatchers.IO) {
                pageExtractor.prefetchAround(pages, index, radius = 2, width = viewportWidth, height = viewportHeight)
            }
        }
    }

    /**
     * Adia a gravação do progresso. Folhear rápido dispara dezenas de trocas de página por
     * segundo; sem o atraso, cada uma viraria uma escrita no banco.
     */
    private fun scheduleProgressSave(page: Int) {
        val fileId = _uiState.value.fileId ?: return
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(PROGRESS_SAVE_DELAY_MS)
            persistProgress(fileId, page)
        }
    }

    private suspend fun persistProgress(fileId: String, page: Int) {
        repository.updateProgress(fileId, page)
        // Marca como lido ao chegar na última página — é o que o app mobile faz.
        if (pages.isNotEmpty() && page >= pages.lastIndex) {
            repository.setFileRead(fileId, true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pageLoadJob?.cancel()
        progressSaveJob?.cancel()
    }

    private companion object {
        const val PROGRESS_SAVE_DELAY_MS = 600L
    }
}
