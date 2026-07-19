package com.jotape.inkshelf.model

data class ReadingHistoryItem(
    val id: Long,
    val fileId: String,
    val title: String,
    val coverPath: String? = null,
    val folderName: String? = null,
    val pages: Int = 0,
    val completedAt: Long,
)
