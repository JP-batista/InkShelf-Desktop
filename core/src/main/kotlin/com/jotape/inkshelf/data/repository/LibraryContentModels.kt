package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.model.FileItem
import com.jotape.inkshelf.model.FolderItem

/**
 * Tipos que o repositório de conteúdo produz para alimentar a tela inicial.
 *
 * Nota de porte: no Android eram classes aninhadas dentro da fachada `LibraryRepository`, apesar
 * de quem os constrói ser o repositório de conteúdo. Aqui ficam junto de quem os produz.
 */

/** "Você terminou X — o próximo é Y", a sugestão de continuidade dentro de uma pasta. */
data class NextFileSuggestionCandidate(
    val completedFile: FileItem,
    val nextFile: FileItem,
    val folderTitle: String?,
)

/** Pasta já iniciada e os arquivos que ainda faltam nela. */
data class FolderContinuationQueue(
    val folder: FolderItem,
    val pendingFiles: List<FileItem>,
)

/** Pasta ainda não iniciada, sugerida para começar. */
data class FolderSuggestionQueue(
    val folder: FolderItem,
    val suggestedFiles: List<FileItem>,
)

/** Contagem de favoritos. */
data class FavoriteWidgetSummary(
    val favoriteFiles: Int,
    val favoriteFolders: Int,
) {
    val totalItems: Int
        get() = favoriteFiles + favoriteFolders
}
