package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity

/**
 * Marcador de página (Parte 9). Chave composta (fileId, pageIndex) para que cada
 * página seja marcada no máximo uma vez e o toggle seja idempotente.
 */
@Entity(tableName = "bookmarks", primaryKeys = ["fileId", "pageIndex"])
data class BookmarkEntity(
    val fileId: String,
    val pageIndex: Int,
    val createdAt: Long,
    val label: String? = null,
)
