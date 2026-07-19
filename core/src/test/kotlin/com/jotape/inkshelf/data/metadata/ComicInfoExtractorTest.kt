package com.jotape.inkshelf.data.metadata

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ComicInfoExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val extractor = ComicInfoExtractor()
    private lateinit var comics: File

    @Before
    fun setUp() {
        comics = tempFolder.newFolder("Quadrinhos")
    }

    private fun buildCbz(name: String, entries: Map<String, String>): File {
        val file = File(comics, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (entryName, content) ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private val comicInfoXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <ComicInfo>
            <Title>O Cavaleiro das Trevas</Title>
            <Series>Batman</Series>
            <Number>1</Number>
            <Writer>Frank Miller</Writer>
            <Penciller>Frank Miller</Penciller>
            <Colorist>Lynn Varley</Colorist>
            <Publisher>DC Comics</Publisher>
            <Year>1986</Year>
            <Summary>Bruce Wayne volta a vestir o manto.</Summary>
        </ComicInfo>
    """.trimIndent()

    @Test
    fun `le os metadados do comicinfo`() {
        val cbz = buildCbz("hq.cbz", mapOf("ComicInfo.xml" to comicInfoXml, "001.png" to "img"))

        val meta = extractor.extract(cbz)!!

        assertEquals("O Cavaleiro das Trevas", meta.title)
        assertEquals("Batman", meta.series)
        assertEquals("1", meta.number)
        assertEquals("Frank Miller", meta.writer)
        assertEquals("Lynn Varley", meta.colorist)
        assertEquals("DC Comics", meta.publisher)
        assertEquals(1986, meta.year)
    }

    @Test
    fun `encontra o arquivo em subpasta e com outra caixa`() {
        val cbz = buildCbz("hq.cbz", mapOf("meta/comicinfo.XML" to comicInfoXml))

        assertEquals("Batman", extractor.extract(cbz)?.series)
    }

    @Test
    fun `arquivo sem comicinfo devolve null`() {
        val cbz = buildCbz("hq.cbz", mapOf("001.png" to "img"))

        assertNull(extractor.extract(cbz))
    }

    @Test
    fun `comicinfo vazio devolve null em vez de metadados em branco`() {
        val cbz = buildCbz("hq.cbz", mapOf("ComicInfo.xml" to "<ComicInfo></ComicInfo>"))

        assertNull(extractor.extract(cbz))
    }

    @Test
    fun `xml malformado devolve null em vez de estourar`() {
        val cbz = buildCbz("hq.cbz", mapOf("ComicInfo.xml" to "<ComicInfo><Title>sem fechar"))

        assertNull(extractor.extract(cbz))
    }

    @Test
    fun `campos em branco sao ignorados`() {
        val xml = "<ComicInfo><Title>   </Title><Series>Batman</Series></ComicInfo>"
        val cbz = buildCbz("hq.cbz", mapOf("ComicInfo.xml" to xml))

        val meta = extractor.extract(cbz)!!

        assertNull(meta.title)
        assertEquals("Batman", meta.series)
    }

    @Test
    fun `ano invalido nao vira zero`() {
        val xml = "<ComicInfo><Series>Batman</Series><Year>mil novecentos</Year></ComicInfo>"
        val cbz = buildCbz("hq.cbz", mapOf("ComicInfo.xml" to xml))

        assertNull(extractor.extract(cbz)!!.year)
    }

    @Test
    fun `xml com entidade externa nao le arquivos da maquina`() {
        // Um .cbz vem de fonte desconhecida; um XML com DOCTYPE não pode virar leitura de disco.
        val segredo = File(comics, "segredo.txt").apply { writeText("conteudo secreto") }
        val ataque = """
            <?xml version="1.0"?>
            <!DOCTYPE ComicInfo [<!ENTITY xxe SYSTEM "file:///${segredo.absolutePath.replace('\\', '/')}">]>
            <ComicInfo><Series>&xxe;</Series></ComicInfo>
        """.trimIndent()
        val cbz = buildCbz("hq.cbz", mapOf("ComicInfo.xml" to ataque))

        // O parser rejeita o DOCTYPE por inteiro, então não sobra metadado nenhum.
        assertNull(extractor.extract(cbz))
    }

    @Test
    fun `pdf nao tem comicinfo`() {
        val pdf = File(comics, "livro.pdf").apply { writeBytes("%PDF-1.4 resto".toByteArray()) }

        assertNull(extractor.extract(pdf))
    }
}
