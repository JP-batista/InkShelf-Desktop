package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.db.entity.BookmarkEntity
import com.jotape.inkshelf.data.db.entity.DailyStatisticsEntity
import com.jotape.inkshelf.data.db.entity.ReadingHistoryEntity
import com.jotape.inkshelf.data.db.entity.ReadingSessionEntity
import com.jotape.inkshelf.model.DailyStatisticPoint
import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.ReadingHistoryItem
import com.jotape.inkshelf.model.ReadingSessionStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Atividade do usuário: histórico de leitura, marcadores de página e estatísticas
 * (sessões/uso diário). Os interruptores que ligam/desligam essas features
 * (histórico, estatísticas) continuam sendo checados via [settings], não duplicados aqui.
 */
class LibraryStatisticsRepository(
    dbName: String = InkPaths.MAIN_DB_NAME,
    private val settings: LibrarySettingsRepository,
) {

    private val db = InkShelfDatabase.getInstance(dbName)
    private val folderDao = db.folderDao()
    private val readingHistoryDao = db.readingHistoryDao()
    private val statisticsDao = db.statisticsDao()
    private val bookmarkDao = db.bookmarkDao()

    fun getReadingHistory(): Flow<List<ReadingHistoryItem>> =
        readingHistoryDao.getAll().map { list ->
            list.map { entry ->
                ReadingHistoryItem(
                    id = entry.id,
                    fileId = entry.fileId,
                    title = entry.title,
                    coverPath = entry.coverPath,
                    folderName = entry.folderName,
                    pages = entry.pages,
                    completedAt = entry.completedAt,
                )
            }
        }

    suspend fun getReadingHistoryCount(): Int = readingHistoryDao.count()

    suspend fun getReadingStatisticsTrackedDayCount(): Int = statisticsDao.countTrackedDays()

    suspend fun getLatestReadingHistoryItem(): ReadingHistoryItem? =
        readingHistoryDao.getLatest()?.toModel()

    suspend fun addReadingHistory(
        file: FileItem,
        pages: Int = file.pages,
        completedAt: Long = System.currentTimeMillis(),
    ) {
        if (!settings.isReadingHistoryEnabled()) return

        val folderName = folderDao.getFolderById(file.folderId)?.name
        readingHistoryDao.insert(
            ReadingHistoryEntity(
                fileId = file.id,
                title = file.title,
                coverPath = file.coverPath,
                folderName = folderName,
                pages = pages,
                completedAt = completedAt,
            ),
        )
    }

    suspend fun clearReadingHistory() = readingHistoryDao.deleteAll()

    suspend fun deleteReadingHistoryItem(id: Long) = readingHistoryDao.deleteById(id)

    /** Marcadores de um arquivo (página + rótulo opcional), como Flow. */
    fun getBookmarks(fileId: String): Flow<List<ReaderBookmark>> =
        bookmarkDao.getForFile(fileId).map { list ->
            list.map { ReaderBookmark(pageIndex = it.pageIndex, label = it.label) }
        }

    suspend fun renameBookmark(fileId: String, pageIndex: Int, label: String?) =
        bookmarkDao.updateLabel(fileId, pageIndex, label?.trim()?.ifBlank { null })

    /** Todos os marcadores agrupados por arquivo (arquivos disponíveis primeiro, por nome). */
    fun getAllBookmarkGroups(): Flow<List<BookmarkGroup>> =
        bookmarkDao.getAllWithFile().map { rows ->
            rows.groupBy { it.fileId }
                .map { (fileId, list) ->
                    BookmarkGroup(
                        fileId = fileId,
                        title = list.first().fileName,
                        coverPath = list.first().coverPath,
                        bookmarks = list.sortedBy { it.pageIndex }
                            .map { ReaderBookmark(it.pageIndex, it.label) },
                    )
                }
                .sortedWith(compareBy({ it.title == null }, { it.title?.lowercase() ?: "" }))
        }

    suspend fun addBookmark(fileId: String, pageIndex: Int) {
        bookmarkDao.insert(
            BookmarkEntity(fileId = fileId, pageIndex = pageIndex, createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun removeBookmark(fileId: String, pageIndex: Int) =
        bookmarkDao.delete(fileId, pageIndex)

    suspend fun isPageBookmarked(fileId: String, pageIndex: Int): Boolean =
        bookmarkDao.countAt(fileId, pageIndex) > 0

    /** Adiciona se ausente, remove se presente. */
    suspend fun toggleBookmark(fileId: String, pageIndex: Int) {
        if (bookmarkDao.countAt(fileId, pageIndex) > 0) {
            bookmarkDao.delete(fileId, pageIndex)
        } else {
            addBookmark(fileId, pageIndex)
        }
    }

    suspend fun clearReadingStatistics() {
        statisticsDao.deleteAllReadingSessions()
        statisticsDao.deleteAllDailyStatistics()
    }

    fun getDailyStatisticPointsFlow(): Flow<List<DailyStatisticPoint>> =
        statisticsDao.getDailyStatistics().map { list ->
            list.map { stat ->
                DailyStatisticPoint(
                    dayKey = stat.dayKey,
                    appUsageMillis = stat.appUsageMillis,
                    readingMillis = stat.readingMillis,
                    pagesRead = stat.pagesRead,
                    filesCompleted = stat.filesCompleted,
                    sessionCount = stat.sessionCount,
                )
            }
        }

    fun getReadingSessionStatsFlow(): Flow<List<ReadingSessionStat>> =
        statisticsDao.getReadingSessions().map { list ->
            list.map { session ->
                ReadingSessionStat(
                    id = session.id,
                    fileId = session.fileId,
                    title = session.title,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    durationMillis = session.durationMillis,
                    pagesRead = session.pagesRead,
                )
            }
        }

    suspend fun recordAppUsage(
        durationMillis: Long,
        endedAt: Long = System.currentTimeMillis(),
    ) {
        if (durationMillis <= 0L || !settings.areReadingStatisticsEnabled()) return
        val dayKey = statisticsDayKey(endedAt)
        upsertDailyStatistic(dayKey) { current ->
            current.copy(appUsageMillis = current.appUsageMillis + durationMillis)
        }
    }

    suspend fun recordReadingSession(
        fileId: String?,
        title: String?,
        startedAt: Long,
        endedAt: Long,
        durationMillis: Long,
        pagesRead: Int,
    ) {
        if (durationMillis <= 0L) return

        // A sessão completa e os agregados diários seguem atrás do opt-in de estatísticas.
        if (!settings.areReadingStatisticsEnabled()) return

        statisticsDao.insertReadingSession(
            ReadingSessionEntity(
                fileId = fileId,
                title = title,
                startedAt = startedAt,
                endedAt = endedAt,
                durationMillis = durationMillis,
                pagesRead = pagesRead.coerceAtLeast(0),
            ),
        )

        val dayKey = statisticsDayKey(endedAt)
        upsertDailyStatistic(dayKey) { current ->
            current.copy(
                readingMillis = current.readingMillis + durationMillis,
                pagesRead = current.pagesRead + pagesRead.coerceAtLeast(0),
                sessionCount = current.sessionCount + 1,
            )
        }
    }

    suspend fun recordCompletedFileInStatistics(
        completedAt: Long = System.currentTimeMillis(),
    ) {
        if (!settings.areReadingStatisticsEnabled()) return
        val dayKey = statisticsDayKey(completedAt)
        upsertDailyStatistic(dayKey) { current ->
            current.copy(filesCompleted = current.filesCompleted + 1)
        }
    }

    /** Usado só pela fachada, ao zerar tudo (`clearAll`/`clearReaderCache`). */
    suspend fun clearHistoryAndStatistics() {
        readingHistoryDao.deleteAll()
        bookmarkDao.deleteAll()
        statisticsDao.deleteAllReadingSessions()
        statisticsDao.deleteAllDailyStatistics()
    }

    private suspend fun upsertDailyStatistic(
        dayKey: String,
        transform: (DailyStatisticsEntity) -> DailyStatisticsEntity,
    ) {
        val current = statisticsDao.getDailyStatistic(dayKey)
            ?: DailyStatisticsEntity(dayKey = dayKey)
        statisticsDao.upsertDailyStatistic(transform(current))
    }

    private fun statisticsDayKey(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    private fun ReadingHistoryEntity.toModel() = ReadingHistoryItem(
        id = id,
        fileId = fileId,
        title = title,
        coverPath = coverPath,
        folderName = folderName,
        pages = pages,
        completedAt = completedAt,
    )
}
