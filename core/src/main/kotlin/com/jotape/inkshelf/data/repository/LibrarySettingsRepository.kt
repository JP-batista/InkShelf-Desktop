package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.InkShelfDatabase
import com.jotape.inkshelf.data.db.entity.SettingsEntity
import com.jotape.inkshelf.model.HomeSectionType
import com.jotape.inkshelf.model.LibrarySortMode
import com.jotape.inkshelf.model.LibraryViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Preferências do app (aparência, layout do leitor, home) — chave-valor sobre a tabela
 * `settings`. As chaves e os valores padrão ficam em [LibrarySettingsKeys].
 */
class LibrarySettingsRepository(dbName: String = InkPaths.MAIN_DB_NAME) {

    private val settingsDao = InkShelfDatabase.getInstance(dbName).settingsDao()

    suspend fun getSetting(key: String): String? = settingsDao.getValue(key)

    suspend fun setSetting(key: String, value: String) =
        settingsDao.setValue(SettingsEntity(key, value))

    suspend fun resetSettings() {
        settingsDao.deleteAll()
    }

    fun getColumnsFlow(): Flow<Int> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_COLUMNS).map { value ->
            value?.toIntOrNull()?.coerceIn(LibrarySettingsKeys.MIN_GRID_COLUMNS, LibrarySettingsKeys.MAX_GRID_COLUMNS)
                ?: LibrarySettingsKeys.DEFAULT_GRID_COLUMNS
        }

    fun getViewModeFlow(): Flow<LibraryViewMode> =
        combine(
            settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_VIEW_MODE),
            settingsDao.getValueFlow(LibrarySettingsKeys.LEGACY_SETTING_GRID_MODE),
        ) { value, legacyGridMode ->
            LibraryViewMode.fromStoredValue(
                value = value,
                legacyGridMode = legacyGridMode,
            )
        }

    suspend fun getViewMode(): LibraryViewMode =
        LibraryViewMode.fromStoredValue(
            value = settingsDao.getValue(LibrarySettingsKeys.SETTING_VIEW_MODE),
            legacyGridMode = settingsDao.getValue(LibrarySettingsKeys.LEGACY_SETTING_GRID_MODE),
        )

    fun getSortModeFlow(): Flow<LibrarySortMode> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_SORT_MODE).map { value ->
            LibrarySortMode.fromId(value)
        }

    suspend fun setSortMode(mode: LibrarySortMode) =
        setSetting(LibrarySettingsKeys.SETTING_SORT_MODE, mode.id)

    fun getCacheLimitMbFlow(): Flow<Int> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_CACHE_LIMIT_MB).map { value ->
            value?.toIntOrNull() ?: LibrarySettingsKeys.DEFAULT_CACHE_LIMIT_MB
        }

    suspend fun getCacheLimitMb(): Int =
        getSetting(LibrarySettingsKeys.SETTING_CACHE_LIMIT_MB)?.toIntOrNull()
            ?: LibrarySettingsKeys.DEFAULT_CACHE_LIMIT_MB

    suspend fun setCacheLimitMb(mb: Int) = setSetting(LibrarySettingsKeys.SETTING_CACHE_LIMIT_MB, mb.toString())

    fun getDarkModeFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_DARK_MODE).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_DARK_MODE
        }

    fun getThemeIdFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_THEME_ID).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_THEME_ID
        }

    fun getIconAliasFlow(): Flow<String?> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ICON_ALIAS).map { value ->
            value?.takeIf { it.isNotEmpty() }
        }

    fun getHeroCardStyleFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_HERO_CARD_STYLE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_HERO_CARD_STYLE
        }

    fun getFolderCardStyleFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_FOLDER_CARD_STYLE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_FOLDER_CARD_STYLE
        }

    fun getReadingHistoryEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_READING_HISTORY_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_READING_HISTORY_ENABLED
        }

    suspend fun isReadingHistoryEnabled(): Boolean =
        settingsDao.getValue(LibrarySettingsKeys.SETTING_READING_HISTORY_ENABLED)?.toBooleanStrictOrNull()
            ?: LibrarySettingsKeys.DEFAULT_READING_HISTORY_ENABLED

    fun getCharactersEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_CHARACTERS_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_CHARACTERS_ENABLED
        }

    fun getReadingStatisticsEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_READING_STATISTICS_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_READING_STATISTICS_ENABLED
        }

    suspend fun areReadingStatisticsEnabled(): Boolean =
        settingsDao.getValue(LibrarySettingsKeys.SETTING_READING_STATISTICS_ENABLED)
            ?.toBooleanStrictOrNull()
            ?: LibrarySettingsKeys.DEFAULT_READING_STATISTICS_ENABLED

    fun getInterfaceDensityFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_INTERFACE_DENSITY).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_INTERFACE_DENSITY
        }

    fun getFontScaleFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_FONT_SCALE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_FONT_SCALE
        }

    fun getCardSizeFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_CARD_SIZE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_CARD_SIZE
        }

    fun getItemSpacingFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ITEM_SPACING).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_ITEM_SPACING
        }

    fun getHeaderStyleFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_HEADER_STYLE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_HEADER_STYLE
        }

    fun getCoverCornersFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_COVER_CORNERS).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_COVER_CORNERS
        }

    fun getCustomCoverCornersProgressFlow(): Flow<Float> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_CUSTOM_COVER_CORNERS_PROGRESS).map { value ->
            value?.toFloatOrNull()?.coerceIn(0f, 1f) ?: LibrarySettingsKeys.DEFAULT_CUSTOM_COVER_CORNERS_PROGRESS
        }

    fun getCustomCoverCornersEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_CUSTOM_COVER_CORNERS_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_CUSTOM_COVER_CORNERS_ENABLED
        }

    fun getAnimationModeFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ANIMATION_MODE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_ANIMATION_MODE
        }

    fun getTransitionStyleFlow(): Flow<String> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_TRANSITION_STYLE).map { value ->
            value ?: LibrarySettingsKeys.DEFAULT_TRANSITION_STYLE
        }

    fun getHomeScreenEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_HOME_SCREEN_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_HOME_SCREEN_ENABLED
        }

    fun getHomeSectionOrderFlow(): Flow<List<HomeSectionType>> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_HOME_SECTION_ORDER).map { value ->
            HomeSectionType.parseOrder(value)
        }

    fun getHomeHiddenSectionsFlow(): Flow<Set<HomeSectionType>> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_HOME_HIDDEN_SECTIONS).map { value ->
            HomeSectionType.parseSet(value)
        }

    suspend fun setHomeSectionOrder(sectionOrder: List<HomeSectionType>) =
        setSetting(
            LibrarySettingsKeys.SETTING_HOME_SECTION_ORDER,
            HomeSectionType.serialize(sectionOrder),
        )

    suspend fun setHomeHiddenSections(hiddenSections: Set<HomeSectionType>) =
        setSetting(
            LibrarySettingsKeys.SETTING_HOME_HIDDEN_SECTIONS,
            HomeSectionType.serialize(hiddenSections),
        )

    fun getResumeReadingNotificationsEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_RESUME_READING_NOTIFICATIONS_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_RESUME_READING_NOTIFICATIONS_ENABLED
        }

    suspend fun areResumeReadingNotificationsEnabled(): Boolean =
        settingsDao.getValue(LibrarySettingsKeys.SETTING_RESUME_READING_NOTIFICATIONS_ENABLED)
            ?.toBooleanStrictOrNull()
            ?: LibrarySettingsKeys.DEFAULT_RESUME_READING_NOTIFICATIONS_ENABLED

    fun getNextFileNotificationsEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_NEXT_FILE_NOTIFICATIONS_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_NEXT_FILE_NOTIFICATIONS_ENABLED
        }

    suspend fun areNextFileNotificationsEnabled(): Boolean =
        settingsDao.getValue(LibrarySettingsKeys.SETTING_NEXT_FILE_NOTIFICATIONS_ENABLED)
            ?.toBooleanStrictOrNull()
            ?: LibrarySettingsKeys.DEFAULT_NEXT_FILE_NOTIFICATIONS_ENABLED

    fun getShelfTiltDegreesFlow(): Flow<Float> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_SHELF_TILT_DEGREES).map { value ->
            value?.toFloatOrNull()?.coerceIn(0f, 75f) ?: LibrarySettingsKeys.DEFAULT_SHELF_TILT_DEGREES
        }

    fun getAdaptiveShelfTiltEnabledFlow(): Flow<Boolean> =
        settingsDao.getValueFlow(LibrarySettingsKeys.SETTING_ADAPTIVE_SHELF_TILT_ENABLED).map { value ->
            value?.toBooleanStrictOrNull() ?: LibrarySettingsKeys.DEFAULT_ADAPTIVE_SHELF_TILT_ENABLED
        }
}
