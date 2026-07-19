package com.jotape.inkshelf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jotape.inkshelf.ui.viewmodel.LeitorUiState

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val ZOOM_STEP = 0.25f

/**
 * Leitor de páginas.
 *
 * Nota de porte: a tela do mobile tem 2.897 linhas construídas em torno de gestos de toque —
 * zonas de toque configuráveis, pinça, deslize horizontal. Nada disso existe no desktop. Aqui as
 * entradas são as que um usuário de Windows espera:
 *
 * | Ação            | Entrada                                   |
 * |-----------------|-------------------------------------------|
 * | virar página    | roda do mouse, setas, PageUp/PageDown, espaço |
 * | primeira/última | Home / End                                |
 * | zoom            | Ctrl + roda, ou `+` / `-`                 |
 * | mover ampliado  | arrastar com o mouse                      |
 * | ajustar         | duplo clique                              |
 * | tela cheia      | F11                                       |
 * | sair            | Esc                                       |
 */
@Composable
fun LeitorScreen(
    state: LeitorUiState,
    isFullscreen: Boolean,
    onClose: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onFirstPage: () -> Unit,
    onLastPage: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onViewportChanged: (width: Int, height: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Toda troca de página volta ao enquadramento original: manter o zoom da página anterior
    // deixaria a próxima começando num recorte arbitrário.
    LaunchedEffect(state.currentPage) {
        zoom = 1f
        offset = Offset.Zero
    }

    // Sem isto o teclado não chega à tela — nada teria foco ao abrir o leitor.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun resetZoom() {
        zoom = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { onViewportChanged(it.width, it.height) }
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when {
                    event.key == Key.Escape -> { onClose(); true }
                    event.key == Key.F11 -> { onToggleFullscreen(); true }
                    event.key == Key.MoveHome -> { onFirstPage(); true }
                    event.key == Key.MoveEnd -> { onLastPage(); true }
                    event.key in NEXT_KEYS -> { onNextPage(); true }
                    event.key in PREVIOUS_KEYS -> { onPreviousPage(); true }
                    event.key == Key.Equals || event.key == Key.Plus -> {
                        zoom = (zoom + ZOOM_STEP).coerceAtMost(MAX_ZOOM); true
                    }
                    event.key == Key.Minus -> {
                        zoom = (zoom - ZOOM_STEP).coerceAtLeast(MIN_ZOOM)
                        if (zoom == MIN_ZOOM) offset = Offset.Zero
                        true
                    }
                    else -> false
                }
            }
            // Roda do mouse: vira a página; com Ctrl, aplica zoom — a convenção de qualquer
            // visualizador de imagem ou documento no Windows.
            .pointerInput(state.currentPage) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scroll == 0f) continue

                        val ctrlPressed = event.keyboardModifiers.isCtrlPressed
                        if (ctrlPressed) {
                            zoom = (zoom - scroll * ZOOM_STEP).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            if (zoom == MIN_ZOOM) offset = Offset.Zero
                        } else if (scroll > 0) {
                            onNextPage()
                        } else {
                            onPreviousPage()
                        }
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { resetZoom() })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Arrastar só faz sentido ampliado; sem zoom não há o que revelar.
                    if (zoom > MIN_ZOOM) {
                        offset += dragAmount
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.error != null -> Text(
                text = state.error,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )

            state.currentImage != null -> AsyncImage(
                model = state.currentImage,
                contentDescription = "Página ${state.currentPage + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )

            else -> CircularProgressIndicator(color = Color.White)
        }

        if (state.isLoading && state.currentImage != null) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
        }

        ReaderTopBar(
            state = state,
            isFullscreen = isFullscreen,
            onClose = onClose,
            onToggleFullscreen = onToggleFullscreen,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun ReaderTopBar(
    state: LeitorUiState,
    isFullscreen: Boolean,
    onClose: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Fechar leitor",
                tint = Color.White,
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = state.title.substringBeforeLast('.'),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.pageLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        IconButton(onClick = onToggleFullscreen) {
            Icon(
                if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullscreen) "Sair da tela cheia" else "Tela cheia",
                tint = Color.White,
            )
        }
    }
}

private val NEXT_KEYS = setOf(
    Key.DirectionRight,
    Key.DirectionDown,
    Key.PageDown,
    Key.Spacebar,
)

private val PREVIOUS_KEYS = setOf(
    Key.DirectionLeft,
    Key.DirectionUp,
    Key.PageUp,
)
