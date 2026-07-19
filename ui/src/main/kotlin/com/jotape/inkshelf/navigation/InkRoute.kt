package com.jotape.inkshelf.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Rotas da navegação.
 *
 * Nota de porte: a lista do mobile tem 16 rotas. Aqui entram as que já têm tela; as demais
 * chegam com as respectivas fases. Ficam de fora permanentemente as de EPUB e as de snapshot
 * enquanto essas features não forem portadas.
 */
sealed class InkRoute(val route: String) {
    data object Biblioteca : InkRoute("biblioteca")
    data object Favoritos : InkRoute("favoritos")
    data object ContinuarLendo : InkRoute("continuar_lendo")
    data object Configuracoes : InkRoute("configuracoes")

    data object Leitor : InkRoute("leitor/{fileId}?page={page}") {
        fun createRoute(fileId: String, page: Int = -1) = "leitor/${encode(fileId)}?page=$page"
    }
}

/**
 * O `fileId` é um caminho do Windows — contém `\`, `:` e espaços, que quebrariam a URL da rota.
 * (No mobile isso não aparecia: lá o id já era Base64 do SAF.)
 */
internal fun encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)
