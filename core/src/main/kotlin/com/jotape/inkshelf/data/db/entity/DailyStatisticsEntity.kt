package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_statistics")
data class DailyStatisticsEntity(
    @PrimaryKey
    val dayKey: String,
    val appUsageMillis: Long = 0L,
    val readingMillis: Long = 0L,
    val pagesRead: Int = 0,
    val filesCompleted: Int = 0,
    val sessionCount: Int = 0,
)
