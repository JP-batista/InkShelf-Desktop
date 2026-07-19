package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fileId: String? = null,
    val title: String? = null,
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val pagesRead: Int = 0,
)
