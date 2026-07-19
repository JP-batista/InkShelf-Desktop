package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Percurso completo, do disco até a consulta: varre uma pasta de verdade com CBZs de verdade e
 * consulta pela fachada, que é exatamente o que a UI vai fazer na Fase 3. É o teste que prova
 * que as peças da Fase 1 e da Fase 2 se encaixam.
 */
class LibraryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repo: LibraryRepository
    private lateinit var library: File

    @Before
    fun setUp() {
        InkShelfDatabase.closeAll()
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        repo = LibraryRepository()
        library = tempFolder.newFolder("Biblioteca")
    }

    @After
    fun tearDown() {
        InkShelfDatabase.closeAll()
    }

    private fun pngBytes(): ByteArray {
        val image = BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = Color.RED
            fillRect(0, 0, 40, 60)
            dispose()
        }
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    private fun createCbz(path: String, pageCount: Int = 3): File {
        val file = File(library, path)
        file.parentFile.mkdirs()
        ZipOutputStream(file.outputStream()).use { zip ->
            repeat(pageCount) { index ->
                zip.putNextEntry(ZipEntry("%03d.png".format(index + 1)))
                zip.write(pngBytes())
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun `varre a biblioteca e consulta pela fachada`() = runTest {
        createCbz("Batman/batman-001.cbz")
        createCbz("Batman/batman-002.cbz")
        createCbz("Superman/superman-001.cbz")

        repo.initialScan(library)

        val pastas = repo.getFolders("root").first()
        assertEquals(setOf("Batman", "Superman"), pastas.map { it.title }.toSet())

        val batman = pastas.first { it.title == "Batman" }
        val arquivos = repo.getFiles(batman.id).first()
        assertEquals(listOf("batman-001.cbz", "batman-002.cbz"), arquivos.map { it.title })
    }

    @Test
    fun `favoritar um arquivo o faz aparecer entre os favoritos`() = runTest {
        val cbz = createCbz("Batman/batman-001.cbz")
        repo.initialScan(library)

        repo.setFileFavorite(cbz.canonicalPath, true)

        val favoritos = repo.getFavoriteFiles().first()
        assertEquals(listOf("batman-001.cbz"), favoritos.map { it.title })
    }

    @Test
    fun `progresso de leitura alimenta o continuar lendo`() = runTest {
        val cbz = createCbz("Batman/batman-001.cbz", pageCount = 10)
        repo.initialScan(library)
        val id = cbz.canonicalPath

        repo.updatePages(id, 10)
        repo.updateProgress(id, 4)

        val emAndamento = repo.getInProgressFiles().first()
        assertEquals(1, emAndamento.size)
        assertEquals(4, emAndamento.first().currentPage)
    }

    @Test
    fun `arquivo lido sai do continuar lendo`() = runTest {
        val cbz = createCbz("Batman/batman-001.cbz", pageCount = 10)
        repo.initialScan(library)
        val id = cbz.canonicalPath
        repo.updatePages(id, 10)
        repo.updateProgress(id, 4)

        repo.setFileRead(id, true)

        assertTrue(repo.getInProgressFiles().first().isEmpty())
    }

    @Test
    fun `busca encontra por parte do nome`() = runTest {
        createCbz("Batman/batman-001.cbz")
        createCbz("Superman/superman-001.cbz")
        repo.initialScan(library)

        val achados = repo.searchFiles("superman").first()

        assertEquals(listOf("superman-001.cbz"), achados.map { it.title })
    }

    @Test
    fun `nova varredura preserva favorito e progresso`() = runTest {
        val cbz = createCbz("Batman/batman-001.cbz", pageCount = 10)
        repo.initialScan(library)
        val id = cbz.canonicalPath
        repo.updatePages(id, 10)
        repo.updateProgress(id, 7)
        repo.setFileFavorite(id, true)

        createCbz("Batman/batman-002.cbz")
        repo.rescan(library)

        val arquivo = repo.getFileById(id)!!
        assertEquals(7, arquivo.currentPage)
        assertTrue(arquivo.isFavorite)
        assertEquals(2, repo.getFiles(repo.getFolders("root").first().first().id).first().size)
    }

    @Test
    fun `preferencia gravada pela fachada e lida de volta`() = runTest {
        repo.setSetting(LibrarySettingsKeys.SETTING_THEME_ID, "purple")

        assertEquals("purple", repo.getThemeIdFlow().first())
    }

    @Test
    fun `limpar tudo esvazia a biblioteca`() = runTest {
        createCbz("Batman/batman-001.cbz")
        repo.initialScan(library)
        assertTrue(repo.getFolders("root").first().isNotEmpty())

        repo.clearAll()

        assertTrue(repo.getFolders("root").first().isEmpty())
        assertNotNull(repo.getThemeIdFlow().first())
    }
}
