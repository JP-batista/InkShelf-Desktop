package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history")
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fileId: String,
    val title: String,
    val coverPath: String? = null,
    val folderName: String? = null,
    val pages: Int = 0,
    val completedAt: Long,
)
