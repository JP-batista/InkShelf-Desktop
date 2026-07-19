package com.jotape.inkshelf.model

data class ComicInfoMetadata(
    val title: String? = null,
    val series: String? = null,
    val number: String? = null,
    val writer: String? = null,
    val penciller: String? = null,
    val artist: String? = null,
    val colorist: String? = null,
    val publisher: String? = null,
    val year: Int? = null,
    val summary: String? = null,
) {
    val hasAnyValue: Boolean
        get() = listOf(
            title,
            series,
            number,
            writer,
            penciller,
            artist,
            colorist,
            publisher,
            summary,
        ).any { !it.isNullOrBlank() } || year != null

    val issueLabel: String?
        get() = when {
            !series.isNullOrBlank() && !number.isNullOrBlank() -> "$series #$number"
            !series.isNullOrBlank() -> series
            !title.isNullOrBlank() -> title
            else -> null
        }
}
