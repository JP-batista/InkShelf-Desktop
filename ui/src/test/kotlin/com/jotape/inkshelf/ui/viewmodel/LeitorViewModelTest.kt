package com.jotape.inkshelf.ui.viewmodel

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.reader.PageExtractor
import com.jotape.inkshelf.data.repository.LibraryRepository
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Testes de integração do leitor: banco real, arquivos reais, extração real.
 *
 * Por que não `runTest` com tempo virtual: o ViewModel extrai páginas em `Dispatchers.IO` e o
 * Room também roda suas consultas fora do dispatcher de teste. O relógio virtual não espera por
 * nada disso — `advanceUntilIdle()` retornaria antes de o trabalho terminar e os testes
 * passariam ou falhariam por sorte. Aqui a espera é real, com prazo, via [awaitUntil].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeitorViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: LibraryRepository
    private lateinit var viewModel: LeitorViewModel
    private lateinit var library: File

    @Before
    fun setUp() {
        // O viewModelScope roda no Dispatchers.Main, que não existe fora do Android.
        Dispatchers.setMain(Dispatchers.Default)
        InkShelfDatabase.closeAll()
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        repository = LibraryRepository()
        viewModel = LeitorViewModel(repository, PageExtractor())
        library = tempFolder.newFolder("Biblioteca")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        InkShelfDatabase.closeAll()
    }

    /** Espera a condição acontecer, falhando com mensagem clara se estourar o prazo. */
    private fun awaitUntil(timeoutMs: Long = 10_000, descricao: String, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(20)
        }
        throw AssertionError("tempo esgotado esperando: $descricao")
    }

    private fun createCbz(name: String, pageCount: Int): File {
        val file = File(library, name)
        file.parentFile.mkdirs()
        val image = BufferedImage(60, 90, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = Color.RED
            fillRect(0, 0, 60, 90)
            dispose()
        }
        val png = ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
        ZipOutputStream(file.outputStream()).use { zip ->
            repeat(pageCount) { index ->
                zip.putNextEntry(ZipEntry("%03d.png".format(index + 1)))
                zip.write(png)
                zip.closeEntry()
            }
        }
        return file
    }

    /** Varre a biblioteca para que o arquivo exista no banco, como em uso real. */
    private fun scan() = runBlocking { repository.initialScan(library) }

    private fun openAndWait(fileId: String, leitor: LeitorViewModel = viewModel) {
        leitor.open(fileId)
        awaitUntil(descricao = "o leitor terminar de abrir") {
            val s = leitor.uiState.value
            s.error != null || (s.pageCount > 0 && s.currentImage != null)
        }
    }

    private fun fileFromDb(id: String) = runBlocking { repository.getFileById(id) }

    @Test
    fun `abre o arquivo e carrega a primeira pagina`() {
        val cbz = createCbz("hq.cbz", pageCount = 5)
        scan()

        openAndWait(cbz.canonicalPath)

        val state = viewModel.uiState.value
        assertTrue(state.isOpen)
        assertEquals(5, state.pageCount)
        assertEquals(0, state.currentPage)
        assertNotNull("a primeira página deveria ter sido extraída", state.currentImage)
        assertEquals("1 / 5", state.pageLabel)
    }

    @Test
    fun `grava a contagem real de paginas na primeira abertura`() {
        val cbz = createCbz("hq.cbz", pageCount = 7)
        scan()
        // A varredura não abre arquivos, então a contagem começa zerada.
        assertEquals(0, fileFromDb(cbz.canonicalPath)!!.pages)

        openAndWait(cbz.canonicalPath)

        awaitUntil(descricao = "a contagem de páginas ser gravada") {
            fileFromDb(cbz.canonicalPath)!!.pages == 7
        }
    }

    @Test
    fun `avanca e volta paginas`() {
        val cbz = createCbz("hq.cbz", pageCount = 4)
        scan()
        openAndWait(cbz.canonicalPath)

        viewModel.nextPage()
        assertEquals(1, viewModel.uiState.value.currentPage)

        viewModel.previousPage()
        assertEquals(0, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `nao passa dos limites do arquivo`() {
        val cbz = createCbz("hq.cbz", pageCount = 3)
        scan()
        openAndWait(cbz.canonicalPath)

        viewModel.previousPage()
        assertEquals("não deveria ir antes da primeira", 0, viewModel.uiState.value.currentPage)

        viewModel.lastPage()
        viewModel.nextPage()
        assertEquals("não deveria passar da última", 2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `salva o progresso ao virar a pagina`() {
        val cbz = createCbz("hq.cbz", pageCount = 6)
        scan()
        openAndWait(cbz.canonicalPath)

        viewModel.goToPage(3)

        awaitUntil(descricao = "o progresso ser gravado") {
            fileFromDb(cbz.canonicalPath)!!.currentPage == 3
        }
    }

    @Test
    fun `retoma de onde parou`() {
        val cbz = createCbz("hq.cbz", pageCount = 8)
        scan()
        openAndWait(cbz.canonicalPath)
        viewModel.goToPage(5)
        awaitUntil(descricao = "o progresso ser gravado") {
            fileFromDb(cbz.canonicalPath)!!.currentPage == 5
        }
        viewModel.close()

        val outroLeitor = LeitorViewModel(repository, PageExtractor())
        openAndWait(cbz.canonicalPath, outroLeitor)

        assertEquals(5, outroLeitor.uiState.value.currentPage)
    }

    @Test
    fun `posicao salva alem do fim e limitada ao tamanho atual`() {
        // O arquivo pode ter sido substituído por um menor entre duas leituras.
        val cbz = createCbz("hq.cbz", pageCount = 10)
        scan()
        runBlocking { repository.updateProgress(cbz.canonicalPath, 9) }
        createCbz("hq.cbz", pageCount = 3)
        cbz.setLastModified(System.currentTimeMillis() + 5_000)

        openAndWait(cbz.canonicalPath)

        assertEquals(2, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `marca como lido ao chegar na ultima pagina`() {
        val cbz = createCbz("hq.cbz", pageCount = 3)
        scan()
        openAndWait(cbz.canonicalPath)
        assertFalse(fileFromDb(cbz.canonicalPath)!!.isRead)

        viewModel.lastPage()

        awaitUntil(descricao = "o arquivo ser marcado como lido") {
            fileFromDb(cbz.canonicalPath)!!.isRead
        }
    }

    @Test
    fun `arquivo ilegivel abre com mensagem de erro em vez de estourar`() {
        val quebrado = File(library, "quebrado.cbz").apply { writeText("lixo") }
        scan()

        viewModel.open(quebrado.canonicalPath)
        awaitUntil(descricao = "o erro ser reportado") { viewModel.uiState.value.error != null }

        assertEquals(0, viewModel.uiState.value.pageCount)
    }

    @Test
    fun `arquivo que sumiu do disco reporta erro`() {
        val cbz = createCbz("hq.cbz", pageCount = 2)
        scan()
        val id = cbz.canonicalPath
        cbz.delete()

        viewModel.open(id)

        awaitUntil(descricao = "o erro ser reportado") { viewModel.uiState.value.error != null }
    }

    @Test
    fun `fechar limpa o estado`() {
        val cbz = createCbz("hq.cbz", pageCount = 3)
        scan()
        openAndWait(cbz.canonicalPath)

        viewModel.close()

        assertFalse(viewModel.uiState.value.isOpen)
    }
}
