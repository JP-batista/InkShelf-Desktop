package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.cover.CoverExtractor
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.db.dao.FileProgressRow
import com.jotape.inkshelf.data.db.dao.FolderTreeRow
import com.jotape.inkshelf.data.db.entity.CoverEntity
import com.jotape.inkshelf.data.db.entity.FileEntity
import com.jotape.inkshelf.data.db.entity.FolderEntity
import com.jotape.inkshelf.data.db.entity.SettingsEntity
import com.jotape.inkshelf.data.reader.PageExtractor
import com.jotape.inkshelf.data.reader.ReaderCacheStore
import com.jotape.inkshelf.data.scanner.SyncEngine
import com.jotape.inkshelf.model.ComicInfoMetadata
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.model.FormatBreakdown
import com.jotape.inkshelf.model.LibraryProgressSnapshot
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Núcleo do domínio "biblioteca": pastas, arquivos, capas, cache de leitura, scan/sync, busca
 * e as sugestões da home ("continuar lendo", pastas não lidas). O maior dos três serviços
 * extraídos de [LibraryRepository] — inerente ao domínio, não sobrou por falta de divisão.
 */
class LibraryContentRepository(
    dbName: String = InkPaths.MAIN_DB_NAME,
    private val settings: LibrarySettingsRepository,
) {

    private val db = InkShelfDatabase.getInstance(dbName)
    private val coverExtractor = CoverExtractor()
    private val pageExtractor = PageExtractor()
    private val readerCache = ReaderCacheStore()
    private val folderDao = db.folderDao()
    private val fileDao = db.fileDao()
    private val coverDao = db.coverDao()
    private val settingsDao = db.settingsDao()
    private val chapterPaginationCacheDao = db.chapterPaginationCacheDao()

    // Progresso das pastas agregado a partir de projeções ENXUTAS (id/parentId e
    // folderId/pages/currentPage/isRead) com distinctUntilChanged: em bibliotecas
    // grandes, cada gravação de capa/comicInfo invalidava a tabela inteira e
    // recarregava 30k FileEntity completas + reagregava a árvore a cada card
    // atualizado. Com a projeção, essas gravações produzem listas idênticas e o
    // recálculo é suprimido; só mudanças reais de progresso/estrutura propagam.
    private fun visibleFolderTreeFlow(): Flow<List<FolderTreeRow>> =
        folderDao.getVisibleFolderTree().distinctUntilChanged()

    private fun fullFolderTreeFlow(): Flow<List<FolderTreeRow>> =
        folderDao.getFolderTree().distinctUntilChanged()

    private fun visibleFileProgressFlow(): Flow<List<FileProgressRow>> =
        fileDao.getVisibleFileProgressRows().distinctUntilChanged()

    private fun fullFileProgressFlow(): Flow<List<FileProgressRow>> =
        fileDao.getFileProgressRows().distinctUntilChanged()

    fun getFolders(parentId: String): Flow<List<FolderItem>> =
        combine(
            folderDao.getVisibleFoldersByParent(parentId),
            visibleFolderTreeFlow(),
            visibleFileProgressFlow(),
        ) { visibleFolders, tree, files ->
            mapFoldersWithProgress(visibleFolders, tree, files)
        }

    fun getFoldersIncludingHidden(parentId: String): Flow<List<FolderItem>> =
        combine(
            folderDao.getFoldersByParent(parentId),
            fullFolderTreeFlow(),
            fullFileProgressFlow(),
        ) { visibleFolders, tree, files ->
            mapFoldersWithProgress(visibleFolders, tree, files)
        }

    fun getFavoriteFolders(): Flow<List<FolderItem>> =
        combine(
            folderDao.getVisibleFavoriteFolders(),
            visibleFolderTreeFlow(),
            visibleFileProgressFlow(),
        ) { visibleFolders, tree, files ->
            mapFoldersWithProgress(visibleFolders, tree, files)
        }

    fun getAllFolders(): Flow<List<FolderItem>> =
        combine(
            folderDao.getVisibleAllFolders(),
            visibleFolderTreeFlow(),
            visibleFileProgressFlow(),
        ) { allFolders, tree, files ->
            mapFoldersWithProgress(allFolders, tree, files)
        }

    suspend fun insertFolder(folder: FolderEntity) = folderDao.insert(folder)
    suspend fun insertFolders(folders: List<FolderEntity>) = folderDao.insertAll(folders)
    suspend fun setFolderFavorite(id: String, isFavorite: Boolean) = folderDao.setFavorite(id, isFavorite)
    suspend fun setFolderHidden(id: String, isHidden: Boolean) = folderDao.setHidden(id, isHidden)

    suspend fun addFolderToFavorites(id: String) {
        setFolderFavoriteRecursively(id, true)
    }

    suspend fun removeFolderFromFavorites(id: String) {
        setFolderFavoriteRecursively(id, false)
    }

    suspend fun setFolderCover(id: String, coverId: String?) = folderDao.setCustomCover(id, coverId)

    fun getFiles(folderId: String): Flow<List<FileItem>> =
        fileDao.getVisibleFilesByFolder(folderId).map { list -> list.map { it.toModel() } }

    fun getFilesIncludingHidden(folderId: String): Flow<List<FileItem>> =
        fileDao.getFilesByFolder(folderId).map { list -> list.map { it.toModel() } }

    fun getInProgressFiles(): Flow<List<FileItem>> =
        fileDao.getVisibleInProgressFiles().map { list -> list.map { it.toModel() } }

    fun getFavoriteFiles(): Flow<List<FileItem>> =
        fileDao.getVisibleFavoriteFiles().map { list -> list.map { it.toModel() } }

    fun getAllFilesFlow(): Flow<List<FileItem>> =
        fileDao.getVisibleAllFiles().map { list -> list.map { it.toModel() } }

    fun getRootPathFlow(): Flow<String?> = settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ROOT_PATH)

    fun getRootPathsFlow(): Flow<List<String>> =
        combine(
            settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ROOT_PATHS),
            settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ROOT_PATH),
        ) { newPaths, legacyPath ->
            if (newPaths != null) parseRootPaths(newPaths)
            else if (legacyPath != null) listOf(legacyPath)
            else emptyList()
        }

    suspend fun getRootPathsOnce(): List<String> {
        val raw = settingsDao.getValue(LibrarySettingsKeys.SETTING_ROOT_PATHS)
        if (raw != null) return parseRootPaths(raw)
        val legacy = settingsDao.getValue(LibrarySettingsKeys.SETTING_ROOT_PATH)
        return if (legacy != null) listOf(legacy) else emptyList()
    }

    suspend fun addRootPath(uriString: String) {
        val current = getRootPathsOnce()
        if (uriString !in current) setRootPaths(current + uriString)
    }

    suspend fun removeRootPath(uriString: String) {
        setRootPaths(getRootPathsOnce() - uriString)
    }

    private suspend fun setRootPaths(paths: List<String>) {
        settingsDao.setValue(
            SettingsEntity(LibrarySettingsKeys.SETTING_ROOT_PATHS, paths.joinToString(LibrarySettingsKeys.ROOT_PATHS_SEPARATOR)),
        )
    }

    private fun parseRootPaths(raw: String): List<String> =
        raw.split(LibrarySettingsKeys.ROOT_PATHS_SEPARATOR).filter { it.isNotBlank() }

    suspend fun getFileById(id: String): FileItem? =
        fileDao.getFileById(id)?.toModel()

    suspend fun insertFile(file: FileEntity) = fileDao.insert(file)
    suspend fun insertFiles(files: List<FileEntity>) = fileDao.insertAll(files)
    suspend fun setFileRead(id: String, isRead: Boolean) {
        fileDao.setRead(id, isRead)
        if (isRead) {
            readerCache.clearPageCache(id)
        }
    }

    suspend fun setFileFavorite(id: String, isFavorite: Boolean) = fileDao.setFavorite(id, isFavorite)
    suspend fun setFileHidden(id: String, isHidden: Boolean) = fileDao.setHidden(id, isHidden)
    suspend fun updateProgress(id: String, page: Int) = fileDao.updateProgress(id, page)
    suspend fun updatePages(id: String, pages: Int) = fileDao.updatePages(id, pages)
    suspend fun updateCoverPath(id: String, path: String) = fileDao.updateCoverPath(id, path)
    suspend fun updateComicInfo(id: String, metadata: ComicInfoMetadata?) =
        fileDao.updateComicInfo(
            id = id,
            title = metadata?.title,
            series = metadata?.series,
            number = metadata?.number,
            writer = metadata?.writer,
            penciller = metadata?.penciller,
            artist = metadata?.artist,
            colorist = metadata?.colorist,
            publisher = metadata?.publisher,
            year = metadata?.year,
            summary = metadata?.summary,
            scanned = true,
        )

    suspend fun getChapterPageCount(
        fileId: String,
        chapterIndex: Int,
        textZoom: Int,
        width: Int,
        height: Int,
    ): Int? = chapterPaginationCacheDao.getPageCount(fileId, chapterIndex, textZoom, width, height)

    suspend fun saveChapterPageCount(
        fileId: String,
        chapterIndex: Int,
        textZoom: Int,
        width: Int,
        height: Int,
        pageCount: Int,
    ) = chapterPaginationCacheDao.upsert(
        com.jotape.inkshelf.data.db.entity.ChapterPaginationCacheEntity(
            fileId = fileId,
            chapterIndex = chapterIndex,
            textZoom = textZoom,
            viewportWidth = width,
            viewportHeight = height,
            pageCount = pageCount,
            cachedAt = System.currentTimeMillis(),
        )
    )

    suspend fun clearChapterPaginationCache(fileId: String) =
        chapterPaginationCacheDao.deleteByFileId(fileId)

    suspend fun getEpubScrollPercent(fileId: String): Int =
        fileDao.getFileById(fileId)?.epubScrollPercent ?: 0

    suspend fun saveEpubScrollPercent(fileId: String, chapterIndex: Int, scrollPercent: Int) {
        fileDao.updateProgress(fileId, chapterIndex)
        fileDao.updateEpubScrollPercent(fileId, scrollPercent)
    }

    suspend fun resetEpubScrollPercent(fileId: String) =
        fileDao.updateEpubScrollPercent(fileId, 0)

    suspend fun saveEpubPageProgress(fileId: String, globalCurrentPage: Int, totalPages: Int, textZoom: Int) =
        fileDao.updateEpubPageProgress(fileId, globalCurrentPage, totalPages, textZoom)

    suspend fun clearEpubPageProgress(fileId: String) =
        fileDao.clearEpubPageProgress(fileId)

    suspend fun insertCover(cover: CoverEntity) = coverDao.insert(cover)
    suspend fun getCover(fileId: String) = coverDao.getCoverByFileId(fileId)
    suspend fun deleteCover(fileId: String) = coverDao.deleteCover(fileId)
    suspend fun getTotalCoverSize(): Long = coverDao.getTotalSizeBytes() ?: 0L

    suspend fun setFileCoverFromPageImage(
        fileId: String,
        imageFile: File,
    ): String? {
        val file = fileDao.getFileById(fileId) ?: return null
        val previousFileCoverPath = file.coverPath
        val cover = coverExtractor.createCoverFromImageFile(fileId, imageFile) ?: return null

        coverDao.insert(cover)
        fileDao.updateCoverPath(fileId, cover.thumbnailPath)
        propagateFolderCoverPathForFile(
            folderId = file.folderId,
            fileId = fileId,
            previousCoverPath = previousFileCoverPath,
            newCoverPath = cover.thumbnailPath,
        )

        return cover.thumbnailPath
    }

    /**
     * Tudo que o app ocupa em disco. No Android eram três diretórios separados, entregues pelo
     * Context; aqui é uma árvore só, sob %LOCALAPPDATA%.
     */
    suspend fun getTotalAppStorageSize(): Long = InkPaths.root.totalSize()

    /**
     * Evicts old reader cache (least-recently-read first) so the whole app
     * storage stays at or below the configured limit. Because only the reader
     * cache (pages + archives) can be LRU-evicted, the non-evictable overhead
     * (covers, database, settings) is measured and subtracted from the budget,
     * keeping the total — the number shown in settings — under the limit.
     * A limit of 0 means unlimited; [excludeFileId] is the file being opened.
     */
    suspend fun enforceCacheLimit(excludeFileId: String?) {
        val limitMb = settings.getCacheLimitMb()
        if (limitMb <= 0) return
        val limitBytes = limitMb.toLong() * 1024L * 1024L

        val store = readerCache
        val readerCacheBytes = store.totalCacheSize()
        val totalAppBytes = getTotalAppStorageSize()
        val overheadBytes = (totalAppBytes - readerCacheBytes).coerceAtLeast(0L)
        val readerBudget = (limitBytes - overheadBytes).coerceAtLeast(0L)

        store.evictToLimit(readerBudget, excludeFileId)
    }

    suspend fun getFolderTitle(folderId: String): String? =
        folderDao.getFolderById(folderId)?.name

    suspend fun getFolderCoverInfo(folderId: String): Pair<String?, String?>? {
        val entity = folderDao.getFolderById(folderId) ?: return null
        return entity.coverPath to entity.parentId
    }

    suspend fun getFirstInProgressFile(): FileItem? =
        fileDao.getVisibleInProgressFilesOnce().firstOrNull()?.toModel()

    suspend fun getInProgressFilesOnce(): List<FileItem> =
        fileDao.getVisibleInProgressFilesOnce().map { it.toModel() }

    suspend fun getAllFilesOnce(): List<FileItem> =
        fileDao.getVisibleAllFilesOnce().map { it.toModel() }

    suspend fun getFavoriteWidgetSummary(): FavoriteWidgetSummary =
        FavoriteWidgetSummary(
            favoriteFiles = fileDao.countVisibleFavoriteFiles(),
            favoriteFolders = folderDao.countVisibleFavoriteFolders(),
        )

    fun getLibraryProgressSnapshotFlow(): Flow<LibraryProgressSnapshot> =
        fileDao.getVisibleAllFiles().map { allFiles ->
            val readFiles = allFiles.count { it.isRead }
            val inProgressFiles = allFiles.count { !it.isRead && it.currentPage > 0 }
            val notStartedFiles = allFiles.count { !it.isRead && it.currentPage <= 0 }
            val favoriteReadFiles = allFiles.count { it.isFavorite && it.isRead }
            val favoriteUnreadFiles = allFiles.count { it.isFavorite && !it.isRead }
            val totalFiles = allFiles.size

            val totalLibraryPages = allFiles.sumOf { it.pages }
            val readLibraryPages = allFiles.sumOf { file ->
                if (file.isRead) file.pages else file.currentPage.coerceIn(0, file.pages)
            }

            val formats = allFiles
                .groupBy { formatLabelOf(it.name) }
                .map { (format, files) ->
                    FormatBreakdown(
                        format = format,
                        total = files.size,
                        read = files.count { it.isRead },
                    )
                }
                .sortedByDescending { it.total }

            val libraryReadProgress = if (totalFiles > 0) {
                allFiles.sumOf { it.aggregateProgress().toDouble() }.toFloat() / totalFiles
            } else {
                0f
            }

            LibraryProgressSnapshot(
                totalFiles = totalFiles,
                readFiles = readFiles,
                inProgressFiles = inProgressFiles,
                notStartedFiles = notStartedFiles,
                favoriteReadFiles = favoriteReadFiles,
                favoriteUnreadFiles = favoriteUnreadFiles,
                totalLibraryPages = totalLibraryPages,
                readLibraryPages = readLibraryPages,
                formats = formats,
                libraryReadProgress = libraryReadProgress.coerceIn(0f, 1f),
            )
        }

    private fun formatLabelOf(name: String): String =
        when (name.substringAfterLast('.', "").lowercase()) {
            "cbz", "zip" -> "CBZ"
            "cbr", "rar" -> "CBR"
            "pdf" -> "PDF"
            "epub" -> "EPUB"
            else -> "Outro"
        }

    fun getNextSuggestionCandidatesFlow(): Flow<List<NextFileSuggestionCandidate>> =
        combine(
            fileDao.getVisibleAllFiles(),
            folderDao.getVisibleAllFolders(),
        ) { allFiles, allFolders ->
            buildNextSuggestionCandidates(
                allFiles = allFiles,
                folderTitlesById = allFolders.associate { it.id to it.name },
            )
        }

    fun getFolderContinuationQueuesFlow(): Flow<List<FolderContinuationQueue>> =
        combine(
            folderDao.getVisibleAllFolders(),
            fileDao.getVisibleAllFiles(),
        ) { allFolders, allFiles ->
            buildFolderContinuationQueues(
                allFolders = allFolders,
                allFiles = allFiles,
            )
        }

    fun getSuggestedUnreadFoldersFlow(): Flow<List<FolderItem>> =
        combine(
            folderDao.getVisibleAllFolders(),
            fileDao.getVisibleAllFiles(),
        ) { allFolders, allFiles ->
            buildSuggestedUnreadFolders(
                allFolders = allFolders,
                allFiles = allFiles,
            )
        }

    fun getSuggestedUnreadFolderQueuesFlow(): Flow<List<FolderSuggestionQueue>> =
        combine(
            folderDao.getVisibleAllFolders(),
            fileDao.getVisibleAllFiles(),
        ) { allFolders, allFiles ->
            buildSuggestedUnreadFolderQueues(
                allFolders = allFolders,
                allFiles = allFiles,
            )
        }

    suspend fun getNextFileInFolder(fileId: String): FileItem? {
        val currentFile = getFileById(fileId) ?: return null
        val siblings = getFilesOnce(currentFile.folderId).sortedBy { it.title.lowercase() }
        val index = siblings.indexOfFirst { it.id == fileId }
        return if (index >= 0 && index < siblings.lastIndex) siblings[index + 1] else null
    }

    suspend fun getNextSuggestionCandidate(): NextFileSuggestionCandidate? {
        return getNextSuggestionCandidates().firstOrNull()
    }

    suspend fun getNextSuggestionCandidates(): List<NextFileSuggestionCandidate> {
        val allFolders = folderDao.getVisibleAllFolders().firstOrNull().orEmpty()
        return buildNextSuggestionCandidates(
            allFiles = fileDao.getVisibleAllFilesOnce(),
            folderTitlesById = allFolders.associate { it.id to it.name },
        )
    }

    suspend fun clearAllLibraryData() {
        folderDao.deleteAll()
        fileDao.deleteAll()
        coverDao.deleteAll()
        coverExtractor.clearAllCovers()
        pageExtractor.clearAllPageCache()
    }

    suspend fun clearCovers() {
        coverDao.deleteAll()
        coverExtractor.clearAllCovers()
        fileDao.clearAllCoverPaths()
        folderDao.clearAllCoverPaths()
    }

    /** Tamanho em bytes do cache de páginas (páginas extraídas + arquivos de leitura). */
    suspend fun getPageCacheSize(): Long =
        readerCache.totalCacheSize()

    /** Limpa apenas o cache de páginas — biblioteca, progresso e capas são preservados. */
    suspend fun clearPageCache() {
        pageExtractor.clearAllPageCache()
    }

    /**
     * Limpa todo o cache (capas + páginas), preservando a biblioteca, o progresso
     * de leitura, favoritos, histórico e as configurações.
     */
    suspend fun clearAllCache() {
        clearCovers()
        pageExtractor.clearAllPageCache()
    }

    suspend fun initialScan(root: File) {
        syncEngine().initialScan(root)
    }

    /** Acrescenta uma raiz a uma biblioteca que já tem outras. */
    suspend fun addRootScan(root: File) {
        syncEngine().initialScan(root)
    }

    suspend fun rescan(root: File) {
        syncEngine().rescan(root)
    }

    suspend fun rescanMultiple(roots: List<File>) {
        syncEngine().rescan(roots)
    }

    private fun syncEngine() = SyncEngine.withDiskCaches(db)

    suspend fun updateFolderCoverPath(id: String, path: String) =
        folderDao.updateCoverPath(id, path)

    suspend fun getFilesOnce(folderId: String, includeHidden: Boolean = false): List<FileItem> =
        if (includeHidden) {
            fileDao.getFilesByFolderOnce(folderId)
        } else {
            fileDao.getVisibleFilesByFolderOnce(folderId)
        }.map { it.toModel() }

    suspend fun getSubFolderIdsOnce(folderId: String, includeHidden: Boolean = false): List<String> =
        if (includeHidden) {
            folderDao.getSubFolderIds(folderId)
        } else {
            folderDao.getVisibleSubFolderIds(folderId)
        }

    fun searchFiles(query: String): Flow<List<FileItem>> =
        fileDao.searchVisibleFiles(query).map { list -> list.map { it.toModel() } }

    fun searchFolders(query: String): Flow<List<FolderItem>> =
        combine(
            folderDao.searchVisibleFolders(query),
            visibleFolderTreeFlow(),
            visibleFileProgressFlow(),
        ) { visibleFolders, tree, files ->
            mapFoldersWithProgress(visibleFolders, tree, files)
        }

    suspend fun countItemsInFolder(folderId: String): Int {
        val directFiles = fileDao.countFilesInFolder(folderId)
        val subFolderIds = folderDao.getSubFolderIds(folderId)
        val subCount = subFolderIds.sumOf { countItemsInFolder(it) }
        return directFiles + subCount
    }

    private suspend fun setFolderFavoriteRecursively(folderId: String, isFavorite: Boolean) {
        folderDao.setFavorite(folderId, isFavorite)
        fileDao.getFilesByFolderOnce(folderId).forEach { file ->
            fileDao.setFavorite(file.id, isFavorite)
        }
        folderDao.getSubFolderIds(folderId).forEach { subId ->
            setFolderFavoriteRecursively(subId, isFavorite)
        }
    }

    suspend fun setFolderHiddenRecursively(folderId: String, isHidden: Boolean) {
        folderDao.setHidden(folderId, isHidden)
        fileDao.getFilesByFolderOnce(folderId).forEach { file ->
            fileDao.setHidden(file.id, isHidden)
        }
        folderDao.getSubFolderIds(folderId).forEach { subId ->
            setFolderHiddenRecursively(subId, isHidden)
        }
    }

    private fun mapFoldersWithProgress(
        visibleFolders: List<FolderEntity>,
        tree: List<FolderTreeRow>,
        files: List<FileProgressRow>,
    ): List<FolderItem> {
        val statsByFolderId = buildFolderProgressStats(tree, files)
        return visibleFolders.map { folder ->
            folder.toModel(statsByFolderId[folder.id])
        }
    }

    private fun buildFolderProgressStats(
        tree: List<FolderTreeRow>,
        files: List<FileProgressRow>,
    ): Map<String, FolderProgressStats> {
        val childFoldersByParent = tree.groupBy { it.parentId }
        val filesByFolder = files.groupBy { it.folderId }
        val cache = mutableMapOf<String, FolderProgressStats>()

        fun collect(folderId: String): FolderProgressStats {
            cache[folderId]?.let { return it }

            val directFiles = filesByFolder[folderId].orEmpty()
            val directFileCount = directFiles.size
            val directProgressSum = directFiles.sumOf { file ->
                file.aggregateProgress().toDouble()
            }.toFloat()
            val childStats = childFoldersByParent[folderId].orEmpty().map { child ->
                collect(child.id)
            }
            val totalFiles = directFileCount + childStats.sumOf { it.totalFiles }
            val progressSum = directProgressSum + childStats.sumOf { it.progressSum.toDouble() }.toFloat()

            return FolderProgressStats(
                totalFiles = totalFiles,
                progressSum = progressSum,
            ).also { cache[folderId] = it }
        }

        tree.forEach { folder ->
            collect(folder.id)
        }

        return cache
    }

    private fun FolderEntity.toTreeRow() = FolderTreeRow(id = id, parentId = parentId)

    private fun FileEntity.toProgressRow() = FileProgressRow(
        folderId = folderId,
        pages = pages,
        currentPage = currentPage,
        isRead = isRead,
    )

    private fun buildNextSuggestionCandidates(
        allFiles: List<FileEntity>,
        folderTitlesById: Map<String, String>,
    ): List<NextFileSuggestionCandidate> {
        val groupedFiles = allFiles
            .groupBy { it.folderId }
            .values

        val results = mutableListOf<NextFileSuggestionCandidate>()
        groupedFiles.forEach { files ->
            val orderedFiles = files.sortedBy { it.name.lowercase() }
            orderedFiles.zipWithNext().firstOrNull { (current, next) ->
                current.isRead && !next.isRead && next.currentPage <= 0
            }?.let { (current, next) ->
                results += NextFileSuggestionCandidate(
                    completedFile = current.toModel(),
                    nextFile = next.toModel(),
                    folderTitle = folderTitlesById[next.folderId],
                )
            }
        }

        return results.sortedWith(
            compareBy<NextFileSuggestionCandidate> { it.folderTitle.orEmpty().lowercase() }
                .thenBy { it.nextFile.title.lowercase() },
        )
    }

    private fun buildFolderContinuationQueues(
        allFolders: List<FolderEntity>,
        allFiles: List<FileEntity>,
    ): List<FolderContinuationQueue> {
        val statsByFolderId = buildFolderProgressStats(
            allFolders.map { it.toTreeRow() },
            allFiles.map { it.toProgressRow() },
        )
        val foldersById = allFolders.associateBy { it.id }

        return allFiles
            .groupBy { it.folderId }
            .mapNotNull { (folderId, filesInFolder) ->
                val folder = foldersById[folderId] ?: return@mapNotNull null
                val orderedFiles = filesInFolder.sortedBy { it.name.lowercase() }
                val lastEngagedIndex = orderedFiles.indexOfLast { file ->
                    file.isRead || file.currentPage > 0
                }
                if (lastEngagedIndex < 0 || lastEngagedIndex >= orderedFiles.lastIndex) {
                    return@mapNotNull null
                }

                val pendingFiles = orderedFiles
                    .drop(lastEngagedIndex + 1)
                    .filterNot { it.isRead }
                    .map { it.toModel() }

                if (pendingFiles.isEmpty()) {
                    return@mapNotNull null
                }

                val folderItem = folder.toModel(statsByFolderId[folder.id])
                if (folderItem.progress <= 0f || folderItem.isRead) {
                    return@mapNotNull null
                }

                FolderContinuationQueue(
                    folder = folderItem,
                    pendingFiles = pendingFiles,
                )
            }
            .sortedWith(
                compareByDescending<FolderContinuationQueue> { it.folder.progress }
                    .thenBy { it.folder.title.lowercase() },
            )
    }

    private fun buildSuggestedUnreadFolders(
        allFolders: List<FolderEntity>,
        allFiles: List<FileEntity>,
    ): List<FolderItem> {
        val statsByFolderId = buildFolderProgressStats(
            allFolders.map { it.toTreeRow() },
            allFiles.map { it.toProgressRow() },
        )
        val directFilesByFolderId = allFiles.groupBy { it.folderId }

        return allFolders
            .map { folder -> folder.toModel(statsByFolderId[folder.id]) }
            .filter { folder ->
                folder.itemCount > 0 &&
                    folder.progress <= 0f &&
                    !folder.isRead &&
                    directFilesByFolderId[folder.id].orEmpty().isNotEmpty()
            }
            .sortedWith(
                compareByDescending<FolderItem> { directFilesByFolderId[it.id].orEmpty().size }
                    .thenByDescending { it.itemCount }
                    .thenBy { it.title.lowercase() },
            )
    }

    private fun buildSuggestedUnreadFolderQueues(
        allFolders: List<FolderEntity>,
        allFiles: List<FileEntity>,
    ): List<FolderSuggestionQueue> {
        val statsByFolderId = buildFolderProgressStats(
            allFolders.map { it.toTreeRow() },
            allFiles.map { it.toProgressRow() },
        )
        val directFilesByFolderId = allFiles.groupBy { it.folderId }

        return allFolders
            .mapNotNull { folder ->
                val folderItem = folder.toModel(statsByFolderId[folder.id])
                if (
                    folderItem.itemCount <= 0 ||
                    folderItem.progress > 0f ||
                    folderItem.isRead
                ) {
                    return@mapNotNull null
                }

                val suggestedFiles = directFilesByFolderId[folder.id]
                    .orEmpty()
                    .sortedBy { it.name.lowercase() }
                    .filterNot { it.isRead }
                    .take(LibrarySettingsKeys.HOME_MAX_ITEMS_PER_RAIL)
                    .map { it.toModel() }

                if (suggestedFiles.isEmpty()) {
                    return@mapNotNull null
                }

                FolderSuggestionQueue(
                    folder = folderItem,
                    suggestedFiles = suggestedFiles,
                )
            }
            .sortedWith(
                compareByDescending<FolderSuggestionQueue> { it.suggestedFiles.size }
                    .thenByDescending { it.folder.itemCount }
                    .thenBy { it.folder.title.lowercase() },
            )
    }

    private fun FolderEntity.toModel(stats: FolderProgressStats? = null) = FolderItem(
        id = id,
        title = name,
        isFavorite = isFavorite,
        isHidden = isHidden,
        parentId = parentId,
        itemCount = stats?.totalFiles ?: itemCount,
        progress = stats?.progress ?: 0f,
        customCoverId = customCoverId,
        coverPath = coverPath,
        lastModifiedAt = lastModifiedAt,
    )

    private fun FileEntity.toModel() = FileItem(
        id = id,
        title = name,
        isFavorite = isFavorite,
        isHidden = isHidden,
        pages = pages,
        currentPage = currentPage,
        isRead = isRead,
        coverPath = coverPath,
        folderId = folderId,
        comicInfo = toComicInfoMetadata(),
        comicInfoScanned = comicInfoScanned,
        epubGlobalCurrentPage = epubGlobalCurrentPage,
        epubTotalPages = epubTotalPages,
        epubLastTextZoom = epubLastTextZoom,
    )

    private fun FileEntity.toComicInfoMetadata(): ComicInfoMetadata? =
        ComicInfoMetadata(
            title = comicInfoTitle,
            series = comicInfoSeries,
            number = comicInfoNumber,
            writer = comicInfoWriter,
            penciller = comicInfoPenciller,
            artist = comicInfoArtist,
            colorist = comicInfoColorist,
            publisher = comicInfoPublisher,
            year = comicInfoYear,
            summary = comicInfoSummary,
        ).takeIf { it.hasAnyValue }

    private fun FileEntity.aggregateProgress(): Float = when {
        isRead -> 1f
        pages > 0 -> (currentPage.toFloat() / pages).coerceIn(0f, 1f)
        else -> 0f
    }

    private fun FileProgressRow.aggregateProgress(): Float = when {
        isRead -> 1f
        pages > 0 -> (currentPage.toFloat() / pages).coerceIn(0f, 1f)
        else -> 0f
    }

    private suspend fun propagateFolderCoverPathForFile(
        folderId: String,
        fileId: String,
        previousCoverPath: String?,
        newCoverPath: String,
    ) {
        var currentFolderId: String? = folderId
        while (currentFolderId != null && currentFolderId != LibrarySettingsKeys.ROOT_FOLDER_ID) {
            val folder = folderDao.getFolderById(currentFolderId) ?: break
            val usesCustomCover = folder.customCoverId == fileId
            val usesPreviousPath = previousCoverPath != null && folder.coverPath == previousCoverPath

            if (usesCustomCover || usesPreviousPath) {
                folderDao.updateCoverPath(folder.id, newCoverPath)
            }

            currentFolderId = folder.parentId
        }
    }

    private fun File.totalSize(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { child -> child.totalSize() } ?: 0L
    }

    private data class FolderProgressStats(
        val totalFiles: Int,
        val progressSum: Float,
    ) {
        val progress: Float
            get() = if (totalFiles > 0) {
                (progressSum / totalFiles).coerceIn(0f, 1f)
            } else {
                0f
            }
    }
}
