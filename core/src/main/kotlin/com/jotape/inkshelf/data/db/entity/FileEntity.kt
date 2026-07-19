package com.jotape.inkshelf.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "files",
    indices = [
        // Navegação por pasta (WHERE folderId = ? [AND isHidden = 0] ORDER BY name):
        // sem este índice cada abertura de pasta varre a tabela inteira (30k+ linhas).
        Index(value = ["folderId", "name"]),
        Index(value = ["isFavorite"]),
        // "Continuar lendo" (WHERE currentPage > 0 AND ... isRead = 0).
        Index(value = ["isRead", "currentPage"]),
    ],
)
data class FileEntity(
    @PrimaryKey
    val id: String,
    val folderId: String,
    val name: String,
    val coverPath: String? = null,
    val pages: Int = 0,
    val currentPage: Int = 0,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val comicInfoTitle: String? = null,
    val comicInfoSeries: String? = null,
    val comicInfoNumber: String? = null,
    val comicInfoWriter: String? = null,
    val comicInfoPenciller: String? = null,
    val comicInfoArtist: String? = null,
    val comicInfoColorist: String? = null,
    val comicInfoPublisher: String? = null,
    val comicInfoYear: Int? = null,
    val comicInfoSummary: String? = null,
    val comicInfoScanned: Boolean = false,
    val epubScrollPercent: Int = 0,
    val epubGlobalCurrentPage: Int = 0,
    val epubTotalPages: Int = 0,
    val epubLastTextZoom: Int = 0,
)
