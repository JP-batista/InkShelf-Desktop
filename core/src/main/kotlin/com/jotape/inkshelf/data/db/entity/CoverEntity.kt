package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "covers")
data class CoverEntity(
    @PrimaryKey
    val fileId: String,
    val thumbnailPath: String,
    val sizeBytes: Long     = 0L,
    val createdAt: Long     = System.currentTimeMillis(),
)