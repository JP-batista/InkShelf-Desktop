package com.jotape.inkshelf.model


enum class LibrarySortMode(val id: String, val label: String) {
    NAME_ASC("name_asc", "Nome (A→Z)"),
    NAME_DESC("name_desc", "Nome (Z→A)"),
    MOST_ITEMS("most_items", "Mais itens"),
    MOST_RECENT("most_recent", "Mais recentes"),
    ;

    companion object {
        val DEFAULT = NAME_ASC

        fun fromId(id: String?): LibrarySortMode =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
