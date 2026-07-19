package com.jotape.inkshelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jotape.inkshelf.data.repository.LibraryRepository
import com.jotape.inkshelf.di.appModule
import com.jotape.inkshelf.ui.InkShelfApp
import com.jotape.inkshelf.ui.theme.InkShelfTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1180.dp, 800.dp))
    val isFullscreen = windowState.placement == WindowPlacement.Fullscreen

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "InkShelf",
    ) {
        KoinApplication(application = { modules(appModule) }) {
            InkShelfRoot(
                isFullscreen = isFullscreen,
                onToggleFullscreen = {
                    windowState.placement = if (isFullscreen) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Fullscreen
                    }
                },
            )
        }
    }
}

@Composable
private fun InkShelfRoot(isFullscreen: Boolean, onToggleFullscreen: () -> Unit) {
    val repository = koinInject<LibraryRepository>()

    // O tema vem do banco, então acompanha o que o usuário escolheu — os mesmos 8 presets e o
    // mesmo par claro/escuro do app mobile.
    val darkTheme by repository.getDarkModeFlow().collectAsState(initial = true)
    val themeId by repository.getThemeIdFlow().collectAsState(initial = "red")

    InkShelfTheme(darkTheme = darkTheme, themeId = themeId) {
        InkShelfApp(isFullscreen = isFullscreen, onToggleFullscreen = onToggleFullscreen)
    }
}
