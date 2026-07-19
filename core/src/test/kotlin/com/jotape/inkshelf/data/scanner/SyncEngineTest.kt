package com.jotape.inkshelf.data.scanner

import com.jotape.inkshelf.data.db.InkShelfDatabase
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * O contrato que estes testes protegem: **uma nova varredura nunca pode destruir estado do
 * usuário**. Favoritos, progresso de leitura e metadados vivem só no banco — se o diff do
 * [SyncEngine] sobrescrever em vez de mesclar, o usuário perde a biblioteca inteira em silêncio
 * e só descobre depois. É a regressão mais cara possível neste módulo, e a mais fácil de
 * introduzir sem perceber.
 */
class SyncEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: InkShelfDatabase
    private lateinit var library: File

    @Before
    fun setUp() {
        db = InkShelfDatabase.build(tempFolder.newFile("test.db").absolutePath)
        library = tempFolder.newFolder("Biblioteca")
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun syncEngine() = SyncEngine(db)

    /** Cria `Biblioteca/<caminho>` com conteúdo qualquer. */
    private fun createFile(path: String): File =
        File(library, path).apply {
            parentFile.mkdirs()
            writeText("conteudo")
        }

    private fun folderId(path: String) = File(library, path).canonicalPath

    private fun fileId(path: String) = File(library, path).canonicalPath

    @Test
    fun `varredura inicial indexa pastas e arquivos`() = runTest {
        createFile("Batman/batman-001.cbz")
        createFile("Batman/batman-002.cbr")
        createFile("Superman/superman-001.pdf")

        syncEngine().initialScan(library)

        val folders = db.folderDao().getAllFoldersOnce()
        val files = db.fileDao().getAllFilesOnce()

        assertEquals(setOf("Batman", "Superman"), folders.map { it.name }.toSet())
        assertEquals(3, files.size)
        // Os filhos diretos da raiz escolhida pendem do nó virtual "root", que não é linha.
        assertTrue(folders.all { it.parentId == LibraryScanner.ROOT_ID })
    }

    @Test
    fun `arquivo com extensao nao suportada e ignorado`() = runTest {
        createFile("Batman/batman-001.cbz")
        createFile("Batman/leiame.txt")
        createFile("Batman/capa.jpg")

        syncEngine().initialScan(library)

        assertEquals(listOf("batman-001.cbz"), db.fileDao().getAllFilesOnce().map { it.name })
    }

    @Test
    fun `pasta sem arquivos suportados na arvore e descartada`() = runTest {
        createFile("Batman/batman-001.cbz")
        File(library, "Vazia/Subvazia").mkdirs()
        createFile("SoTexto/notas.txt")

        syncEngine().initialScan(library)

        assertEquals(setOf("Batman"), db.folderDao().getAllFoldersOnce().map { it.name }.toSet())
    }

    @Test
    fun `pasta intermediaria sem arquivos proprios e mantida se a subarvore tem arquivos`() =
        runTest {
            createFile("DC/Batman/batman-001.cbz")

            syncEngine().initialScan(library)

            assertEquals(
                setOf("DC", "Batman"),
                db.folderDao().getAllFoldersOnce().map { it.name }.toSet(),
            )
        }

    @Test
    fun `rescan preserva favorito progresso e leitura do usuario`() = runTest {
        createFile("Batman/batman-001.cbz")
        val engine = syncEngine()
        engine.initialScan(library)

        val id = fileId("Batman/batman-001.cbz")
        db.fileDao().updateProgress(id, 42)
        db.fileDao().updatePages(id, 100)
        db.fileDao().setFavorite(id, true)
        db.fileDao().updateCoverPath(id, "C:/cache/capa.jpg")

        // Um arquivo novo força o diff a reescrever o lote — é justamente aí que uma
        // sobrescrita indevida apagaria o progresso do arquivo antigo.
        createFile("Batman/batman-002.cbz")
        engine.rescan(library)

        val file = db.fileDao().getFileById(id)
        assertNotNull(file)
        assertEquals(42, file!!.currentPage)
        assertEquals(100, file.pages)
        assertTrue(file.isFavorite)
        assertEquals("C:/cache/capa.jpg", file.coverPath)
        assertEquals(2, db.fileDao().getAllFilesOnce().size)
    }

    @Test
    fun `rescan preserva metadados de comicinfo`() = runTest {
        createFile("Batman/batman-001.cbz")
        val engine = syncEngine()
        engine.initialScan(library)

        val id = fileId("Batman/batman-001.cbz")
        db.fileDao().updateComicInfo(
            id = id,
            title = "O Cavaleiro das Trevas",
            series = "Batman",
            number = "1",
            writer = "Frank Miller",
            penciller = null,
            artist = null,
            colorist = null,
            publisher = "DC",
            year = 1986,
            summary = null,
            scanned = true,
        )

        engine.rescan(library)

        val file = db.fileDao().getFileById(id)!!
        assertEquals("O Cavaleiro das Trevas", file.comicInfoTitle)
        assertEquals("Frank Miller", file.comicInfoWriter)
        assertEquals(1986, file.comicInfoYear)
        assertTrue(file.comicInfoScanned)
    }

    @Test
    fun `rescan preserva favorito e capa customizada da pasta`() = runTest {
        createFile("Batman/batman-001.cbz")
        val engine = syncEngine()
        engine.initialScan(library)

        val id = folderId("Batman")
        db.folderDao().setFavorite(id, true)
        db.folderDao().setCustomCover(id, "cover-123")
        db.folderDao().updateCoverPath(id, "C:/cache/pasta.jpg")

        createFile("Batman/batman-002.cbz")
        engine.rescan(library)

        val folder = db.folderDao().getFolderById(id)!!
        assertTrue(folder.isFavorite)
        assertEquals("cover-123", folder.customCoverId)
        assertEquals("C:/cache/pasta.jpg", folder.coverPath)
    }

    @Test
    fun `rescan remove arquivo apagado do disco`() = runTest {
        createFile("Batman/batman-001.cbz")
        val removido = createFile("Batman/batman-002.cbz")
        val engine = syncEngine()
        engine.initialScan(library)
        assertEquals(2, db.fileDao().getAllFilesOnce().size)

        removido.delete()
        engine.rescan(library)

        assertEquals(listOf("batman-001.cbz"), db.fileDao().getAllFilesOnce().map { it.name })
    }

    @Test
    fun `rescan remove pasta apagada junto com seus arquivos`() = runTest {
        createFile("Batman/batman-001.cbz")
        createFile("Superman/superman-001.cbz")
        val engine = syncEngine()
        engine.initialScan(library)

        File(library, "Superman").deleteRecursively()
        engine.rescan(library)

        assertEquals(setOf("Batman"), db.folderDao().getAllFoldersOnce().map { it.name }.toSet())
        assertEquals(listOf("batman-001.cbz"), db.fileDao().getAllFilesOnce().map { it.name })
        assertNull(db.fileDao().getFileById(fileId("Superman/superman-001.cbz")))
    }

    @Test
    fun `itemCount agrega a subarvore inteira`() = runTest {
        createFile("DC/Batman/batman-001.cbz")
        createFile("DC/Batman/batman-002.cbz")
        createFile("DC/Superman/superman-001.cbz")
        createFile("DC/dc-especial.cbz")

        syncEngine().initialScan(library)

        val counts = db.folderDao().getAllFoldersOnce().associate { it.name to it.itemCount }
        assertEquals(2, counts["Batman"])
        assertEquals(1, counts["Superman"])
        // 1 arquivo próprio + 2 de Batman + 1 de Superman.
        assertEquals(4, counts["DC"])
    }

    @Test
    fun `itemCount e recalculado quando arquivos somem`() = runTest {
        createFile("Batman/batman-001.cbz")
        val removido = createFile("Batman/batman-002.cbz")
        val engine = syncEngine()
        engine.initialScan(library)
        assertEquals(2, db.folderDao().getFolderById(folderId("Batman"))!!.itemCount)

        removido.delete()
        engine.rescan(library)

        assertEquals(1, db.folderDao().getFolderById(folderId("Batman"))!!.itemCount)
    }

    @Test
    fun `rescan de multiplas raizes convive numa biblioteca so`() = runTest {
        createFile("Batman/batman-001.cbz")
        val outraRaiz = tempFolder.newFolder("Mangas")
        File(outraRaiz, "Naruto").mkdirs()
        File(outraRaiz, "Naruto/naruto-001.cbz").writeText("conteudo")

        syncEngine().rescan(listOf(library, outraRaiz))

        assertEquals(
            setOf("Batman", "Naruto"),
            db.folderDao().getAllFoldersOnce().map { it.name }.toSet(),
        )
        assertEquals(2, db.fileDao().getAllFilesOnce().size)
    }

    @Test
    fun `rescan sem mudanca no disco nao altera nada`() = runTest {
        createFile("Batman/batman-001.cbz")
        val engine = syncEngine()
        engine.initialScan(library)
        val antes = db.fileDao().getAllFilesOnce()

        engine.rescan(library)

        assertEquals(antes, db.fileDao().getAllFilesOnce())
    }
}
