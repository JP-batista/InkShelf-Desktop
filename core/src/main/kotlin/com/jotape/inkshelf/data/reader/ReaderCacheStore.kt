package com.jotape.inkshelf.data.reader

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.scanner.PrunableCache
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache em disco das páginas já extraídas, em `%LOCALAPPDATA%\InkShelf\cache`.
 *
 * Duas diferenças em relação ao mobile:
 *
 * 1. **Não existe mais cache de arquivo compactado.** No Android o `.cbz` inteiro era copiado
 *    para o cache antes de ser lido, porque um `Uri` do SAF não permite acesso aleatório. Aqui o
 *    original é lido direto de onde está — o que economiza uma cópia integral por arquivo aberto.
 * 2. **O nome do diretório de cache é um hash do `fileId`.** O `fileId` agora é o caminho
 *    canônico do arquivo, que contém `\` e `:` e não serve como nome de pasta. O hash também
 *    mantém o caminho do cache curto, longe do limite de 260 caracteres do Windows.
 */
class ReaderCacheStore : PrunableCache {

    val pageCacheDir: File
        get() = File(InkPaths.cacheDir, "pages").ensureDir()

    private val indexDir: File
        get() = File(InkPaths.cacheDir, "page_index").ensureDir()

    fun pageFile(fileId: String, name: String): File =
        File(File(pageCacheDir, cacheKey(fileId)).ensureDir(), name)

    /**
     * Impressão digital do arquivo de origem. Se ele mudar no disco, o token muda e o índice de
     * páginas cacheado é descartado em vez de descrever um conteúdo que não existe mais.
     */
    fun sourceToken(source: File): String? =
        if (source.exists()) "${source.lastModified()}:${source.length()}" else null

    fun savePageIndex(fileId: String, sourceToken: String?, index: ReaderPageIndex) {
        val file = indexFile(fileId)
        withLock(file.absolutePath) {
            writeTextSafely(file, index.serialize(sourceToken))
        }
    }

    fun loadPageIndex(fileId: String, sourceToken: String?): ReaderPageIndex? {
        val file = indexFile(fileId)
        if (!file.exists()) return null

        return withLock(file.absolutePath) {
            try {
                ReaderPageIndex.deserialize(file.readLines(), sourceToken)
            } catch (e: Exception) {
                // Índice corrompido (escrita interrompida, disco cheio): apagar e reindexar é
                // sempre seguro — é cache derivado, o arquivo original continua intacto.
                file.delete()
                null
            }
        }
    }

    fun clearPageCache(fileId: String) {
        File(pageCacheDir, cacheKey(fileId)).deleteRecursively()
        indexFile(fileId).delete()
    }

    fun clearAllPageCache() {
        pageCacheDir.deleteRecursively()
        indexDir.deleteRecursively()
        pageCacheDir.ensureDir()
        indexDir.ensureDir()
    }

    fun totalCacheSize(): Long =
        pageCacheDir.listFiles().orEmpty().sumOf { it.recursiveSize() }

    /**
     * Reduz o cache até [limitBytes], descartando primeiro o que foi usado há mais tempo.
     * [excludeFileId] protege o arquivo aberto no momento — despejá-lo faria o leitor
     * reextrair a página que acabou de mostrar.
     */
    fun evictToLimit(limitBytes: Long, excludeFileId: String? = null) {
        if (limitBytes <= 0) return
        var currentSize = totalCacheSize()
        if (currentSize <= limitBytes) return

        val excludeKey = excludeFileId?.let { cacheKey(it) }
        val byLastUse = pageCacheDir.listFiles().orEmpty()
            .filter { it.isDirectory && it.name != excludeKey }
            .map { dir ->
                val latest = dir.listFiles().orEmpty().maxOfOrNull { it.lastModified() }
                    ?: dir.lastModified()
                dir to latest
            }
            .sortedBy { it.second }

        for ((dir, _) in byLastUse) {
            if (currentSize <= limitBytes) break
            currentSize -= dir.recursiveSize()
            dir.deleteRecursively()
            File(indexDir, "${dir.name}.index").delete()
        }
    }

    /** Descarta o cache de arquivos que saíram da biblioteca. */
    override fun retainOnly(validFileIds: Set<String>) {
        val validKeys = validFileIds.mapTo(HashSet()) { cacheKey(it) }
        pageCacheDir.listFiles().orEmpty().forEach { dir ->
            if (dir.name !in validKeys) dir.deleteRecursively()
        }
        indexDir.listFiles().orEmpty().forEach { file ->
            if (file.nameWithoutExtension !in validKeys) file.delete()
        }
    }

    private fun indexFile(fileId: String): File = File(indexDir, "${cacheKey(fileId)}.index")

    private fun writeTextSafely(outputFile: File, content: String): Boolean =
        writeToFile(outputFile) { sink -> sink.write(content.toByteArray(Charsets.UTF_8)) }

    private fun File.recursiveSize(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles().orEmpty().sumOf { it.recursiveSize() }
    }

    private fun File.ensureDir(): File = apply { mkdirs() }

    private inline fun <T> withLock(key: String, block: () -> T): T {
        val lock = cacheLocks.getOrPut(key) { Any() }
        return synchronized(lock) { block() }
    }

    companion object {
        private val cacheLocks = ConcurrentHashMap<String, Any>()

        /** Nome de diretório derivado do `fileId`, seguro em qualquer sistema de arquivos. */
        fun cacheKey(fileId: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(fileId.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(32)
        }
    }
}

/**
 * Índice das páginas de um arquivo, persistido para que reabrir não custe uma releitura completa
 * da lista de entradas (num CBZ com centenas de páginas isso é perceptível).
 */
sealed class ReaderPageIndex {

    data class Zip(val entries: List<String>) : ReaderPageIndex()

    data class Rar(val entries: List<String>) : ReaderPageIndex()

    data class Rar5(val entries: List<Rar5Entry>) : ReaderPageIndex()

    data class Pdf(val pageCount: Int) : ReaderPageIndex()

    data class Rar5Entry(val index: Int, val path: String)

    fun serialize(sourceToken: String?): String = buildString {
        append("token=")
        append(sourceToken.orEmpty())
        appendLine()

        when (this@ReaderPageIndex) {
            is Zip -> {
                appendLine("format=ZIP")
                entries.forEach { appendLine(encodeLine(it)) }
            }
            is Rar -> {
                appendLine("format=RAR")
                entries.forEach { appendLine(encodeLine(it)) }
            }
            is Rar5 -> {
                appendLine("format=RAR5")
                entries.forEach { entry ->
                    append(entry.index)
                    append('\t')
                    appendLine(encodeLine(entry.path))
                }
            }
            is Pdf -> {
                appendLine("format=PDF")
                append("count=")
                append(pageCount)
                appendLine()
            }
        }
    }

    companion object {
        fun deserialize(lines: List<String>, sourceToken: String?): ReaderPageIndex? {
            if (lines.size < 2) return null

            val storedToken = lines[0].substringAfter("token=", missingDelimiterValue = "")
            if (sourceToken != null && storedToken != sourceToken) return null

            val format = lines[1].substringAfter("format=", missingDelimiterValue = "")
            val payload = lines.drop(2).filter { it.isNotBlank() }

            return when (format) {
                "ZIP" -> Zip(payload.map(::decodeLine))
                "RAR" -> Rar(payload.map(::decodeLine))
                "RAR5" -> {
                    val entries = payload.mapNotNull { line ->
                        val parts = line.split('\t', limit = 2)
                        val index = parts.firstOrNull()?.toIntOrNull() ?: return@mapNotNull null
                        val path = parts.getOrNull(1)?.let(::decodeLine) ?: return@mapNotNull null
                        Rar5Entry(index = index, path = path)
                    }
                    Rar5(entries)
                }
                "PDF" -> {
                    val countLine = payload.firstOrNull() ?: return null
                    val pageCount = countLine.substringAfter("count=", missingDelimiterValue = "")
                        .toIntOrNull()
                        ?: return null
                    Pdf(pageCount)
                }
                else -> null
            }
        }

        // Os nomes de entrada podem conter quebras de linha e tabs, que quebrariam o formato de
        // uma linha por entrada. Base64 os deixa opacos.
        private fun encodeLine(value: String): String =
            Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

        private fun decodeLine(value: String): String =
            String(Base64.getDecoder().decode(value), Charsets.UTF_8)
    }
}

private fun writeToFile(outputFile: File, writer: (FileOutputStream) -> Unit): Boolean {
    return try {
        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        tempFile.delete()

        FileOutputStream(tempFile).use(writer)

        if (!tempFile.isUsableCache()) {
            tempFile.delete()
            return false
        }

        if (!tempFile.renameTo(outputFile)) {
            tempFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
        }

        outputFile.isUsableCache()
    } catch (_: Exception) {
        false
    }
}

/**
 * Escrita em arquivo temporário seguida de rename, para que um cache interrompido no meio nunca
 * seja lido como se estivesse completo.
 */
internal fun writeCacheFile(outputFile: File, writer: (FileOutputStream) -> Unit): Boolean =
    writeToFile(outputFile, writer)

internal fun File.isUsableCache(): Boolean = exists() && length() > 0L
