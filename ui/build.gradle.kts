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

    // Navegação e ViewModel: as variantes `org.jetbrains.androidx.*` do version catalog são as
    // multiplataforma — as `androidx.*` do mobile só existem para Android.
    api(libs.androidx.navigation.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)

    // Carregamento de capas a partir de arquivos em disco.
    api(libs.coil.compose)

    api(libs.koin.core)
    api(libs.koin.compose)
    api(libs.koin.compose.viewmodel)

    api(libs.kotlinx.coroutines.swing)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnit()
}
