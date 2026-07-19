package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.db.entity.FileEntity
import com.jotape.inkshelf.data.db.entity.FolderEntity
import com.jotape.inkshelf.model.ComicInfoMetadata
import com.jotape.inkshelf.model.DailyStatisticPoint
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem
import com.jotape.inkshelf.model.HomeSectionType
import com.jotape.inkshelf.model.FormatBreakdown
import com.jotape.inkshelf.model.LibraryProgressSnapshot
import com.jotape.inkshelf.model.LibrarySortMode
import com.jotape.inkshelf.model.LibraryViewMode
import com.jotape.inkshelf.model.ReadingSessionStat
import com.jotape.inkshelf.model.ReadingHistoryItem
import java.io.File
import kotlinx.coroutines.flow.Flow
/**
 * Ponto de acesso único da UI a todos os dados (fachada). Cada método aqui delega a um dos três
 * repositórios de domínio: [LibrarySettingsRepository] (preferências),
 * [LibraryStatisticsRepository] (histórico, marcadores, estatísticas) e
 * [LibraryContentRepository] (pastas, arquivos, capas, cache, varredura, busca, sugestões da
 * tela inicial).
 *
 * Nota de porte: no Android esta classe também hospedava as ~190 constantes de preferências e
 * quatro tipos de modelo, o que forçava os repositórios de domínio a enxergar a fachada que os
 * contém. Aqui as constantes estão em [LibrarySettingsKeys] e os modelos em
 * `LibraryContentModels.kt`, então a dependência aponta numa direção só.
 *
 * O parâmetro [dbName] existe porque as bibliotecas salvas (snapshots) rodam sobre um `.db`
 * próprio, lado a lado com o principal.
 */
class LibraryRepository(private val dbName: String = InkPaths.MAIN_DB_NAME) {

    private val db = InkShelfDatabase.getInstance(dbName)
    private val settingsDao = db.settingsDao()
    private val fileCharacterTagDao = db.fileCharacterTagDao()
    private val fileTeamTagDao = db.fileTeamTagDao()

    private val settings = LibrarySettingsRepository(dbName)
    private val statistics = LibraryStatisticsRepository(dbName, settings)
    private val content = LibraryContentRepository(dbName, settings)

    // ── Pastas ────────────────────────────────────────────────────────────
    fun getFolders(parentId: String): Flow<List<FolderItem>> = content.getFolders(parentId)
    fun getFoldersIncludingHidden(parentId: String): Flow<List<FolderItem>> = content.getFoldersIncludingHidden(parentId)
    fun getFavoriteFolders(): Flow<List<FolderItem>> = content.getFavoriteFolders()
    fun getAllFolders(): Flow<List<FolderItem>> = content.getAllFolders()
    suspend fun insertFolder(folder: FolderEntity) = content.insertFolder(folder)
    suspend fun insertFolders(folders: List<FolderEntity>) = content.insertFolders(folders)
    suspend fun setFolderFavorite(id: String, isFavorite: Boolean) = content.setFolderFavorite(id, isFavorite)
    suspend fun setFolderHidden(id: String, isHidden: Boolean) = content.setFolderHidden(id, isHidden)
    suspend fun addFolderToFavorites(id: String) = content.addFolderToFavorites(id)
    suspend fun removeFolderFromFavorites(id: String) = content.removeFolderFromFavorites(id)
    suspend fun setFolderCover(id: String, coverId: String?) = content.setFolderCover(id, coverId)
    suspend fun getFolderTitle(folderId: String): String? = content.getFolderTitle(folderId)
    suspend fun getFolderCoverInfo(folderId: String): Pair<String?, String?>? = content.getFolderCoverInfo(folderId)
    suspend fun updateFolderCoverPath(id: String, path: String) = content.updateFolderCoverPath(id, path)
    suspend fun getSubFolderIdsOnce(folderId: String, includeHidden: Boolean = false): List<String> =
        content.getSubFolderIdsOnce(folderId, includeHidden)
    fun searchFolders(query: String): Flow<List<FolderItem>> = content.searchFolders(query)
    suspend fun countItemsInFolder(folderId: String): Int = content.countItemsInFolder(folderId)
    suspend fun setFolderHiddenRecursively(folderId: String, isHidden: Boolean) =
        content.setFolderHiddenRecursively(folderId, isHidden)

    // ── Arquivos ──────────────────────────────────────────────────────────
    fun getFiles(folderId: String): Flow<List<FileItem>> = content.getFiles(folderId)
    fun getFilesIncludingHidden(folderId: String): Flow<List<FileItem>> = content.getFilesIncludingHidden(folderId)
    fun getInProgressFiles(): Flow<List<FileItem>> = content.getInProgressFiles()
    fun getFavoriteFiles(): Flow<List<FileItem>> = content.getFavoriteFiles()
    fun getAllFilesFlow(): Flow<List<FileItem>> = content.getAllFilesFlow()
    suspend fun getFileById(id: String): FileItem? = content.getFileById(id)
    suspend fun insertFile(file: FileEntity) = content.insertFile(file)
    suspend fun insertFiles(files: List<FileEntity>) = content.insertFiles(files)
    suspend fun setFileRead(id: String, isRead: Boolean) = content.setFileRead(id, isRead)
    suspend fun setFileFavorite(id: String, isFavorite: Boolean) = content.setFileFavorite(id, isFavorite)
    suspend fun setFileHidden(id: String, isHidden: Boolean) = content.setFileHidden(id, isHidden)
    suspend fun updateProgress(id: String, page: Int) = content.updateProgress(id, page)
    suspend fun updatePages(id: String, pages: Int) = content.updatePages(id, pages)
    suspend fun updateCoverPath(id: String, path: String) = content.updateCoverPath(id, path)
    suspend fun updateComicInfo(id: String, metadata: ComicInfoMetadata?) = content.updateComicInfo(id, metadata)
    suspend fun getFirstInProgressFile(): FileItem? = content.getFirstInProgressFile()
    suspend fun getInProgressFilesOnce(): List<FileItem> = content.getInProgressFilesOnce()
    suspend fun getAllFilesOnce(): List<FileItem> = content.getAllFilesOnce()
    suspend fun getFilesOnce(folderId: String, includeHidden: Boolean = false): List<FileItem> =
        content.getFilesOnce(folderId, includeHidden)
    fun searchFiles(query: String): Flow<List<FileItem>> = content.searchFiles(query)
    suspend fun getNextFileInFolder(fileId: String): FileItem? = content.getNextFileInFolder(fileId)

    // ── Raízes (SAF) e scan ───────────────────────────────────────────────
    fun getRootPathFlow(): Flow<String?> = content.getRootPathFlow()
    fun getRootPathsFlow(): Flow<List<String>> = content.getRootPathsFlow()
    suspend fun getRootPathsOnce(): List<String> = content.getRootPathsOnce()
    suspend fun addRootPath(uriString: String) = content.addRootPath(uriString)
    suspend fun removeRootPath(uriString: String) = content.removeRootPath(uriString)
    suspend fun initialScan(root: File) = content.initialScan(root)
    suspend fun addRootScan(root: File) = content.addRootScan(root)
    suspend fun rescan(root: File) = content.rescan(root)
    suspend fun rescanMultiple(roots: List<File>) = content.rescanMultiple(roots)

    // ── Capítulos EPUB / paginação ───────────────────────────────────────
    suspend fun getChapterPageCount(fileId: String, chapterIndex: Int, textZoom: Int, width: Int, height: Int): Int? =
        content.getChapterPageCount(fileId, chapterIndex, textZoom, width, height)
    suspend fun saveChapterPageCount(
        fileId: String,
        chapterIndex: Int,
        textZoom: Int,
        width: Int,
        height: Int,
        pageCount: Int,
    ) = content.saveChapterPageCount(fileId, chapterIndex, textZoom, width, height, pageCount)
    suspend fun clearChapterPaginationCache(fileId: String) = content.clearChapterPaginationCache(fileId)
    suspend fun getEpubScrollPercent(fileId: String): Int = content.getEpubScrollPercent(fileId)
    suspend fun saveEpubScrollPercent(fileId: String, chapterIndex: Int, scrollPercent: Int) =
        content.saveEpubScrollPercent(fileId, chapterIndex, scrollPercent)
    suspend fun resetEpubScrollPercent(fileId: String) = content.resetEpubScrollPercent(fileId)
    suspend fun saveEpubPageProgress(fileId: String, globalCurrentPage: Int, totalPages: Int, textZoom: Int) =
        content.saveEpubPageProgress(fileId, globalCurrentPage, totalPages, textZoom)
    suspend fun clearEpubPageProgress(fileId: String) = content.clearEpubPageProgress(fileId)

    // ── Capas e cache ─────────────────────────────────────────────────────
    suspend fun insertCover(cover: com.jotape.inkshelf.data.db.entity.CoverEntity) = content.insertCover(cover)
    suspend fun getCover(fileId: String) = content.getCover(fileId)
    suspend fun deleteCover(fileId: String) = content.deleteCover(fileId)
    suspend fun getTotalCoverSize(): Long = content.getTotalCoverSize()
    suspend fun setFileCoverFromPageImage(fileId: String, imageFile: File): String? =
        content.setFileCoverFromPageImage(fileId, imageFile)
    suspend fun getTotalAppStorageSize(): Long = content.getTotalAppStorageSize()
    suspend fun enforceCacheLimit(excludeFileId: String?) = content.enforceCacheLimit(excludeFileId)
    suspend fun getPageCacheSize(): Long = content.getPageCacheSize()
    suspend fun clearPageCache() = content.clearPageCache()
    suspend fun clearAllCache() = content.clearAllCache()
    suspend fun clearCovers() = content.clearCovers()

    // ── Sugestões da home ────────────────────────────────────────────────
    suspend fun getFavoriteWidgetSummary(): FavoriteWidgetSummary = content.getFavoriteWidgetSummary()
    fun getLibraryProgressSnapshotFlow(): Flow<LibraryProgressSnapshot> = content.getLibraryProgressSnapshotFlow()
    fun getNextSuggestionCandidatesFlow(): Flow<List<NextFileSuggestionCandidate>> = content.getNextSuggestionCandidatesFlow()
    fun getFolderContinuationQueuesFlow(): Flow<List<FolderContinuationQueue>> = content.getFolderContinuationQueuesFlow()
    fun getSuggestedUnreadFoldersFlow(): Flow<List<FolderItem>> = content.getSuggestedUnreadFoldersFlow()
    fun getSuggestedUnreadFolderQueuesFlow(): Flow<List<FolderSuggestionQueue>> = content.getSuggestedUnreadFolderQueuesFlow()
    suspend fun getNextSuggestionCandidate(): NextFileSuggestionCandidate? = content.getNextSuggestionCandidate()
    suspend fun getNextSuggestionCandidates(): List<NextFileSuggestionCandidate> = content.getNextSuggestionCandidates()

    // ── Histórico de leitura ─────────────────────────────────────────────
    fun getReadingHistory(): Flow<List<ReadingHistoryItem>> = statistics.getReadingHistory()
    suspend fun getReadingHistoryCount(): Int = statistics.getReadingHistoryCount()
    suspend fun getLatestReadingHistoryItem(): ReadingHistoryItem? = statistics.getLatestReadingHistoryItem()
    suspend fun addReadingHistory(file: FileItem, pages: Int = file.pages, completedAt: Long = System.currentTimeMillis()) =
        statistics.addReadingHistory(file, pages, completedAt)
    suspend fun clearReadingHistory() = statistics.clearReadingHistory()
    suspend fun deleteReadingHistoryItem(id: Long) = statistics.deleteReadingHistoryItem(id)

    // ── Marcadores de página ─────────────────────────────────────────────
    fun getBookmarks(fileId: String): Flow<List<ReaderBookmark>> = statistics.getBookmarks(fileId)
    suspend fun renameBookmark(fileId: String, pageIndex: Int, label: String?) =
        statistics.renameBookmark(fileId, pageIndex, label)
    fun getAllBookmarkGroups(): Flow<List<BookmarkGroup>> = statistics.getAllBookmarkGroups()
    suspend fun addBookmark(fileId: String, pageIndex: Int) = statistics.addBookmark(fileId, pageIndex)
    suspend fun removeBookmark(fileId: String, pageIndex: Int) = statistics.removeBookmark(fileId, pageIndex)
    suspend fun isPageBookmarked(fileId: String, pageIndex: Int): Boolean = statistics.isPageBookmarked(fileId, pageIndex)
    suspend fun toggleBookmark(fileId: String, pageIndex: Int) = statistics.toggleBookmark(fileId, pageIndex)

    // ── Estatísticas ──────────────────────────────────────────────────────
    suspend fun getReadingStatisticsTrackedDayCount(): Int = statistics.getReadingStatisticsTrackedDayCount()
    suspend fun clearReadingStatistics() = statistics.clearReadingStatistics()
    fun getDailyStatisticPointsFlow(): Flow<List<DailyStatisticPoint>> = statistics.getDailyStatisticPointsFlow()
    fun getReadingSessionStatsFlow(): Flow<List<ReadingSessionStat>> = statistics.getReadingSessionStatsFlow()
    suspend fun recordAppUsage(durationMillis: Long, endedAt: Long = System.currentTimeMillis()) =
        statistics.recordAppUsage(durationMillis, endedAt)
    suspend fun recordReadingSession(
        fileId: String?,
        title: String?,
        startedAt: Long,
        endedAt: Long,
        durationMillis: Long,
        pagesRead: Int,
    ) = statistics.recordReadingSession(fileId, title, startedAt, endedAt, durationMillis, pagesRead)
    suspend fun recordCompletedFileInStatistics(completedAt: Long = System.currentTimeMillis()) =
        statistics.recordCompletedFileInStatistics(completedAt)

    // ── Configurações (preferências) ─────────────────────────────────────
    suspend fun getSetting(key: String): String? = settings.getSetting(key)
    suspend fun setSetting(key: String, value: String) = settings.setSetting(key, value)
    fun getColumnsFlow(): Flow<Int> = settings.getColumnsFlow()
    fun getViewModeFlow(): Flow<LibraryViewMode> = settings.getViewModeFlow()
    suspend fun getViewMode(): LibraryViewMode = settings.getViewMode()
    fun getSortModeFlow(): Flow<LibrarySortMode> = settings.getSortModeFlow()
    suspend fun setSortMode(mode: LibrarySortMode) = settings.setSortMode(mode)
    fun getCacheLimitMbFlow(): Flow<Int> = settings.getCacheLimitMbFlow()
    suspend fun getCacheLimitMb(): Int = settings.getCacheLimitMb()
    suspend fun setCacheLimitMb(mb: Int) = settings.setCacheLimitMb(mb)
    fun getDarkModeFlow(): Flow<Boolean> = settings.getDarkModeFlow()
    fun getThemeIdFlow(): Flow<String> = settings.getThemeIdFlow()
    fun getIconAliasFlow(): Flow<String?> = settings.getIconAliasFlow()
    fun getHeroCardStyleFlow(): Flow<String> = settings.getHeroCardStyleFlow()
    fun getFolderCardStyleFlow(): Flow<String> = settings.getFolderCardStyleFlow()
    fun getReadingHistoryEnabledFlow(): Flow<Boolean> = settings.getReadingHistoryEnabledFlow()
    fun getCharactersEnabledFlow(): Flow<Boolean> = settings.getCharactersEnabledFlow()
    fun getReadingStatisticsEnabledFlow(): Flow<Boolean> = settings.getReadingStatisticsEnabledFlow()
    fun getInterfaceDensityFlow(): Flow<String> = settings.getInterfaceDensityFlow()
    fun getFontScaleFlow(): Flow<String> = settings.getFontScaleFlow()
    fun getCardSizeFlow(): Flow<String> = settings.getCardSizeFlow()
    fun getItemSpacingFlow(): Flow<String> = settings.getItemSpacingFlow()
    fun getHeaderStyleFlow(): Flow<String> = settings.getHeaderStyleFlow()
    fun getCoverCornersFlow(): Flow<String> = settings.getCoverCornersFlow()
    fun getCustomCoverCornersProgressFlow(): Flow<Float> = settings.getCustomCoverCornersProgressFlow()
    fun getCustomCoverCornersEnabledFlow(): Flow<Boolean> = settings.getCustomCoverCornersEnabledFlow()
    fun getAnimationModeFlow(): Flow<String> = settings.getAnimationModeFlow()
    fun getTransitionStyleFlow(): Flow<String> = settings.getTransitionStyleFlow()
    fun getHomeScreenEnabledFlow(): Flow<Boolean> = settings.getHomeScreenEnabledFlow()
    fun getHomeSectionOrderFlow(): Flow<List<HomeSectionType>> = settings.getHomeSectionOrderFlow()
    fun getHomeHiddenSectionsFlow(): Flow<Set<HomeSectionType>> = settings.getHomeHiddenSectionsFlow()
    suspend fun setHomeSectionOrder(sectionOrder: List<HomeSectionType>) = settings.setHomeSectionOrder(sectionOrder)
    suspend fun setHomeHiddenSections(hiddenSections: Set<HomeSectionType>) = settings.setHomeHiddenSections(hiddenSections)
    fun getResumeReadingNotificationsEnabledFlow(): Flow<Boolean> = settings.getResumeReadingNotificationsEnabledFlow()
    suspend fun areResumeReadingNotificationsEnabled(): Boolean = settings.areResumeReadingNotificationsEnabled()
    fun getNextFileNotificationsEnabledFlow(): Flow<Boolean> = settings.getNextFileNotificationsEnabledFlow()
    suspend fun areNextFileNotificationsEnabled(): Boolean = settings.areNextFileNotificationsEnabled()
    suspend fun areReadingStatisticsEnabled(): Boolean = settings.areReadingStatisticsEnabled()
    fun getShelfTiltDegreesFlow(): Flow<Float> = settings.getShelfTiltDegreesFlow()
    fun getAdaptiveShelfTiltEnabledFlow(): Flow<Boolean> = settings.getAdaptiveShelfTiltEnabledFlow()
    suspend fun resetSettings() = settings.resetSettings()

    // ── Operações que cruzam domínios (zeram múltiplas tabelas de uma vez) ─
    suspend fun clearAll() {
        content.clearAllLibraryData()
        statistics.clearHistoryAndStatistics()
        settings.resetSettings()
    }

    suspend fun clearReaderCache() {
        content.clearAllLibraryData()
        statistics.clearHistoryAndStatistics()
        fileCharacterTagDao.deleteAll()
        fileTeamTagDao.deleteAll()
        settingsDao.deleteKey(LibrarySettingsKeys.SETTING_ROOT_PATHS)
        settingsDao.deleteKey(LibrarySettingsKeys.SETTING_ROOT_PATH)
    }

}