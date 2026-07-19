package com.jotape.inkshelf.di

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.cover.CoverExtractor
import com.jotape.inkshelf.data.cover.CoverGenerator
import com.jotape.inkshelf.data.metadata.ComicInfoExtractor
import com.jotape.inkshelf.data.reader.PageExtractor
import com.jotape.inkshelf.data.repository.LibraryRepository
import com.jotape.inkshelf.ui.viewmodel.BibliotecaViewModel
import com.jotape.inkshelf.ui.viewmodel.ContinuarLendoViewModel
import com.jotape.inkshelf.ui.viewmodel.FavoritosViewModel
import com.jotape.inkshelf.ui.viewmodel.LeitorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Grafo de injeção de dependência (Koin).
 *
 * Nota de porte: o módulo do Android é praticamente este, sem os `androidContext()` — no desktop
 * não existe `Context`, e cada peça resolve seus próprios caminhos via [InkPaths]. O Koin foi
 * mantido (em vez de trocar por Hilt/Dagger) porque é DI de runtime puro, sem plugin Gradle, e
 * funciona igual nas duas plataformas.
 */
val appModule = module {

    // Extractors guardam cache em memória (sessões de PDF abertas, locks por arquivo), então
    // instância única — duas instâncias competindo pelo mesmo arquivo desperdiçariam trabalho.
    single { PageExtractor() }
    single { CoverExtractor(get()) }
    single { ComicInfoExtractor() }

    // Fachada parametrizada pelo nome do banco, para suportar as bibliotecas salvas (snapshots),
    // que rodam sobre um `.db` próprio. Sem parâmetro, é o banco principal.
    single { LibraryRepository(InkPaths.MAIN_DB_NAME) }

    single { CoverGenerator(get(), get()) }

    viewModel { BibliotecaViewModel(get(), get()) }
    viewModel { FavoritosViewModel(get()) }
    viewModel { ContinuarLendoViewModel(get()) }
    viewModel { LeitorViewModel(get(), get()) }
}
