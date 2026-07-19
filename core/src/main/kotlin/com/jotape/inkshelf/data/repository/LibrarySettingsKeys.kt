package com.jotape.inkshelf.data.repository

import com.jotape.inkshelf.model.HomeSectionType
import com.jotape.inkshelf.model.LibraryViewMode

/**
 * Chaves e valores padrão das preferências, todas persistidas como linhas da tabela `settings`.
 *
 * Nota de porte: no app Android estas constantes moram no `companion object` de
 * `LibraryRepository` — e o comentário de lá explica que ficaram naquele lugar só porque mais de
 * 400 pontos do código já referenciavam `LibraryRepository.SETTING_*`, não porque fosse o lugar
 * certo. No desktop essas referências ainda não existem, então aproveitamos para colocá-las onde
 * pertencem: um objeto próprio, que o repositório de preferências consome sem inverter a
 * dependência (antes o repositório de domínio precisava enxergar a fachada que o contém).
 */
object LibrarySettingsKeys {

    const val DEFAULT_GRID_COLUMNS = 3
    const val MIN_GRID_COLUMNS = 2
    const val MAX_GRID_COLUMNS = 6
    val DEFAULT_VIEW_MODE = LibraryViewMode.GRID
    const val DEFAULT_DARK_MODE = true
    const val DEFAULT_THEME_ID = "red"
    const val DEFAULT_READING_HISTORY_ENABLED = false
    const val DEFAULT_CHARACTERS_ENABLED = false
    const val DEFAULT_INTERFACE_DENSITY = "normal"
    const val DEFAULT_FONT_SCALE = "normal"
    const val DEFAULT_CARD_SIZE = "large"
    const val DEFAULT_ITEM_SPACING = "normal"
    const val DEFAULT_HEADER_STYLE = "expanded"
    const val DEFAULT_COVER_CORNERS = "soft"
    const val DEFAULT_CUSTOM_COVER_CORNERS_PROGRESS = 0.5f
    const val DEFAULT_CUSTOM_COVER_CORNERS_ENABLED = false
    const val DEFAULT_RESUME_READING_NOTIFICATIONS_ENABLED = false
    const val DEFAULT_NEXT_FILE_NOTIFICATIONS_ENABLED = false
    const val DEFAULT_READING_STATISTICS_ENABLED = false
    const val DEFAULT_ANIMATION_MODE = "fluid"
    const val DEFAULT_TRANSITION_STYLE = "fade"
    const val DEFAULT_SHELF_TILT_DEGREES = 25f
    const val DEFAULT_ADAPTIVE_SHELF_TILT_ENABLED = false
    const val DEFAULT_HOME_SCREEN_ENABLED = true
    const val DEFAULT_ADVANCED_SETTINGS_UNLOCKED = false
    const val DEFAULT_READER_DIRECTION = "lr-paged"
    const val DEFAULT_READER_FIT_MODE = "fit"
    const val DEFAULT_READER_BRIGHTNESS = 1f
    const val DEFAULT_READER_AUTO_BRIGHTNESS = true
    const val DEFAULT_READER_PAGE_VIEW = "single"
    // Transição
    const val DEFAULT_READER_TRANSITION_TYPE = "translate"
    const val DEFAULT_READER_TRANSITION_AXIS = "horizontal"
    // Correção de cor
    const val DEFAULT_READER_COLOR_CORRECTION_ENABLED = false
    // Interruptores por seção da correção de cor.
    const val DEFAULT_READER_COLOR_WB_ENABLED = false
    const val DEFAULT_READER_COLOR_VIBRANCE_ENABLED = false
    const val DEFAULT_READER_COLOR_GAMMA_ENABLED = false
    // Equilíbrio do branco: armazenado em -1..1 (UI: -100..100), 0 = neutro.
    const val DEFAULT_READER_COLOR_WHITE_BALANCE = 0f
    // Vibração: armazenada em 0..1 (UI: 0..100), 1.0 = 100%.
    const val DEFAULT_READER_COLOR_VIBRANCE = 1f
    // Gama geral e por canal: 0.10..5.00, 1.0 = neutro.
    const val DEFAULT_READER_COLOR_GAMMA = 1f
    const val DEFAULT_READER_COLOR_GAMMA_R = 1f
    const val DEFAULT_READER_COLOR_GAMMA_G = 1f
    const val DEFAULT_READER_COLOR_GAMMA_B = 1f
    // Corte de bordas
    const val DEFAULT_READER_CROP_BORDERS = false
    // Fundo e borda entre páginas
    const val DEFAULT_READER_AUTO_BACKGROUND = false
    const val DEFAULT_READER_PAGE_BORDER = false
    // Tela e imersão
    const val DEFAULT_READER_KEEP_SCREEN_ON = true
    const val DEFAULT_READER_IMMERSIVE_MODE = true
    const val DEFAULT_READER_SHOW_NOTCH = false
    // Modo noturno do leitor de imagens ("on"|"off"|"auto")
    const val DEFAULT_READER_NIGHT_MODE = "off"
    // Travar rotação
    const val DEFAULT_READER_LOCK_ROTATION = false
    // Escala avançada (multiplicador de largura/altura)
    const val DEFAULT_READER_SCALE_WIDTH = 1f
    const val DEFAULT_READER_SCALE_HEIGHT = 1f
    // Animação/scroll
    const val DEFAULT_READER_SCROLL_DISTANCE = 1.0f
    const val DEFAULT_READER_SCROLL_DURATION_MS = 300
    const val DEFAULT_READER_ZOOM_DURATION_MS = 200
    // Navegação por volume
    const val DEFAULT_READER_VOLUME_NAVIGATION = false
    const val DEFAULT_READER_VOLUME_UP_ACTION = "none"
    const val DEFAULT_READER_VOLUME_DOWN_ACTION = "none"
    // Mapa de zonas de toque (vazio = layout padrão; formato definido no Passo 12)
    const val DEFAULT_READER_TAP_ZONE_MAP = ""
    // Inverte esquerda/direita das zonas quando a leitura é RTL
    const val DEFAULT_READER_ZONE_INVERT_RTL = false
    const val DEFAULT_EPUB_READER_DARK_MODE = true
    const val DEFAULT_EPUB_TEXT_ZOOM_PERCENT = 100
    val DEFAULT_HOME_SECTION_ORDER = HomeSectionType.defaultOrder
    val DEFAULT_HOME_HIDDEN_SECTIONS: Set<HomeSectionType> = emptySet()
    const val HOME_MAX_ITEMS_PER_RAIL = 20
    const val ROOT_FOLDER_ID = "root"


    const val SETTING_ROOT_PATH = "root_path"
    const val SETTING_ROOT_PATHS = "root_paths"
    const val ROOT_PATHS_SEPARATOR = "|"
    const val SETTING_DARK_MODE = "dark_mode"
    const val SETTING_THEME_ID = "theme_id"
    const val SETTING_ICON_ALIAS = "icon_alias"
    const val SETTING_COLUMNS = "columns"
    const val SETTING_VIEW_MODE = "view_mode"
    const val SETTING_SORT_MODE = "sort_mode"
    const val LEGACY_SETTING_GRID_MODE = "grid_mode"
    const val SETTING_READING_HISTORY_ENABLED = "reading_history_enabled"
    const val SETTING_CHARACTERS_ENABLED = "characters_enabled"
    const val SETTING_INTERFACE_DENSITY = "interface_density"
    const val SETTING_FONT_SCALE = "font_scale"
    const val SETTING_CARD_SIZE = "card_size"
    const val SETTING_ITEM_SPACING = "item_spacing"
    const val SETTING_HEADER_STYLE = "header_style"
    const val SETTING_COVER_CORNERS = "cover_corners"
    const val SETTING_CUSTOM_COVER_CORNERS_PROGRESS = "custom_cover_corners_progress"
    const val SETTING_CUSTOM_COVER_CORNERS_ENABLED = "custom_cover_corners_enabled"
    const val SETTING_RESUME_READING_NOTIFICATIONS_ENABLED = "resume_reading_notifications_enabled"
    const val SETTING_NEXT_FILE_NOTIFICATIONS_ENABLED = "next_file_notifications_enabled"
    const val SETTING_READING_STATISTICS_ENABLED = "reading_statistics_enabled"
    const val SETTING_ANIMATION_MODE = "animation_mode"
    const val SETTING_TRANSITION_STYLE = "transition_style"
    const val SETTING_SHELF_TILT_DEGREES = "shelf_tilt_degrees"
    const val SETTING_ADAPTIVE_SHELF_TILT_ENABLED = "adaptive_shelf_tilt_enabled"
    const val SETTING_HOME_SCREEN_ENABLED = "home_screen_enabled"
    const val SETTING_ADVANCED_SETTINGS_UNLOCKED = "advanced_settings_unlocked"
    const val SETTING_HOME_SECTION_ORDER = "home_section_order"
    const val SETTING_HOME_HIDDEN_SECTIONS = "home_hidden_sections"
    const val SETTING_READER_DIRECTION = "reader_direction"
    const val SETTING_READER_FIT_MODE = "reader_fit_mode"
    const val SETTING_READER_BRIGHTNESS = "reader_brightness"
    const val SETTING_READER_AUTO_BRIGHTNESS = "reader_auto_brightness"
    const val SETTING_READER_PAGE_VIEW = "reader_page_view"
    // Transição
    const val SETTING_READER_TRANSITION_TYPE = "reader_transition_type"
    const val SETTING_READER_TRANSITION_AXIS = "reader_transition_axis"
    // Correção de cor
    const val SETTING_READER_COLOR_CORRECTION_ENABLED = "reader_color_correction_enabled"
    const val SETTING_READER_COLOR_WB_ENABLED = "reader_color_wb_enabled"
    const val SETTING_READER_COLOR_VIBRANCE_ENABLED = "reader_color_vibrance_enabled"
    const val SETTING_READER_COLOR_GAMMA_ENABLED = "reader_color_gamma_enabled"
    const val SETTING_READER_COLOR_WHITE_BALANCE = "reader_color_white_balance"
    const val SETTING_READER_COLOR_VIBRANCE = "reader_color_vibrance"
    const val SETTING_READER_COLOR_GAMMA = "reader_color_gamma"
    const val SETTING_READER_COLOR_GAMMA_R = "reader_color_gamma_r"
    const val SETTING_READER_COLOR_GAMMA_G = "reader_color_gamma_g"
    const val SETTING_READER_COLOR_GAMMA_B = "reader_color_gamma_b"
    // Corte de bordas
    const val SETTING_READER_CROP_BORDERS = "reader_crop_borders"
    // Fundo e borda entre páginas
    const val SETTING_READER_AUTO_BACKGROUND = "reader_auto_background"
    const val SETTING_READER_PAGE_BORDER = "reader_page_border"
    // Tela e imersão
    const val SETTING_READER_KEEP_SCREEN_ON = "reader_keep_screen_on"
    const val SETTING_READER_IMMERSIVE_MODE = "reader_immersive_mode"
    const val SETTING_READER_SHOW_NOTCH = "reader_show_notch"
    // Modo noturno do leitor de imagens
    const val SETTING_READER_NIGHT_MODE = "reader_night_mode"
    // Travar rotação
    const val SETTING_READER_LOCK_ROTATION = "reader_lock_rotation"
    // Escala avançada
    const val SETTING_READER_SCALE_WIDTH = "reader_scale_width"
    const val SETTING_READER_SCALE_HEIGHT = "reader_scale_height"
    // Animação/scroll
    const val SETTING_READER_SCROLL_DISTANCE = "reader_scroll_distance"
    const val SETTING_READER_SCROLL_DURATION_MS = "reader_scroll_duration_ms"
    const val SETTING_READER_ZOOM_DURATION_MS = "reader_zoom_duration_ms"
    // Navegação por volume
    const val SETTING_READER_VOLUME_NAVIGATION = "reader_volume_navigation"
    const val SETTING_READER_VOLUME_UP_ACTION = "reader_volume_up_action"
    const val SETTING_READER_VOLUME_DOWN_ACTION = "reader_volume_down_action"
    // Mapa de zonas de toque
    const val SETTING_READER_TAP_ZONE_MAP = "reader_tap_zone_map"
    const val SETTING_READER_ZONE_INVERT_RTL = "reader_zone_invert_rtl"
    const val SETTING_EPUB_READER_DARK_MODE = "epub_reader_dark_mode"
    const val SETTING_EPUB_TEXT_ZOOM_PERCENT = "epub_text_zoom_percent"
    const val SETTING_EPUB_READER_FONT = "epub_reader_font"
    const val DEFAULT_EPUB_READER_FONT = "merriweather"
    const val SETTING_CACHE_LIMIT_MB = "cache_limit_mb"
    const val DEFAULT_CACHE_LIMIT_MB = 2048

    const val SETTING_HERO_CARD_STYLE = "hero_card_style"
    const val DEFAULT_HERO_CARD_STYLE = "spine"

    const val SETTING_FOLDER_CARD_STYLE = "folder_card_style"
    const val DEFAULT_FOLDER_CARD_STYLE = "stack"
}
