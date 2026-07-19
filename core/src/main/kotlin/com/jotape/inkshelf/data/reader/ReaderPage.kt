package com.jotape.inkshelf.data.reader

/**
 * Uma página do arquivo aberto, ainda não extraída — é o endereço dela dentro do arquivo.
 *
 * Nota de porte: no Android cada variante carregava dois caminhos, `archivePath` (a cópia do
 * arquivo no cache) e `sourceUri` (o original no SAF). Isso existia porque um `Uri` do SAF não
 * permite acesso aleatório, então o arquivo inteiro precisava ser copiado para o cache antes de
 * ser aberto. No desktop o original já é um arquivo comum no disco: os dois viram um só
 * [sourcePath], e a cópia deixa de existir.
 */
sealed class ReaderPage {
    abstract val index: Int
    abstract val fileId: String

    /** Caminho do arquivo original no disco (o `.cbz`/`.cbr`/`.pdf`). */
    abstract val sourcePath: String

    data class ZipPage(
        override val index: Int,
        override val fileId: String,
        override val sourcePath: String,
        val entryName: String,
    ) : ReaderPage()

    data class RarPage(
        override val index: Int,
        override val fileId: String,
        override val sourcePath: String,
        val entryName: String,
    ) : ReaderPage()

    data class Rar5Page(
        override val index: Int,
        override val fileId: String,
        override val sourcePath: String,
        val entryIndex: Int,
        val entryName: String,
    ) : ReaderPage()

    data class PdfPage(
        override val index: Int,
        override val fileId: String,
        override val sourcePath: String,
    ) : ReaderPage()
}
