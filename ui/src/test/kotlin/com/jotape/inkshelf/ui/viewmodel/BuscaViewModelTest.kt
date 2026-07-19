package com.jotape.inkshelf.ui.viewmodel

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.repository.LibraryRepository
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Ver a nota em [LeitorViewModelTest] sobre espera real em vez de tempo virtual. */
@OptIn(ExperimentalCoroutinesApi::class)
class BuscaViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: LibraryRepository
    private lateinit var viewModel: BuscaViewModel
    private lateinit var library: File
    private lateinit var collector: Job

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        InkShelfDatabase.closeAll()
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        repository = LibraryRepository()
        viewModel = BuscaViewModel(repository)
        library = tempFolder.newFolder("Biblioteca")

        // O uiState usa WhileSubscribed: sem alguém coletando, o fluxo nunca roda.
        collector = CoroutineScope(Dispatchers.Default).launch { viewModel.uiState.collect {} }
    }

    @After
    fun tearDown() {
        collector.cancel()
        Dispatchers.resetMain()
        InkShelfDatabase.closeAll()
    }

    private fun awaitUntil(timeoutMs: Long = 10_000, descricao: String, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(20)
        }
        throw AssertionError("tempo esgotado esperando: $descricao")
    }

    private fun createCbz(path: String) {
        val file = File(library, path)
        file.parentFile.mkdirs()
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("001.png"))
            zip.write(ByteArray(16))
            zip.closeEntry()
        }
    }

    private fun scan() = runBlocking { repository.initialScan(library) }

    @Test
    fun `encontra arquivo por parte do nome`() {
        createCbz("Batman/batman-001.cbz")
        createCbz("Superman/superman-001.cbz")
        scan()

        viewModel.onQueryChange("superman")

        awaitUntil(descricao = "o resultado da busca") {
            viewModel.uiState.value.files.map { it.title } == listOf("superman-001.cbz")
        }
    }

    @Test
    fun `encontra pastas alem de arquivos`() {
        createCbz("Batman/batman-001.cbz")
        scan()

        viewModel.onQueryChange("batman")

        awaitUntil(descricao = "pasta e arquivo encontrados") {
            val s = viewModel.uiState.value
            s.folders.size == 1 && s.files.size == 1
        }
    }

    @Test
    fun `busca vazia nao devolve a biblioteca inteira`() {
        createCbz("Batman/batman-001.cbz")
        scan()
        viewModel.onQueryChange("batman")
        awaitUntil(descricao = "resultados aparecerem") {
            !viewModel.uiState.value.isEmpty
        }

        viewModel.clear()

        awaitUntil(descricao = "os resultados serem limpos") {
            val s = viewModel.uiState.value
            !s.hasQuery && s.isEmpty
        }
    }

    @Test
    fun `busca sem correspondencia devolve vazio`() {
        createCbz("Batman/batman-001.cbz")
        scan()

        viewModel.onQueryChange("naoexiste")

        awaitUntil(descricao = "busca sem resultado") {
            val s = viewModel.uiState.value
            s.query == "naoexiste" && s.isEmpty
        }
    }

    @Test
    fun `busca ignora diferenca de caixa`() {
        createCbz("Batman/batman-001.cbz")
        scan()

        viewModel.onQueryChange("BATMAN")

        awaitUntil(descricao = "busca insensível a caixa") {
            viewModel.uiState.value.files.isNotEmpty()
        }
    }

    @Test
    fun `conta o total de resultados`() {
        createCbz("Batman/batman-001.cbz")
        createCbz("Batman/batman-002.cbz")
        scan()

        viewModel.onQueryChange("batman")

        awaitUntil(descricao = "contagem de resultados") {
            // 2 arquivos + a pasta Batman
            viewModel.uiState.value.totalCount == 3
        }
        assertTrue(viewModel.uiState.value.hasQuery)
        assertEquals("batman", viewModel.uiState.value.query)
    }
}
