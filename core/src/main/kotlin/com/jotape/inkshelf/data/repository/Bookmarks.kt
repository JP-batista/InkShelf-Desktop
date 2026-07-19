package com.jotape.inkshelf.data.repository

/**
 * Nota de porte: no Android estes dois tipos eram declarados no topo do arquivo da fachada
 * `LibraryRepository`, embora só o repositório de estatísticas os produza. Aqui moram junto de
 * quem os usa.
 */

/** Marcador de página exposto à UI: índice da página + rótulo opcional. */
data class ReaderBookmark(val pageIndex: Int, val label: String?)

/** Marcadores agrupados por arquivo, para a tela de "todos os marcadores". */
data class BookmarkGroup(
    val fileId: String,
    val title: String?,
    val coverPath: String?,
    val bookmarks: List<ReaderBookmark>,
)
