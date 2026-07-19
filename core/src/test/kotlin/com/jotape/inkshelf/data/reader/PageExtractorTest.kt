package com.jotape.inkshelf.data.reader

import com.jotape.inkshelf.data.InkPaths
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Testes contra arquivos de verdade, montados aqui mesmo: um CBZ é um ZIP com imagens e um PDF é
 * gerado pelo PDFBox. Nada de mock — o que se quer provar é justamente que as bibliotecas leem o
 * formato, então simular a leitura não provaria nada.
 */
class PageExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var extractor: PageExtractor
    private lateinit var comics: File

    @Before
    fun setUp() {
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        extractor = PageExtractor()
        comics = tempFolder.newFolder("Quadrinhos")
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private fun pngBytes(color: Color): ByteArray {
        val image = BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            paint = color
            fillRect(0, 0, 40, 60)
            dispose()
        }
        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    /** Monta um ZIP com as entradas dadas (nome → conteúdo). */
    private fun buildZip(name: String, entries: List<String>): File {
        val file = File(comics, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { entryName ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(if (isImageName(entryName)) pngBytes(Color.RED) else "texto".toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private fun isImageName(name: String) =
        name.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg")

    private fun buildPdf(name: String, pageCount: Int): File {
        val file = File(comics, name)
        PDDocument().use { doc ->
            repeat(pageCount) { doc.addPage(PDPage()) }
            doc.save(file)
        }
        return file
    }

    // ── CBZ ──────────────────────────────────────────────────────────────────

    @Test
    fun `cbz lista as paginas de imagem`() {
        val cbz = buildZip("hq.cbz", listOf("001.png", "002.png", "003.png"))

        val pages = extractor.getPages(cbz.canonicalPath, cbz)

        assertEquals(3, pages.size)
        assertTrue(pages.all { it is ReaderPage.ZipPage })
        assertEquals(listOf(0, 1, 2), pages.map { it.index })
    }

    @Test
    fun `cbz ignora entradas que nao sao imagem`() {
        val cbz = buildZip("hq.cbz", listOf("001.png", "ComicInfo.xml", "leiame.txt", "002.png"))

        assertEquals(2, extractor.getPages(cbz.canonicalPath, cbz).size)
    }

    @Test
    fun `paginas seguem ordem natural e nao alfabetica`() {
        // Em ordem alfabética "10" viria antes de "2" e a leitura sairia embaralhada.
        val cbz = buildZip("hq.cbz", listOf("pagina10.png", "pagina2.png", "pagina1.png"))

        val nomes = extractor.getPages(cbz.canonicalPath, cbz)
            .map { (it as ReaderPage.ZipPage).entryName }

        assertEquals(listOf("pagina1.png", "pagina2.png", "pagina10.png"), nomes)
    }

    @Test
    fun `extrai a imagem da pagina para o cache`() {
        val cbz = buildZip("hq.cbz", listOf("001.png", "002.png"))
        val pages = extractor.getPages(cbz.canonicalPath, cbz)

        val imagem = extractor.getPageImage(pages[1])

        assertNotNull("a página deveria ter sido extraída", imagem)
        assertTrue(imagem!!.length() > 0)
        // O que saiu do cache tem que ser uma imagem decodificável de verdade.
        val decodificada = ImageIO.read(imagem)
        assertNotNull(decodificada)
        assertEquals(40, decodificada.width)
        assertEquals(60, decodificada.height)
    }

    @Test
    fun `segunda extracao reaproveita o arquivo em cache`() {
        val cbz = buildZip("hq.cbz", listOf("001.png"))
        val pages = extractor.getPages(cbz.canonicalPath, cbz)

        val primeira = extractor.getPageImage(pages[0])!!
        val marca = primeira.lastModified()
        val segunda = extractor.getPageImage(pages[0])!!

        assertEquals(primeira.absolutePath, segunda.absolutePath)
        assertEquals("o cache foi reescrito em vez de reaproveitado", marca, segunda.lastModified())
    }

    @Test
    fun `formato vem dos bytes e nao da extensao`() {
        // Um ZIP renomeado para .cbr é comum o bastante para não podermos confiar na extensão.
        val disfarcado = buildZip("na-verdade-zip.cbr", listOf("001.png", "002.png"))

        val pages = extractor.getPages(disfarcado.canonicalPath, disfarcado)

        assertEquals(2, pages.size)
        assertTrue("deveria ter sido lido como ZIP", pages.all { it is ReaderPage.ZipPage })
    }

    @Test
    fun `arquivo ilegivel devolve lista vazia em vez de estourar`() {
        val lixo = File(comics, "quebrado.cbz").apply { writeText("nao sou um arquivo valido") }

        assertEquals(emptyList<ReaderPage>(), extractor.getPages(lixo.canonicalPath, lixo))
    }

    @Test
    fun `cbz sem nenhuma imagem devolve lista vazia`() {
        val cbz = buildZip("vazio.cbz", listOf("leiame.txt"))

        assertEquals(emptyList<ReaderPage>(), extractor.getPages(cbz.canonicalPath, cbz))
    }

    // ── PDF ──────────────────────────────────────────────────────────────────

    @Test
    fun `pdf lista uma pagina por pagina do documento`() {
        val pdf = buildPdf("livro.pdf", pageCount = 5)

        val pages = extractor.getPages(pdf.canonicalPath, pdf)

        assertEquals(5, pages.size)
        assertTrue(pages.all { it is ReaderPage.PdfPage })
    }

    @Test
    fun `pdf renderiza a pagina como imagem`() {
        val pdf = buildPdf("livro.pdf", pageCount = 2)
        val pages = extractor.getPages(pdf.canonicalPath, pdf)

        val imagem = extractor.getPageImage(pages[0], width = 400, height = 600)

        assertNotNull("a página do PDF deveria ter sido renderizada", imagem)
        val decodificada = ImageIO.read(imagem)
        assertNotNull("o arquivo gerado não é uma imagem válida", decodificada)
        assertTrue(decodificada.width > 0 && decodificada.height > 0)
    }

    @Test
    fun `miniatura de pdf sai menor que a pagina cheia`() {
        val pdf = buildPdf("livro.pdf", pageCount = 1)
        val pages = extractor.getPages(pdf.canonicalPath, pdf)

        val miniatura = ImageIO.read(extractor.getThumbnail(pages[0]))
        val cheia = ImageIO.read(extractor.getPageImage(pages[0], width = 1200, height = 1600))

        assertTrue(miniatura.width < cheia.width)
    }

    // ── Índice em cache ──────────────────────────────────────────────────────

    @Test
    fun `indice em cache e reaproveitado entre aberturas`() {
        val cbz = buildZip("hq.cbz", listOf("001.png", "002.png"))
        val id = cbz.canonicalPath
        extractor.getPages(id, cbz)

        // Um extractor novo (como numa reabertura do app) enxerga o mesmo índice em disco.
        val pages = PageExtractor().getPages(id, cbz)

        assertEquals(2, pages.size)
    }

    @Test
    fun `arquivo alterado no disco invalida o indice`() {
        val cbz = buildZip("hq.cbz", listOf("001.png", "002.png"))
        val id = cbz.canonicalPath
        assertEquals(2, extractor.getPages(id, cbz).size)

        // Regravado com mais páginas: o token de origem muda e o índice antigo é descartado.
        buildZip("hq.cbz", listOf("001.png", "002.png", "003.png"))
        cbz.setLastModified(System.currentTimeMillis() + 5_000)

        assertEquals(3, PageExtractor().getPages(id, cbz).size)
    }

    @Test
    fun `limpar cache de um arquivo nao afeta os outros`() {
        val a = buildZip("a.cbz", listOf("001.png"))
        val b = buildZip("b.cbz", listOf("001.png"))
        val paginaA = extractor.getPages(a.canonicalPath, a)[0]
        val paginaB = extractor.getPages(b.canonicalPath, b)[0]
        val arquivoA = extractor.getPageImage(paginaA)!!
        val arquivoB = extractor.getPageImage(paginaB)!!

        extractor.clearPageCache(a.canonicalPath)

        assertTrue("o cache de A deveria ter sumido", !arquivoA.exists())
        assertTrue("o cache de B não deveria ter sido tocado", arquivoB.exists())
    }

    @Test
    fun `retainOnly descarta o cache de arquivos que sairam da biblioteca`() {
        val a = buildZip("a.cbz", listOf("001.png"))
        val b = buildZip("b.cbz", listOf("001.png"))
        val arquivoA = extractor.getPageImage(extractor.getPages(a.canonicalPath, a)[0])!!
        val arquivoB = extractor.getPageImage(extractor.getPages(b.canonicalPath, b)[0])!!

        ReaderCacheStore().retainOnly(setOf(b.canonicalPath))

        assertTrue(!arquivoA.exists())
        assertTrue(arquivoB.exists())
    }

    @Test
    fun `pagina de arquivo que sumiu do disco devolve null`() {
        val cbz = buildZip("hq.cbz", listOf("001.png"))
        val page = extractor.getPages(cbz.canonicalPath, cbz)[0]
        extractor.clearPageCache(cbz.canonicalPath)
        cbz.delete()

        assertNull(extractor.getPageImage(page))
    }
}
