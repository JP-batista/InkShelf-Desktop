package com.jotape.inkshelf.data.cover

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.repository.LibraryRepository
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoverGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: LibraryRepository
    private lateinit var generator: CoverGenerator
    private lateinit var library: File

    @Before
    fun setUp() {
        InkShelfDatabase.closeAll()
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        repository = LibraryRepository()
        generator = CoverGenerator(repository, CoverExtractor())
        library = tempFolder.newFolder("Biblioteca")
    }

    @After
    fun tearDown() {
        InkShelfDatabase.closeAll()
    }

    private fun createCbz(path: String): File {
        val file = File(library, path)
        file.parentFile.mkdirs()
        val image = BufferedImage(80, 120, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = Color.RED
            fillRect(0, 0, 80, 120)
            dispose()
        }
        val png = ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("001.png"))
            zip.write(png)
            zip.closeEntry()
        }
        return file
    }

    @Test
    fun `gera capa para os arquivos varridos`() = runTest {
        createCbz("Batman/batman-001.cbz")
        createCbz("Batman/batman-002.cbz")
        repository.initialScan(library)

        generator.generateMissing()

        val arquivos = repository.getAllFilesOnce()
        assertEquals(2, arquivos.size)
        arquivos.forEach { arquivo ->
            assertNotNull("${arquivo.title} ficou sem capa", arquivo.coverPath)
            assertTrue(File(arquivo.coverPath!!).exists())
        }
    }

    @Test
    fun `nao regera capa ja existente`() = runTest {
        val cbz = createCbz("Batman/batman-001.cbz")
        repository.initialScan(library)
        generator.generateMissing()
        val capa = File(repository.getFileById(cbz.canonicalPath)!!.coverPath!!)
        val marca = capa.lastModified()

        generator.generateMissing()

        assertEquals("a capa foi regerada à toa", marca, capa.lastModified())
    }

    @Test
    fun `regera capa que sumiu do disco`() = runTest {
        val cbz = createCbz("Batman/batman-001.cbz")
        repository.initialScan(library)
        generator.generateMissing()
        val capa = File(repository.getFileById(cbz.canonicalPath)!!.coverPath!!)
        capa.delete()

        generator.generateMissing()

        assertTrue("a capa apagada deveria ter voltado", capa.exists())
    }

    @Test
    fun `arquivo ilegivel nao impede as capas dos demais`() = runTest {
        createCbz("Batman/bom.cbz")
        File(library, "Batman/quebrado.cbz").writeText("lixo")
        repository.initialScan(library)

        generator.generateMissing()

        val comCapa = repository.getAllFilesOnce().filter { it.coverPath != null }
        assertEquals(listOf("bom.cbz"), comCapa.map { it.title })
    }

    @Test
    fun `avisa a cada capa pronta`() = runTest {
        createCbz("Batman/batman-001.cbz")
        createCbz("Batman/batman-002.cbz")
        repository.initialScan(library)

        val prontas = mutableListOf<String>()
        generator.generateMissing { fileId -> synchronized(prontas) { prontas += fileId } }

        assertEquals(2, prontas.size)
    }
}
