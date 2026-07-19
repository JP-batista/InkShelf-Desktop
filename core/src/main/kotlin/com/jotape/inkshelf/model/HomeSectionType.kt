package com.jotape.inkshelf.model


enum class HomeSectionType(
    val id: String,
    val displayName: String,
    val description: String,
) {
    CONTINUE_READING(
        id = "continue_reading",
        displayName = "Continuar lendo",
        description = "Arquivos que ainda estão em andamento",
    ),
    NEXT_IN_FOLDER(
        id = "next_in_folder",
        displayName = "Continuações imediatas",
        description = "Os próximos arquivos prontos para abrir agora",
    ),
    FOLDER_SUGGESTIONS(
        id = "folder_suggestions",
        displayName = "Sugestões",
        description = "Filas para começar novas pastas e coleções",
    ),
    STARTED_FOLDERS(
        id = "started_folders",
        displayName = "Pastas iniciadas",
        description = "Pastas em andamento e filas abertas para continuar",
    ),
    FAVORITES(
        id = "favorites",
        displayName = "Favoritos",
        description = "Itens da aba Favoritos (pastas e arquivos)",
    ),
    RECENT_COMPLETED(
        id = "recent_completed",
        displayName = "Últimos concluídos",
        description = "Leituras finalizadas recentemente",
    ),
    QUICK_ACTIONS(
        id = "quick_actions",
        displayName = "Atalhos",
        description = "Acesso rápido para as áreas principais",
    ),
    ROOT_FILE_RAILS(
        id = "root_file_rails",
        displayName = "Arquivos na raiz",
        description = "Sugestões dos arquivos diretos na pasta raiz",
    ),
    CHARACTERS(
        id = "characters",
        displayName = "Personagens",
        description = "Sua biblioteca organizada por personagem e editora",
    ),
    PUBLISHERS(
        id = "publishers",
        displayName = "Editoras",
        description = "Sua biblioteca organizada por editora",
    ), ;

    companion object {
        val defaultOrder: List<HomeSectionType> = listOf(
            CONTINUE_READING,
            NEXT_IN_FOLDER,
            STARTED_FOLDERS,
            FOLDER_SUGGESTIONS,
            ROOT_FILE_RAILS,
            CHARACTERS,
            PUBLISHERS,
            RECENT_COMPLETED,
            FAVORITES,
            QUICK_ACTIONS,
        )

        fun fromId(id: String?): HomeSectionType? =
            entries.firstOrNull { it.id == id }

        fun parseOrder(value: String?): List<HomeSectionType> {
            val parsed = value
                ?.split(',')
                ?.mapNotNull { fromId(it.trim()) }
                ?.distinct()
                .orEmpty()

            if (parsed.isEmpty()) return defaultOrder

            val normalized = parsed.toMutableList()

            fun ensureAfter(section: HomeSectionType, anchor: HomeSectionType) {
                if (section in normalized) return
                val anchorIndex = normalized.indexOf(anchor)
                if (anchorIndex >= 0) {
                    normalized.add(anchorIndex + 1, section)
                } else {
                    normalized.add(section)
                }
            }

            // Migra ordens antigas: novas seções entram logo após "Próximo da pasta".
            ensureAfter(STARTED_FOLDERS, NEXT_IN_FOLDER)
            ensureAfter(FOLDER_SUGGESTIONS, STARTED_FOLDERS)
            ensureAfter(ROOT_FILE_RAILS, FOLDER_SUGGESTIONS)
            ensureAfter(CHARACTERS, ROOT_FILE_RAILS)
            ensureAfter(PUBLISHERS, CHARACTERS)
            ensureAfter(FAVORITES, RECENT_COMPLETED)

            return (normalized + defaultOrder).distinct()
        }

        fun parseSet(value: String?): Set<HomeSectionType> =
            value
                ?.split(',')
                ?.mapNotNull { fromId(it.trim()) }
                ?.toSet()
                .orEmpty()

        fun serialize(items: Iterable<HomeSectionType>): String =
            items.joinToString(",") { it.id }
    }
}

/**
 * Agrupamento das seções para a tela de personalização do início. A home continua renderizando
 * cada [HomeSectionType] como uma fila separada — o grupo existe só para a personalização:
 * cada card representa um grupo inteiro, mover o card move todas as seções membro juntas (como
 * um bloco contíguo) e ocultar/mostrar afeta todas de uma vez. Alguns grupos têm um único
 * membro (Favoritos, Atalhos, Últimos concluídos), então continuam idênticos a uma seção.
 */
enum class HomeSectionGroup(
    val id: String,
    val displayName: String,
    val description: String,
    val members: List<HomeSectionType>,
) {
    CONTINUE(
        id = "continue",
        displayName = "Continuar lendo",
        description = "Em andamento, continuações imediatas e pastas iniciadas",
        members = listOf(
            HomeSectionType.CONTINUE_READING,
            HomeSectionType.NEXT_IN_FOLDER,
            HomeSectionType.STARTED_FOLDERS,
        ),
    ),
    SUGGESTIONS(
        id = "suggestions",
        displayName = "Sugestões",
        description = "Novas coleções para começar e arquivos na raiz",
        members = listOf(
            HomeSectionType.FOLDER_SUGGESTIONS,
            HomeSectionType.ROOT_FILE_RAILS,
        ),
    ),
    CATALOG(
        id = "catalog",
        displayName = "Personagens e editoras",
        description = "Sua biblioteca organizada por personagem e editora",
        members = listOf(
            HomeSectionType.CHARACTERS,
            HomeSectionType.PUBLISHERS,
        ),
    ),
    RECENT_COMPLETED(
        id = "recent_completed",
        displayName = "Últimos concluídos",
        description = "Leituras finalizadas recentemente",
        members = listOf(HomeSectionType.RECENT_COMPLETED),
    ),
    FAVORITES(
        id = "favorites",
        displayName = "Favoritos",
        description = "Itens da aba Favoritos (pastas e arquivos)",
        members = listOf(HomeSectionType.FAVORITES),
    ),
    QUICK_ACTIONS(
        id = "quick_actions",
        displayName = "Atalhos",
        description = "Acesso rápido para as áreas principais",
        members = listOf(HomeSectionType.QUICK_ACTIONS),
    ), ;

    companion object {
        val defaultOrder: List<HomeSectionGroup> = entries.toList()

        /** Grupo ao qual uma seção pertence. */
        fun of(section: HomeSectionType): HomeSectionGroup =
            entries.first { section in it.members }

        /**
         * Ordem dos grupos derivada de uma ordem de seções: cada grupo assume a posição da
         * primeira das suas seções membro que aparece na lista. Grupos ausentes entram no fim
         * na ordem padrão.
         */
        fun orderFrom(sectionOrder: List<HomeSectionType>): List<HomeSectionGroup> {
            val seen = LinkedHashSet<HomeSectionGroup>()
            sectionOrder.forEach { section -> seen.add(of(section)) }
            seen.addAll(defaultOrder)
            return seen.toList()
        }

        /** Expande uma ordem de grupos de volta para a ordem de seções (membros contíguos). */
        fun expand(groupOrder: List<HomeSectionGroup>): List<HomeSectionType> =
            groupOrder.flatMap { it.members }
    }
}
