package com.jotape.inkshelf.model

sealed class LibraryItem {
    abstract val id: String
    abstract val title: String
    abstract val isFavorite: Boolean
    abstract val isHidden: Boolean
}

data class FolderItem(
    override val id: String,
    override val title: String,
    override val isFavorite: Boolean = false,
    override val isHidden: Boolean = false,
    val parentId: String? = null,
    val itemCount: Int = 0,
    val progress: Float = 0f,
    val coverPath: String? = null,
    val customCoverId: String? = null,
    val lastModifiedAt: Long = 0L,
) : LibraryItem() {
    val isRead: Boolean
        get() = itemCount > 0 && progress >= 0.999f
    val isInProgress: Boolean
        get() = progress > 0f && !isRead
}

data class FileItem(
    override val id: String,
    override val title: String,
    override val isFavorite: Boolean = false,
    override val isHidden: Boolean = false,
    val pages: Int = 0,
    val currentPage: Int = 0,
    val isRead: Boolean = false,
    val coverPath: String? = null,
    val folderId: String = "",
    val comicInfo: ComicInfoMetadata? = null,
    val comicInfoScanned: Boolean = false,
    val epubGlobalCurrentPage: Int = 0,
    val epubTotalPages: Int = 0,
    val epubLastTextZoom: Int = 0,
) : LibraryItem() {
    val isEpub: Boolean
        get() = title.endsWith(".epub", ignoreCase = true)
    val progress: Float
        get() = if (pages > 0) currentPage.toFloat() / pages else 0f
    val isInProgress: Boolean
        get() = currentPage > 0 && currentPage < pages && !isRead
    val progressLabel: String?
        get() = when {
            isEpub && epubTotalPages > 0 -> "Pág. ${epubGlobalCurrentPage + 1} de $epubTotalPages"
            isEpub && epubGlobalCurrentPage > 0 -> "Pág. ${epubGlobalCurrentPage + 1} / Calculando..."
            isEpub -> "Calculando..."
            else -> null
        }
}
