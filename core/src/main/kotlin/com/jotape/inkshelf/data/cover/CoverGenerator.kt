package com.jotape.inkshelf.data.cover

import com.jotape.inkshelf.data.repository.LibraryRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Gera, em segundo plano, as capas que ainda faltam.
 *
 * A varredura só cataloga nomes e caminhos — abrir cada arquivo para extrair a primeira página é
 * caro demais para acontecer durante ela (numa biblioteca de milhares de itens, seriam minutos
 * antes de qualquer coisa aparecer na tela). Então a biblioteca aparece imediatamente com
 * marcadores e as capas vão preenchendo conforme ficam prontas.
 */
class CoverGenerator(
    private val repository: LibraryRepository,
    private val coverExtractor: CoverExtractor,
) {

    /**
     * Gera as capas faltantes. Idempotente: arquivos que já têm capa válida são ignorados, então
     * chamar a cada varredura só custa a consulta.
     *
     * [onCoverReady] é chamado a cada capa gravada, para a UI poder mostrar progresso.
     */
    suspend fun generateMissing(
        concurrency: Int = DEFAULT_CONCURRENCY,
        onCoverReady: (fileId: String) -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        val pending = repository.getAllFilesOnce().filter { it.needsCover() }
        if (pending.isEmpty()) return@withContext

        // Extrair capa é I/O de disco com pico de CPU na decodificação. Sem limite, uma
        // biblioteca grande abriria milhares de arquivos ao mesmo tempo e travaria a máquina.
        val semaphore = Semaphore(concurrency)

        coroutineScope {
            pending.forEach { file ->
                launch {
                    semaphore.withPermit {
                        val source = File(file.id)
                        if (!source.isFile) return@withPermit

                        val cover = coverExtractor.extractCover(file.id, source)
                            ?: return@withPermit

                        repository.insertCover(cover)
                        repository.updateCoverPath(file.id, cover.thumbnailPath)
                        onCoverReady(file.id)
                    }
                }
            }
        }
    }

    /** Capa ausente — ou registrada no banco mas sumida do disco (cache limpo por fora). */
    private fun com.jotape.inkshelf.model.FileItem.needsCover(): Boolean {
        val path = coverPath ?: return true
        val file = File(path)
        return !file.exists() || file.length() == 0L
    }

    companion object {
        private const val DEFAULT_CONCURRENCY = 4
    }
}
