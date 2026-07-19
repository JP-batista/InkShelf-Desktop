package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_pagination_cache",
    indices = [Index(
        value = ["fileId", "chapterIndex", "textZoom", "viewportWidth", "viewportHeight"],
        unique = true,
    )],
)
data class ChapterPaginationCacheEntity(
    val fileId: String,
    val chapterIndex: Int,
    val textZoom: Int,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val pageCount: Int,
    val cachedAt: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
