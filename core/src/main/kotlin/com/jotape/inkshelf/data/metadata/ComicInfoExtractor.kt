package com.jotape.inkshelf.data.metadata

import com.github.junrar.Archive
import com.jotape.inkshelf.model.ComicInfoMetadata
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import org.w3c.dom.Element

/**
 * Lê o `ComicInfo.xml` embutido no arquivo — o padrão de metadados usado por praticamente todo
 * CBZ/CBR catalogado (título, série, número, equipe criativa, editora, ano, sinopse).
 *
 * Nota de porte: o Android parseava com `XmlPullParser`, que é da plataforma. Aqui é o parser DOM
 * do próprio JDK. O arquivo é minúsculo (alguns KB), então carregar a árvore inteira em memória
 * não pesa e sai mais simples que percorrer eventos.
 */
class ComicInfoExtractor {

    fun extract(file: File): ComicInfoMetadata? = when (detectFormat(file)) {
        Format.ZIP -> extractFromZip(file)
        Format.RAR -> extractFromRar(file)
        Format.RAR5 -> extractFromRar5(file)
        Format.PDF, null -> null
    }

    private fun extractFromZip(file: File): ComicInfoMetadata? = try {
        ZipFile(file).use { zip ->
            val entry = zip.entries().asSequence()
                .firstOrNull { !it.isDirectory && it.name.isComicInfoFile() }
            entry?.let { zip.getInputStream(it).use(::parseComicInfo) }
        }
    } catch (_: Exception) {
        null
    }

    private fun extractFromRar(file: File): ComicInfoMetadata? = try {
        Archive(file).use { archive ->
            val header = archive.fileHeaders.firstOrNull {
                !it.isDirectory && it.fileName.isComicInfoFile()
            }
            header?.let { archive.getInputStream(it).use(::parseComicInfo) }
        }
    } catch (_: Exception) {
        null
    }

    private fun extractFromRar5(file: File): ComicInfoMetadata? {
        val bytes = withRar5Archive(file) { archive ->
            val entryIndex = (0 until archive.numberOfItems).firstOrNull { index ->
                val path = archive.getStringProperty(index, PropID.PATH) ?: return@firstOrNull false
                val isFolder = archive.getProperty(index, PropID.IS_FOLDER) as? Boolean ?: false
                !isFolder && path.isComicInfoFile()
            } ?: return@withRar5Archive null

            val output = ByteArrayOutputStream()
            val result = archive.extractSlow(entryIndex, ISequentialOutStream { data ->
                output.write(data)
                data.size
            })

            if (result == ExtractOperationResult.OK) output.toByteArray() else null
        } ?: return null

        return ByteArrayInputStream(bytes).use(::parseComicInfo)
    }

    private fun parseComicInfo(input: InputStream): ComicInfoMetadata? = try {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // O arquivo vem de dentro de um .cbz de origem desconhecida: desligar entidades
            // externas fecha a porta para XXE (um XML malicioso lendo arquivos da máquina).
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
            isNamespaceAware = false
        }

        val document = factory.newDocumentBuilder().parse(input)
        val fields = HashMap<String, String>()

        val children = document.documentElement?.childNodes
        if (children != null) {
            for (i in 0 until children.length) {
                val element = children.item(i) as? Element ?: continue
                val value = element.textContent?.normalizedOrNull() ?: continue
                // Primeira ocorrência vence, como no parser sequencial do mobile.
                fields.putIfAbsent(element.tagName.lowercase(), value)
            }
        }

        ComicInfoMetadata(
            title = fields["title"],
            series = fields["series"],
            number = fields["number"],
            writer = fields["writer"],
            penciller = fields["penciller"],
            artist = fields["artist"],
            colorist = fields["colorist"],
            publisher = fields["publisher"],
            year = fields["year"]?.toIntOrNull(),
            summary = fields["summary"],
        ).takeIf { it.hasAnyValue }
    } catch (_: Exception) {
        null
    }

    private fun detectFormat(file: File): Format? = try {
        file.inputStream().use { input ->
            val header = ByteArray(8)
            val read = input.read(header)
            when {
                read < 4 -> null
                header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() -> Format.ZIP
                header[0] == 0x52.toByte() &&
                    header[1] == 0x61.toByte() &&
                    header[2] == 0x72.toByte() &&
                    header[3] == 0x21.toByte() ->
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

    private enum class Format { ZIP, RAR, RAR5, PDF }
}

/** O arquivo pode estar em qualquer subpasta do compactado, com qualquer caixa. */
private fun String.isComicInfoFile(): Boolean =
    replace('\\', '/').substringAfterLast('/').equals("comicinfo.xml", ignoreCase = true)

private fun String?.normalizedOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
