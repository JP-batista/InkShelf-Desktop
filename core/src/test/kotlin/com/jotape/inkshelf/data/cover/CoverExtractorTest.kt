package com.jotape.inkshelf.data.cover

import com.jotape.inkshelf.data.InkPaths
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoverExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var covers: CoverExtractor
    private lateinit var comics: File

    @Before
    fun setUp() {
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        covers = CoverExtractor()
        comics = tempFolder.newFolder("Quadrinhos")
    }

    private fun pngBytes(width: Int, height: Int, color: Color): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = color
            fillRect(0, 0, width, height)
            dispose()
        }
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    private fun buildCbz(name: String, pages: List<Pair<String, ByteArray>>): File {
        val file = File(comics, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            pages.forEach { (entryName, bytes) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun `capa e gerada a partir da primeira pagina`() {
        val cbz = buildCbz(
            "hq.cbz",
            listOf(
                "001.png" to pngBytes(400, 600, Color.RED),
                "002.png" to pngBytes(400, 600, Color.BLUE),
            ),
        )

        val cover = covers.extractCover(cbz.canonicalPath, cbz)

        assertNotNull(cover)
        val arquivo = File(cover!!.thumbnailPath)
        assertTrue(arquivo.exists())
        assertEquals(arquivo.length(), cover.sizeBytes)
        assertNotNull(ImageIO.read(arquivo))
    }

    @Test
    fun `imagem grande e reduzida ao limite mantendo a proporcao`() {
        val cbz = buildCbz("hq.cbz", listOf("001.png" to pngBytes(2000, 3000, Color.RED)))

        val cover = covers.extractCover(cbz.canonicalPath, cbz)!!
        val imagem = ImageIO.read(File(cover.thumbnailPath))

        assertEquals(CoverExtractor.COVER_MAX_SIZE, maxOf(imagem.width, imagem.height))
        // Proporção original 2:3 preservada.
        assertEquals(2.0 / 3.0, imagem.width.toDouble() / imagem.height, 0.02)
    }

    @Test
    fun `imagem menor que o limite nao e ampliada`() {
        val cbz = buildCbz("hq.cbz", listOf("001.png" to pngBytes(100, 150, Color.RED)))

        val cover = covers.extractCover(cbz.canonicalPath, cbz)!!
        val imagem = ImageIO.read(File(cover.thumbnailPath))

        assertEquals(100, imagem.width)
        assertEquals(150, imagem.height)
    }

    @Test
    fun `a ordem natural decide qual pagina vira capa`() {
        // "pagina2" tem que ser a capa, não "pagina10".
        val cbz = buildCbz(
            "hq.cbz",
            listOf(
                "pagina10.png" to pngBytes(400, 600, Color.BLUE),
                "pagina2.png" to pngBytes(300, 450, Color.RED),
            ),
        )

        val cover = covers.extractCover(cbz.canonicalPath, cbz)!!
        val imagem = ImageIO.read(File(cover.thumbnailPath))

        assertEquals("a capa deveria ser pagina2", 300, imagem.width)
    }

    @Test
    fun `arquivo sem paginas nao gera capa`() {
        val cbz = buildCbz("vazio.cbz", listOf("leiame.txt" to "nada".toByteArray()))

        assertNull(covers.extractCover(cbz.canonicalPath, cbz))
    }

    @Test
    fun `arquivo corrompido nao gera capa nem estoura`() {
        val quebrado = File(comics, "quebrado.cbz").apply { writeText("lixo") }

        assertNull(covers.extractCover(quebrado.canonicalPath, quebrado))
    }

    @Test
    fun `apagar a capa de um arquivo nao afeta a dos outros`() {
        val a = buildCbz("a.cbz", listOf("001.png" to pngBytes(400, 600, Color.RED)))
        val b = buildCbz("b.cbz", listOf("001.png" to pngBytes(400, 600, Color.BLUE)))
        val capaA = File(covers.extractCover(a.canonicalPath, a)!!.thumbnailPath)
        val capaB = File(covers.extractCover(b.canonicalPath, b)!!.thumbnailPath)

        covers.deleteCover(a.canonicalPath)

        assertTrue(!capaA.exists())
        assertTrue(capaB.exists())
    }

    @Test
    fun `retainOnly descarta capas de arquivos que sairam da biblioteca`() {
        val a = buildCbz("a.cbz", listOf("001.png" to pngBytes(400, 600, Color.RED)))
        val b = buildCbz("b.cbz", listOf("001.png" to pngBytes(400, 600, Color.BLUE)))
        val capaA = File(covers.extractCover(a.canonicalPath, a)!!.thumbnailPath)
        val capaB = File(covers.extractCover(b.canonicalPath, b)!!.thumbnailPath)

        covers.retainOnly(setOf(b.canonicalPath))

        assertTrue(!capaA.exists())
        assertTrue(capaB.exists())
    }

    @Test
    fun `capa escolhida a mao substitui a gerada`() {
        val cbz = buildCbz("hq.cbz", listOf("001.png" to pngBytes(400, 600, Color.RED)))
        covers.extractCover(cbz.canonicalPath, cbz)

        val escolhida = File(comics, "minha-capa.png").apply {
            writeBytes(pngBytes(250, 250, Color.GREEN))
        }
        val cover = covers.createCoverFromImageFile(cbz.canonicalPath, escolhida)!!
        val imagem = ImageIO.read(File(cover.thumbnailPath))

        assertEquals(250, imagem.width)
        assertEquals(250, imagem.height)
    }
}
