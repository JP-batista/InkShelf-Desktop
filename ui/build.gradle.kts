plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

dependencies {
    api(project(":core"))

    // Os acessores `compose.*` do plugin são usados de propósito, apesar do aviso de depreciação:
    // os artefatos do Compose Multiplatform não compartilham uma versão única (material3 e runtime
    // seguem trilhas diferentes), então fixar coordenadas no version catalog quebra a resolução.
    @Suppress("DEPRECATION")
    run {
        api(compose.runtime)
        api(compose.foundation)
        api(compose.material3)
        api(compose.ui)
        api(compose.components.resources)
    }
    api(libs.compose.material.icons.extended)

    api(libs.kotlinx.coroutines.swing)
}
