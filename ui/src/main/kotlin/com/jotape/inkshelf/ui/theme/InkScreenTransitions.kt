package com.jotape.inkshelf.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset

/**
 * Constrói as transições de tela de forma unificada, a partir do
 * [InkTransitionStyle] escolhido. Usado tanto pelo NavHost (telas de topo)
 * quanto pela navegação interna das Configurações, garantindo que TODAS as
 * telas — incluindo o retorno — usem exatamente o mesmo movimento.
 *
 * [forward] = true quando se avança na hierarquia (drill-down); false ao voltar.
 * [slidePx] = deslocamento em pixels para estilos "sutis" (eixo H/V).
 */
fun InkTransitionStyle.enterTransition(forward: Boolean, slidePx: Int, durationMs: Int): EnterTransition {
    val dir = if (forward) 1 else -1
    val spec = tween<Float>(durationMs)
    val intSpec = tween<IntOffset>(durationMs)
    return when (this) {
        InkTransitionStyle.SHARED_AXIS ->
            slideInHorizontally(intSpec) { dir * slidePx } + fadeIn(spec)
        InkTransitionStyle.SLIDE ->
            slideInHorizontally(intSpec) { full -> dir * full } + fadeIn(spec)
        InkTransitionStyle.VERTICAL ->
            slideInVertically(intSpec) { dir * slidePx } + fadeIn(spec)
        InkTransitionStyle.FADE ->
            fadeIn(spec)
        InkTransitionStyle.FADE_THROUGH ->
            fadeIn(spec) + scaleIn(spec, initialScale = 0.92f)
        InkTransitionStyle.ZOOM ->
            fadeIn(spec) + scaleIn(spec, initialScale = if (forward) 0.85f else 1.1f)
    }
}

fun InkTransitionStyle.exitTransition(forward: Boolean, slidePx: Int, durationMs: Int): ExitTransition {
    val dir = if (forward) 1 else -1
    val spec = tween<Float>(durationMs)
    val intSpec = tween<IntOffset>(durationMs)
    return when (this) {
        InkTransitionStyle.SHARED_AXIS ->
            slideOutHorizontally(intSpec) { -dir * slidePx } + fadeOut(spec)
        InkTransitionStyle.SLIDE ->
            // A tela que sai desliza um pouco menos, criando profundidade.
            slideOutHorizontally(intSpec) { full -> -dir * full / 6 } + fadeOut(spec)
        InkTransitionStyle.VERTICAL ->
            slideOutVertically(intSpec) { -dir * slidePx } + fadeOut(spec)
        InkTransitionStyle.FADE ->
            fadeOut(spec)
        InkTransitionStyle.FADE_THROUGH ->
            fadeOut(spec) + scaleOut(spec, targetScale = 0.92f)
        InkTransitionStyle.ZOOM ->
            fadeOut(spec) + scaleOut(spec, targetScale = if (forward) 1.1f else 0.85f)
    }
}
