package com.jotape.inkshelf.data.scanner

import com.jotape.inkshelf.data.cover.CoverExtractor
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.db.entity.FileEntity
import com.jotape.inkshelf.data.db.entity.FolderEntity
import com.jotape.inkshelf.data.db.inkTransaction
import com.jotape.inkshelf.data.reader.ReaderCacheStore
import java.io.File

/**
 * Cache em disco que guarda arquivos indexados por `fileId` e sabe descartar os que não
 * pertencem mais à biblioteca.
 *
 * Existe como interface porque o `SyncEngine` (Fase 1) precisa limpar caches que só nascem na
 * Fase 2 — capas, páginas extraídas, arquivos descompactados. Cada um deles vai implementar
 * isto e ser passado no construtor, sem que o motor de sincronização precise conhecê-los.
 */
interface PrunableCache {
    fun retainOnly(validFileIds: Set<String>)
}

/**
 * Concilia o que existe no disco com o que está no banco.
 *
 * O ponto central: uma nova varredura **não pode perder o estado do usuário**. Favoritos,
 * progresso de leitura, capas escolhidas à mão e metadados de ComicInfo vivem só no banco — o
 * disco não sabe nada deles. Por isso o diff mescla cada linha existente com a varredura em vez
 * de sobrescrever, e é isso que os testes deste módulo protegem.
 */
class SyncEngine(
    private val db: InkShelfDatabase,
    private val diskCaches: List<PrunableCache> = emptyList(),
) {

    companion object {
        // Lotes abaixo do limite de 999 variáveis do SQLite (com folga para queries que
        // combinam a lista com outros parâmetros).
        private const val SQL_CHUNK = 500

        /**
         * Motor ligado aos caches reais de página e capa — é este que o app usa. O construtor
         * direto fica sem caches de propósito, para que um teste não apague nada em disco por
         * engano ao instanciar o motor.
         */
        fun withDiskCaches(db: InkShelfDatabase): SyncEngine =
            SyncEngine(db, listOf(ReaderCacheStore(), CoverExtractor()))
    }

    private val scanner = LibraryScanner()

    suspend fun initialScan(root: File) {
        val result = scanner.scan(root)
        db.inkTransaction {
            result.folders.chunked(SQL_CHUNK).forEach { db.folderDao().insertAll(it) }
            result.files.chunked(SQL_CHUNK).forEach { db.fileDao().insertAll(it) }
        }
        updateItemCounts(result)
    }

    suspend fun rescan(root: File) = rescan(listOf(root))

    suspend fun rescan(roots: List<File>) {
        val results = roots.map { scanner.scan(it) }
        val allFolders = results.flatMap { it.folders }
        val allFiles = results.flatMap { it.files }
        syncFolders(allFolders)
        syncFiles(allFiles)
        updateItemCounts(ScanResult(allFolders, allFiles))
        pruneOrphanCaches()
    }

    /**
     * Descarta capas e caches em disco de arquivos que saíram da biblioteca. Roda depois de cada
     * sincronização, para que remover uma pasta raiz realmente libere o espaço que ela ocupava.
     */
    private suspend fun pruneOrphanCaches() {
        val validFileIds = db.fileDao().getAllIds().toHashSet()

        val orphanCoverIds = db.coverDao().getAllFileIds().filter { it !in validFileIds }
        orphanCoverIds.chunked(SQL_CHUNK).forEach { db.coverDao().deleteByIds(it) }

        diskCaches.forEach { it.retainOnly(validFileIds) }

        // Classificações de personagem/equipe também referenciam fileId sem FK/cascade — sem
        // isto, remover um arquivo deixa lixo que infla a contagem dos hubs para sempre.
        db.fileCharacterTagDao().deleteOrphaned()
        db.fileTeamTagDao().deleteOrphaned()
    }

    /**
     * Recalcula `itemCount` de todas as pastas numa única passada memoizada (bottom-up via
     * mapas) e grava só os valores que mudaram, numa transação.
     */
    private suspend fun updateItemCounts(result: ScanResult) {
        val childrenByParent = result.folders.groupBy { it.parentId }
        val directCounts = result.files.groupingBy { it.folderId }.eachCount()
        val counts = HashMap<String, Int>(result.folders.size)

        fun countTree(folderId: String): Int {
            counts[folderId]?.let { return it }
            val direct = directCounts[folderId] ?: 0
            val subTotal = childrenByParent[folderId].orEmpty().sumOf { countTree(it.id) }
            val total = direct + subTotal
            counts[folderId] = total
            return total
        }
        result.folders.forEach { countTree(it.id) }

        val existingCounts = db.folderDao().getItemCountsOnce().associate { it.id to it.itemCount }
        val changed = counts.filter { (id, count) -> existingCounts[id] != count }
        if (changed.isEmpty()) return

        db.inkTransaction {
            changed.forEach { (id, count) -> db.folderDao().updateItemCount(id, count) }
        }
    }

    /**
     * Diff em memória: carrega o estado atual UMA vez num mapa, mescla o estado do usuário e
     * grava somente as linhas novas ou realmente alteradas, em lotes dentro de uma transação.
     * Reescrever linhas idênticas não só custa IO como dispara invalidação da UI à toa.
     */
    private suspend fun syncFolders(scanned: List<FolderEntity>) {
        val existingById = db.folderDao().getAllFoldersOnce().associateBy { it.id }
        val scannedIds = scanned.mapTo(HashSet(scanned.size)) { it.id }
        val toRemove = existingById.keys.filter { it !in scannedIds }

        val toUpsert = scanned.mapNotNull { scannedFolder ->
            val existing = existingById[scannedFolder.id]
            if (existing == null) {
                scannedFolder
            } else {
                val merged = scannedFolder.copy(
                    isFavorite = existing.isFavorite,
                    isHidden = existing.isHidden,
                    customCoverId = existing.customCoverId,
                    coverPath = existing.coverPath,
                    // itemCount é recalculado depois em updateItemCounts; manter o atual
                    // evita reescrever a linha só por causa dele.
                    itemCount = existing.itemCount,
                )
                merged.takeIf { it != existing }
            }
        }

        db.inkTransaction {
            toUpsert.chunked(SQL_CHUNK).forEach { db.folderDao().insertAll(it) }
            if (toRemove.isNotEmpty()) {
                toRemove.chunked(SQL_CHUNK).forEach { chunk ->
                    db.fileDao().deleteByFolderIds(chunk)
                    db.folderDao().deleteByIds(chunk)
                }
            }
        }
    }

    private suspend fun syncFiles(scanned: List<FileEntity>) {
        val existingById = db.fileDao().getAllFilesOnce().associateBy { it.id }
        val scannedIds = scanned.mapTo(HashSet(scanned.size)) { it.id }
        val toRemove = existingById.keys.filter { it !in scannedIds }

        val toUpsert = scanned.mapNotNull { scannedFile ->
            val existing = existingById[scannedFile.id]
            if (existing == null) {
                scannedFile
            } else {
                // Tudo que o usuário ou o app produziram sobrevive à varredura; do disco vêm
                // apenas identidade, nome e pasta.
                val merged = scannedFile.copy(
                    pages = existing.pages,
                    currentPage = existing.currentPage,
                    isRead = existing.isRead,
                    isFavorite = existing.isFavorite,
                    isHidden = existing.isHidden,
                    coverPath = existing.coverPath,
                    comicInfoTitle = existing.comicInfoTitle,
                    comicInfoSeries = existing.comicInfoSeries,
                    comicInfoNumber = existing.comicInfoNumber,
                    comicInfoWriter = existing.comicInfoWriter,
                    comicInfoPenciller = existing.comicInfoPenciller,
                    comicInfoArtist = existing.comicInfoArtist,
                    comicInfoColorist = existing.comicInfoColorist,
                    comicInfoPublisher = existing.comicInfoPublisher,
                    comicInfoYear = existing.comicInfoYear,
                    comicInfoSummary = existing.comicInfoSummary,
                    comicInfoScanned = existing.comicInfoScanned,
                    epubScrollPercent = existing.epubScrollPercent,
                    epubGlobalCurrentPage = existing.epubGlobalCurrentPage,
                    epubTotalPages = existing.epubTotalPages,
                    epubLastTextZoom = existing.epubLastTextZoom,
                )
                merged.takeIf { it != existing }
            }
        }

        db.inkTransaction {
            toUpsert.chunked(SQL_CHUNK).forEach { db.fileDao().insertAll(it) }
            if (toRemove.isNotEmpty()) {
                toRemove.chunked(SQL_CHUNK).forEach { db.fileDao().deleteByIds(it) }
            }
        }
    }
}
