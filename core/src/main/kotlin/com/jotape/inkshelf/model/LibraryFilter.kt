package com.jotape.inkshelf.model


enum class LibraryFilter(
    val label: String,
) {
    ALL("Todos"),
    UNREAD("Não lidos"),
    READ("Lidos"),
    HIDDEN("Ocultados"),
}
