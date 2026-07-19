package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.model.LibraryViewMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Exercita o caminho que o app realmente usa em produção — `InkShelfDatabase.getInstance` sobre
 * os diretórios de [InkPaths] —, e não o construtor avulso de banco dos outros testes. É o que
 * confirma que o banco abre sozinho no perfil do usuário, criando os diretórios se preciso.
 */
class LibrarySettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var settings: LibrarySettingsRepository

    @Before
    fun setUp() {
        // A ordem importa: descartar as instâncias em cache ANTES de redirecionar os
        // diretórios, senão este teste receberia o banco aberto pelo teste anterior, que
        // aponta para outro diretório temporário.
        InkShelfDatabase.closeAll()
        InkPaths.useRootForTesting(tempFolder.newFolder("InkShelf"))
        settings = LibrarySettingsRepository(dbName = "settings-test.db")
    }

    @After
    fun tearDown() {
        InkShelfDatabase.closeAll()
    }

    @Test
    fun `valor ausente cai no padrao`() = runTest {
        assertEquals(LibrarySettingsKeys.DEFAULT_THEME_ID, settings.getThemeIdFlow().first())
        assertEquals(LibrarySettingsKeys.DEFAULT_GRID_COLUMNS, settings.getColumnsFlow().first())
        assertEquals(LibrarySettingsKeys.DEFAULT_DARK_MODE, settings.getDarkModeFlow().first())
    }

    @Test
    fun `valor gravado e lido de volta`() = runTest {
        settings.setSetting(LibrarySettingsKeys.SETTING_THEME_ID, "blue")
        settings.setSetting(LibrarySettingsKeys.SETTING_COLUMNS, "5")

        assertEquals("blue", settings.getThemeIdFlow().first())
        assertEquals(5, settings.getColumnsFlow().first())
    }

    @Test
    fun `numero de colunas fora da faixa e limitado`() = runTest {
        settings.setSetting(LibrarySettingsKeys.SETTING_COLUMNS, "99")
        assertEquals(LibrarySettingsKeys.MAX_GRID_COLUMNS, settings.getColumnsFlow().first())

        settings.setSetting(LibrarySettingsKeys.SETTING_COLUMNS, "0")
        assertEquals(LibrarySettingsKeys.MIN_GRID_COLUMNS, settings.getColumnsFlow().first())
    }

    @Test
    fun `valor corrompido no banco cai no padrao em vez de estourar`() = runTest {
        settings.setSetting(LibrarySettingsKeys.SETTING_COLUMNS, "nao-e-numero")
        assertEquals(LibrarySettingsKeys.DEFAULT_GRID_COLUMNS, settings.getColumnsFlow().first())
    }

    @Test
    fun `modo de visualizacao usa a chave antiga quando so ela existe`() = runTest {
        // Compatibilidade herdada do mobile: versões antigas gravavam um booleano `grid_mode`
        // antes de existir a chave `view_mode`.
        settings.setSetting(LibrarySettingsKeys.LEGACY_SETTING_GRID_MODE, "false")

        assertEquals(LibraryViewMode.LIST, settings.getViewMode())
    }

    @Test
    fun `resetar preferencias volta tudo ao padrao`() = runTest {
        settings.setSetting(LibrarySettingsKeys.SETTING_THEME_ID, "green")
        settings.resetSettings()

        assertEquals(LibrarySettingsKeys.DEFAULT_THEME_ID, settings.getThemeIdFlow().first())
    }
}
