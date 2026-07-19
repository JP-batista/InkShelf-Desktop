package com.jotape.inkshelf.data.cover

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.entity.CoverEntity
import com.jotape.inkshelf.data.reader.PageExtractor
import com.jotape.inkshelf.data.reader.ReaderCacheStore
import com.jotape.inkshelf.data.scanner.PrunableCache
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Gera a miniatura de capa de cada arquivo da biblioteca — a primeira página, reduzida.
 *
 * Nota de porte: no Android esta classe tinha 512 linhas e reimplementava, para uso próprio,
 * detecção de formato, leitura de ZIP/RAR/RAR5/PDF e ordenação natural das páginas — tudo isso
 * já existia no `PageExtractor`, duplicado. Aqui ela apenas pede a primeira página ao
 * [PageExtractor] e a reduz, o que elimina a duplicação e, de quebra, garante que a capa e a
 * primeira página do leitor nunca discordem sobre qual imagem vem primeiro.
 *
 * Capas de EPUB ficaram de fora enquanto o EPUB não entra no app.
 */
class CoverExtractor(
    private val pageExtractor: PageExtractor = PageExtractor(),
) : PrunableCache {

    companion object {
        /** Lado maior da miniatura. Suficiente para o maior card da grade em telas HiDPI. */
        const val COVER_MAX_SIZE = 600
    }

    private val coverDir: File
        get() = File(InkPaths.cacheDir, "covers").apply { mkdirs() }

    /** Capa a partir da primeira página do arquivo, ou `null` se ele não puder ser lido. */
    fun extractCover(fileId: String, file: File): CoverEntity? {
        val firstPage = pageExtractor.getPages(fileId, file).firstOrNull() ?: return null
        val pageImage = pageExtractor.getPageImage(
            firstPage,
            width = COVER_MAX_SIZE,
            height = COVER_MAX_SIZE,
        ) ?: return null

        return createCoverFromImageFile(fileId, pageImage)
    }

    /** Capa a partir de uma imagem qualquer — usado quando o usuário escolhe a capa à mão. */
    fun createCoverFromImageFile(fileId: String, imageFile: File): CoverEntity? {
        val source = try {
            ImageIO.read(imageFile)
        } catch (_: Exception) {
            null
        } ?: return null

        val thumbnail = scaleToFit(source, COVER_MAX_SIZE)
        val outputFile = coverFile(fileId)

        val written = try {
            outputFile.parentFile?.mkdirs()
            val temp = File(outputFile.parentFile, "${outputFile.name}.tmp")
            temp.delete()
            ImageIO.write(thumbnail, "jpg", temp)
            if (temp.length() <= 0L) {
                temp.delete()
                false
            } else {
                if (!temp.renameTo(outputFile)) {
                    temp.copyTo(outputFile, overwrite = true)
                    temp.delete()
                }
                outputFile.length() > 0L
            }
        } catch (_: Exception) {
            false
        }

        if (!written) return null

        return CoverEntity(
            fileId = fileId,
            thumbnailPath = outputFile.absolutePath,
            sizeBytes = outputFile.length(),
        )
    }

    fun deleteCover(fileId: String) {
        coverFile(fileId).delete()
    }

    fun clearAllCovers() {
        coverDir.deleteRecursively()
        coverDir.mkdirs()
    }

    fun totalSizeBytes(): Long =
        coverDir.listFiles().orEmpty().sumOf { it.length() }

    override fun retainOnly(validFileIds: Set<String>) {
        val validNames = validFileIds.mapTo(HashSet()) { "${ReaderCacheStore.cacheKey(it)}.jpg" }
        coverDir.listFiles().orEmpty().forEach { file ->
            if (file.name !in validNames) file.delete()
        }
    }

    /** Mesmo esquema de nome do cache de páginas: o `fileId` é um caminho, não serve como nome. */
    private fun coverFile(fileId: String): File =
        File(coverDir, "${ReaderCacheStore.cacheKey(fileId)}.jpg")

    /**
     * Redimensiona preservando a proporção. Imagens já menores que o alvo passam direto — ampliar
     * uma capa pequena só gastaria espaço e borraria o resultado.
     */
    private fun scaleToFit(source: BufferedImage, maxSize: Int): BufferedImage {
        val largest = maxOf(source.width, source.height)
        if (largest <= maxSize) return toRgb(source)

        val scale = maxSize.toDouble() / largest
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)

        // TYPE_INT_RGB porque o destino é JPEG, que não tem canal alfa: manter transparência
        // aqui deixaria as áreas vazias pretas no arquivo final.
        val target = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        target.createGraphics().apply {
            setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            )
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            drawImage(source, 0, 0, width, height, null)
            dispose()
        }
        return target
    }

    private fun toRgb(source: BufferedImage): BufferedImage {
        if (source.type == BufferedImage.TYPE_INT_RGB) return source
        val target = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)
        target.createGraphics().apply {
            drawImage(source, 0, 0, null)
            dispose()
        }
        return target
    }
}
