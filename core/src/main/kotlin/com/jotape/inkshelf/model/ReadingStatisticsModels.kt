package com.jotape.inkshelf.model

data class DailyStatisticPoint(
    val dayKey: String,
    val appUsageMillis: Long,
    val readingMillis: Long,
    val pagesRead: Int,
    val filesCompleted: Int,
    val sessionCount: Int,
)

data class ReadingSessionStat(
    val id: Long,
    val fileId: String?,
    val title: String?,
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val pagesRead: Int,
)

data class LibraryProgressSnapshot(
    val totalFiles: Int,
    val readFiles: Int,
    val inProgressFiles: Int,
    val notStartedFiles: Int,
    val favoriteReadFiles: Int,
    val favoriteUnreadFiles: Int,
    val totalLibraryPages: Int,
    val readLibraryPages: Int,
    val formats: List<FormatBreakdown>,
    val libraryReadProgress: Float,
)

/** Distribuição de arquivos por formato (CBZ, CBR, PDF, EPUB, …). */
data class FormatBreakdown(
    val format: String,
    val total: Int,
    val read: Int,
)
