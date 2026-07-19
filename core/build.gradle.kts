plugins {
    alias(libs.plugins.kotlin.jvm)
}

// :core é Kotlin/JVM puro — banco, scanner e extractors. Nada de Compose aqui: é essa fronteira
// que permite, no futuro, transformar este módulo num KMP compartilhado com o app Android.
dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnit()
}
