package com.jotape.inkshelf.data.reader

import com.github.junrar.Archive
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer

/**
 * Lê as páginas de um arquivo de quadrinho e as entrega como arquivos de imagem em cache.
 *
 * Formatos: CBZ/ZIP (`java.util.zip`), CBR/RAR4 ([junrar][Archive]), RAR5 (7-Zip-JBinding) e PDF.
 *
 * Nota de porte: o PDF é a única troca real de tecnologia. O Android renderizava com
 * `android.graphics.pdf.PdfRenderer`, que não existe fora dele; aqui quem faz isso é o PDFBox,
 * produzindo um `BufferedImage` que é gravado como JPEG. Os outros três formatos usam bibliotecas
 * Java puras — as mesmas do mobile, sem alteração de lógica.
 */
class PageExtractor(
    // Tamanho alvo de renderização de PDF. No Android vinha das dimensões da tela via `Context`;
    // aqui :core não conhece janela nenhuma, então a UI informa o tamanho ao chamar. O padrão
    // cobre uma tela grande, para o caso de ninguém informar.
    private val defaultPageWidth: Int = 1920,
    private val defaultPageHeight: Int = 1080,
) {

    companion object {
        const val THUMBNAIL_WIDTH = 160
        const val THUMBNAIL_HEIGHT = 240

        // Muda quando a forma de renderizar PDF muda, invalidando o que foi cacheado pela
        // versão anterior (o número entra no nome do arquivo).
        private const val PDF_RENDER_CACHE_VERSION = 5

        // PDFs medem a página em pontos (72dpi). Renderizar no tamanho natural deixa a página
        // borrada ao ampliar em telas de alta densidade, daí o teto de 4x.
        private const val MAX_PDF_SCALE = 4f
        private const val MIN_PDF_SCALE = 0.5f
    }

    private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    private val cacheStore = ReaderCacheStore()
    private val cacheLocks = ConcurrentHashMap<String, Any>()
    private val pdfSessions = ConcurrentHashMap<String, PdfSession>()

    /** Lista as páginas de [file], reaproveitando o índice em cache quando ele ainda vale. */
    fun getPages(fileId: String, file: File): List<ReaderPage> {
        val format = detectFormat(file) ?: return emptyList()
        val sourceToken = cacheStore.sourceToken(file)
        val cachedIndex = cacheStore.loadPageIndex(fileId, sourceToken)
        val path = file.absolutePath

        return when {
            format == Format.ZIP && cachedIndex is ReaderPageIndex.Zip ->
                buildZipPages(fileId, path, cachedIndex.entries)
            format == Format.RAR && cachedIndex is ReaderPageIndex.Rar ->
                buildRarPages(fileId, path, cachedIndex.entries)
            format == Format.RAR5 && cachedIndex is ReaderPageIndex.Rar5 ->
                buildRar5Pages(fileId, path, cachedIndex.entries)
            format == Format.PDF && cachedIndex is ReaderPageIndex.Pdf ->
                buildPdfPages(fileId, path, cachedIndex.pageCount)
            else -> when (format) {
                Format.ZIP -> getPagesFromZip(fileId, file, sourceToken)
                Format.RAR -> getPagesFromRar(fileId, file, sourceToken)
                Format.RAR5 -> getPagesFromRar5(fileId, file, sourceToken)
                Format.PDF -> getPagesFromPdf(fileId, file, sourceToken)
            }
        }
    }

    /**
     * Extrai (ou renderiza) a página e devolve o arquivo de imagem em cache, ou `null` se não
     * for possível lê-la.
     */
    fun getPageImage(
        page: ReaderPage,
        width: Int = defaultPageWidth,
        height: Int = defaultPageHeight,
    ): File? = when (page) {
        is ReaderPage.ZipPage -> extractZipPage(page)
        is ReaderPage.RarPage -> extractRarPage(page)
        is ReaderPage.Rar5Page -> extractRar5Page(page)
        is ReaderPage.PdfPage -> renderPdfPage(page, width, height)
    }

    fun getThumbnail(page: ReaderPage): File? =
        getPageImage(page, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)

    /** Extrai as páginas vizinhas à atual, para que virar a página não espere disco. */
    fun prefetchAround(
        pages: List<ReaderPage>,
        currentPage: Int,
        radius: Int = 1,
        width: Int = defaultPageWidth,
        height: Int = defaultPageHeight,
    ) {
        if (pages.isEmpty()) return

        val center = currentPage.coerceIn(0, pages.lastIndex)
        val start = (center - radius).coerceAtLeast(0)
        val end = (center + radius).coerceAtMost(pages.lastIndex)

        (start..end)
            .map { pages[it] }
            .sortedBy { abs(it.index - center) }
            .forEach { getPageImage(it, width, height) }
    }

    fun clearPageCache(fileId: String) {
        closePdfSession(fileId)
        cacheStore.clearPageCache(fileId)
    }

    fun clearAllPageCache() {
        closeAllPdfSessions()
        cacheStore.clearAllPageCache()
    }

    // ── Indexação por formato ────────────────────────────────────────────────

    private fun getPagesFromZip(fileId: String, file: File, sourceToken: String?): List<ReaderPage> =
        try {
            ZipFile(file).use { zip ->
                val names = zip.entries().asSequence()
                    .filter { !it.isDirectory && isImage(it.name) }
                    .map { it.name }
                    .sortedWith(naturalOrder)
                    .toList()

                cacheStore.savePageIndex(fileId, sourceToken, ReaderPageIndex.Zip(names))
                buildZipPages(fileId, file.absolutePath, names)
            }
        } catch (_: Exception) {
            emptyList()
        }

    private fun getPagesFromRar(fileId: String, file: File, sourceToken: String?): List<ReaderPage> =
        try {
            Archive(file).use { archive ->
                val names = archive.fileHeaders
                    .filter { !it.isDirectory && isImage(it.fileName) }
                    .map { normalizePath(it.fileName) }
                    .sortedWith(naturalOrder)

                cacheStore.savePageIndex(fileId, sourceToken, ReaderPageIndex.Rar(names))
                buildRarPages(fileId, file.absolutePath, names)
            }
        } catch (_: Exception) {
            emptyList()
        }

    private fun getPagesFromRar5(fileId: String, file: File, sourceToken: String?): List<ReaderPage> {
        val entries = listRar5ImageEntries(file)
        cacheStore.savePageIndex(fileId, sourceToken, ReaderPageIndex.Rar5(entries))
        return buildRar5Pages(fileId, file.absolutePath, entries)
    }

    private fun getPagesFromPdf(fileId: String, file: File, sourceToken: String?): List<ReaderPage> {
        val session = getOrCreatePdfSession(fileId, file) ?: return emptyList()
        val pageCount = synchronized(session.lock) {
            try {
                session.document.numberOfPages
            } catch (_: Exception) {
                null
            }
        } ?: return emptyList()

        cacheStore.savePageIndex(fileId, sourceToken, ReaderPageIndex.Pdf(pageCount))
        return buildPdfPages(fileId, file.absolutePath, pageCount)
    }

    // ── Extração de uma página ───────────────────────────────────────────────

    private fun extractZipPage(page: ReaderPage.ZipPage): File? {
        val outputFile = pageFile(page.fileId, page.index, extensionFor(page.entryName))
        if (outputFile.isUsableCache()) return outputFile

        return withCacheLock(outputFile.absolutePath) {
            if (outputFile.isUsableCache()) return@withCacheLock outputFile

            val success = try {
                ZipFile(File(page.sourcePath)).use { zip ->
                    val entry = zip.getEntry(page.entryName) ?: return@use false
                    zip.getInputStream(entry).use { input -> writeStreamToFile(outputFile, input) }
                }
            } catch (_: Exception) {
                false
            }

            outputFile.takeIf { success && it.isUsableCache() }
        }
    }

    private fun extractRarPage(page: ReaderPage.RarPage): File? {
        val outputFile = pageFile(page.fileId, page.index, extensionFor(page.entryName))
        if (outputFile.isUsableCache()) return outputFile

        return withCacheLock(outputFile.absolutePath) {
            if (outputFile.isUsableCache()) return@withCacheLock outputFile

            val success = try {
                Archive(File(page.sourcePath)).use { archive ->
                    val header = archive.fileHeaders.firstOrNull {
                        !it.isDirectory && normalizePath(it.fileName) == page.entryName
                    } ?: return@use false

                    archive.getInputStream(header).use { input ->
                        writeStreamToFile(outputFile, input)
                    }
                }
            } catch (_: Exception) {
                false
            }

            outputFile.takeIf { success && it.isUsableCache() }
        }
    }

    private fun extractRar5Page(page: ReaderPage.Rar5Page): File? {
        val outputFile = pageFile(page.fileId, page.index, extensionFor(page.entryName))
        if (outputFile.isUsableCache()) return outputFile

        return withCacheLock(outputFile.absolutePath) {
            if (outputFile.isUsableCache()) return@withCacheLock outputFile

            val success = withRar5Archive(File(page.sourcePath)) { archive ->
                extractRar5Entry(archive, page.entryIndex, outputFile)
            } ?: false

            outputFile.takeIf { success && it.isUsableCache() }
        }
    }

    private fun renderPdfPage(page: ReaderPage.PdfPage, width: Int, height: Int): File? {
        val safeWidth = bucketDimension(width)
        val safeHeight = bucketDimension(height)
        val outputFile = pageFile(
            page.fileId,
            "${page.index}_pdfv${PDF_RENDER_CACHE_VERSION}_${safeWidth}x$safeHeight.jpg",
        )
        if (outputFile.isUsableCache()) return outputFile

        return withCacheLock(outputFile.absolutePath) {
            if (outputFile.isUsableCache()) return@withCacheLock outputFile

            val session = getOrCreatePdfSession(page.fileId, File(page.sourcePath))
                ?: return@withCacheLock null

            // O PDDocument não é thread-safe: uma única página sendo renderizada por vez.
            val success = synchronized(session.lock) {
                try {
                    val mediaBox = session.document.getPage(page.index).mediaBox
                    val scale = min(
                        safeWidth / mediaBox.width.coerceAtLeast(1f),
                        safeHeight / mediaBox.height.coerceAtLeast(1f),
                    )
                        .takeIf { it.isFinite() && it > 0f }
                        ?.coerceIn(MIN_PDF_SCALE, MAX_PDF_SCALE)
                        ?: 1f

                    // ImageType.RGB (e não ARGB) porque o destino é JPEG, que não tem canal
                    // alfa: renderizar com transparência deixaria as áreas vazias pretas.
                    val image = session.renderer.renderImage(page.index, scale, ImageType.RGB)

                    writeCacheFile(outputFile) { sink ->
                        // JPEG codifica muito mais rápido que PNG neste tamanho, e a qualidade
                        // 90 é indistinguível a olho para página renderizada.
                        ImageIO.write(image, "jpg", sink)
                    }
                } catch (_: Exception) {
                    false
                }
            }

            outputFile.takeIf { success && it.isUsableCache() }
        }
    }

    // ── Construção das listas de página ──────────────────────────────────────

    private fun buildZipPages(fileId: String, sourcePath: String, entries: List<String>) =
        entries.mapIndexed { index, entryName ->
            ReaderPage.ZipPage(index, fileId, sourcePath, entryName)
        }

    private fun buildRarPages(fileId: String, sourcePath: String, entries: List<String>) =
        entries.mapIndexed { index, entryName ->
            ReaderPage.RarPage(index, fileId, sourcePath, entryName)
        }

    private fun buildRar5Pages(
        fileId: String,
        sourcePath: String,
        entries: List<ReaderPageIndex.Rar5Entry>,
    ) = entries.mapIndexed { pageIndex, entry ->
        ReaderPage.Rar5Page(pageIndex, fileId, sourcePath, entry.index, entry.path)
    }

    private fun buildPdfPages(fileId: String, sourcePath: String, pageCount: Int) =
        (0 until pageCount).map { ReaderPage.PdfPage(it, fileId, sourcePath) }

    // ── Formato ──────────────────────────────────────────────────────────────

    /**
     * Detecta o formato pelos bytes iniciais, não pela extensão: um `.cbr` que na verdade é um
     * ZIP renomeado é comum o bastante para valer a leitura.
     */
    private fun detectFormat(file: File): Format? = try {
        file.inputStream().use { stream ->
            val header = ByteArray(8)
            val read = stream.read(header)
            when {
                read < 4 -> null
                header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() -> Format.ZIP
                header[0] == 0x52.toByte() &&
                    header[1] == 0x61.toByte() &&
                    header[2] == 0x72.toByte() &&
                    header[3] == 0x21.toByte() ->
                    // RAR5 se distingue do RAR4 pelo sétimo byte da assinatura.
                    if (read >= 7 && header[6] == 0x01.toByte()) Format.RAR5 else Format.RAR
                header[0] == 0x25.toByte() &&
                    header[1] == 0x50.toByte() &&
                    header[2] == 0x44.toByte() &&
                    header[3] == 0x46.toByte() -> Format.PDF
                else -> null
            }
        }
    } catch (_: Exception) {
        null
    }

    private enum class Format { ZIP, RAR, RAR5, PDF }

    // ── RAR5 ─────────────────────────────────────────────────────────────────

    private fun listRar5ImageEntries(file: File): List<ReaderPageIndex.Rar5Entry> =
        withRar5Archive(file) { archive ->
            (0 until archive.numberOfItems)
                .mapNotNull { index ->
                    val path = archive.getStringProperty(index, PropID.PATH)
                        ?.replace('\\', '/')
                        ?: return@mapNotNull null
                    val isFolder = archive.getProperty(index, PropID.IS_FOLDER) as? Boolean ?: false
                    if (!isFolder && isImage(path)) {
                        ReaderPageIndex.Rar5Entry(index = index, path = path)
                    } else {
                        null
                    }
                }
                .sortedWith { a, b -> naturalOrder.compare(a.path, b.path) }
        }.orEmpty()

    private fun extractRar5Entry(archive: IInArchive, entryIndex: Int, outputFile: File): Boolean =
        try {
            writeCacheFile(outputFile) { sink ->
                val result = archive.extractSlow(entryIndex, ISequentialOutStream { data ->
                    sink.write(data)
                    data.size
                })
                if (result != ExtractOperationResult.OK) {
                    error("Falha ao extrair entrada RAR5: $result")
                }
            }
        } catch (_: Exception) {
            outputFile.delete()
            false
        }

    private fun <T> withRar5Archive(file: File, block: (IInArchive) -> T): T? {
        var archive: IInArchive? = null
        var randomAccessFile: RandomAccessFile? = null

        return try {
            randomAccessFile = RandomAccessFile(file, "r")
            archive = SevenZip.openInArchive(null, RandomAccessFileInStream(randomAccessFile))
            block(archive)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { archive?.close() }
            runCatching { randomAccessFile?.close() }
        }
    }

    // ── Sessão de PDF ────────────────────────────────────────────────────────

    /**
     * Mantém o PDF aberto entre páginas: abrir e fechar o documento a cada página torna a
     * navegação visivelmente lenta em arquivos grandes.
     */
    private fun getOrCreatePdfSession(fileId: String, file: File): PdfSession? =
        withCacheLock("pdf:$fileId") {
            val sourceToken = cacheStore.sourceToken(file)
            val existing = pdfSessions[fileId]
            if (existing != null &&
                existing.path == file.absolutePath &&
                existing.sourceToken == sourceToken
            ) {
                return@withCacheLock existing
            }

            existing?.let { closeSession(it) }
            pdfSessions.remove(fileId)

            val document = try {
                Loader.loadPDF(file)
            } catch (_: Exception) {
                null
            } ?: return@withCacheLock null

            PdfSession(
                path = file.absolutePath,
                sourceToken = sourceToken,
                document = document,
                renderer = PDFRenderer(document),
            ).also { pdfSessions[fileId] = it }
        }

    private fun closePdfSession(fileId: String) {
        pdfSessions.remove(fileId)?.let { closeSession(it) }
    }

    private fun closeAllPdfSessions() {
        val sessions = pdfSessions.values.toList()
        pdfSessions.clear()
        sessions.forEach { closeSession(it) }
    }

    private fun closeSession(session: PdfSession) {
        synchronized(session.lock) { runCatching { session.document.close() } }
    }

    private class PdfSession(
        val path: String,
        val sourceToken: String?,
        val document: PDDocument,
        val renderer: PDFRenderer,
        val lock: Any = Any(),
    )

    // ── Utilidades ───────────────────────────────────────────────────────────

    private fun writeStreamToFile(outputFile: File, input: InputStream): Boolean =
        writeCacheFile(outputFile) { sink -> input.copyTo(sink) }

    private fun pageFile(fileId: String, index: Int, extension: String): File =
        cacheStore.pageFile(fileId, "$index.$extension")

    private fun pageFile(fileId: String, name: String): File =
        cacheStore.pageFile(fileId, name)

    private fun extensionFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return if (ext in supportedImageExtensions) ext else "jpg"
    }

    private fun isImage(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in supportedImageExtensions

    private fun normalizePath(path: String): String = path.replace('\\', '/')

    /**
     * Arredonda a dimensão para o múltiplo de 128 acima. Sem isso, cada variação de alguns pixels
     * no tamanho da janela geraria uma renderização e uma entrada de cache novas.
     */
    private fun bucketDimension(value: Int): Int {
        val clamped = value.coerceAtLeast(1)
        return ((clamped + 127) / 128) * 128
    }

    private inline fun <T> withCacheLock(key: String, block: () -> T): T {
        val lock = cacheLocks.getOrPut(key) { Any() }
        return synchronized(lock) { block() }
    }

    /**
     * Ordena "pagina2" antes de "pagina10" comparando os trechos numéricos como números. A ordem
     * alfabética pura embaralharia a leitura de qualquer arquivo com mais de 9 páginas.
     */
    private val naturalOrder = Comparator<String> { a, b ->
        val partsA = splitNatural(normalizePath(a).lowercase())
        val partsB = splitNatural(normalizePath(b).lowercase())

        val len = minOf(partsA.size, partsB.size)
        for (i in 0 until len) {
            val (numericA, valueA) = partsA[i]
            val (numericB, valueB) = partsB[i]
            val cmp = if (numericA && numericB) {
                valueA.toLong().compareTo(valueB.toLong())
            } else {
                valueA.compareTo(valueB)
            }
            if (cmp != 0) return@Comparator cmp
        }
        partsA.size.compareTo(partsB.size)
    }

    private fun splitNatural(value: String): List<Pair<Boolean, String>> {
        val result = mutableListOf<Pair<Boolean, String>>()
        val current = StringBuilder()
        var isNumber = false

        for (char in value) {
            val currentIsNumber = char.isDigit()
            if (current.isNotEmpty() && currentIsNumber != isNumber) {
                result.add(isNumber to current.toString())
                current.clear()
            }
            isNumber = currentIsNumber
            current.append(char)
        }

        if (current.isNotEmpty()) {
            result.add(isNumber to current.toString())
        }

        return result
    }
}
