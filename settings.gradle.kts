pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "InkShelf-Desktop"

// :core  — Kotlin/JVM puro (banco, scanner, extractors). Sem Compose.
// :ui    — Compose (tema, componentes, telas, viewmodels, navegação).
// :app   — main(), janela, grafo Koin, empacotamento nativo.
include(":core")
include(":ui")
include(":app")
