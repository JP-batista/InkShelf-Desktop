package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        // Navegação (WHERE parentId = ? ORDER BY name).
        Index(value = ["parentId", "name"]),
        Index(value = ["isFavorite"]),
    ],
)
data class FolderEntity(
    @PrimaryKey
    val id: String,
    val parentId: String?,
    val name: String,
    val customCoverId: String? = null,
    val itemCount: Int = 0,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val coverPath: String? = null,
    val lastModifiedAt: Long = 0L,
)
