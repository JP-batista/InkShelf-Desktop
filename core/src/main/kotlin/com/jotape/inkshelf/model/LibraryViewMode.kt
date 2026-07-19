package com.jotape.inkshelf.model


enum class LibraryViewMode(
    val id: String,
    val label: String,
) {
    GRID(
        id = "grid",
        label = "Grade",
    ),
    LIST(
        id = "list",
        label = "Lista",
    ),
    SHELF(
        id = "shelf",
        label = "Estante",
    ),
    ;

    val supportsColumns: Boolean
        get() = this != LIST

    companion object {
        fun fromStoredValue(
            value: String?,
            legacyGridMode: String? = null,
        ): LibraryViewMode {
            entries.firstOrNull { it.id == value }?.let { return it }

            return when (legacyGridMode?.toBooleanStrictOrNull()) {
                true -> GRID
                false -> LIST
                null -> GRID
            }
        }
    }
}
