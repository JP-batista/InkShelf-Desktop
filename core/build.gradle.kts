plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// :core é Kotlin/JVM puro — banco, scanner e extractors. Nada de Compose aqui: é essa fronteira
// que permite, no futuro, transformar este módulo num KMP compartilhado com o app Android.
dependencies {
    api(libs.kotlinx.coroutines.core)

    api(libs.androidx.room.runtime)
    // Driver SQLite nativo empacotado pelo próprio Room: no desktop não existe o SQLite do
    // sistema (que no Android vem com a plataforma), então o binário precisa vir junto.
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room.compiler)

    // Leitura dos formatos de quadrinho.
    implementation(libs.junrar)                 // RAR4 — Java puro, veio do mobile sem mudança
    implementation(libs.sevenzip.jbinding)      // RAR5
    implementation(libs.sevenzip.jbinding.all)  // binários nativos do 7-Zip para desktop
    implementation(libs.pdfbox)                 // PDF — substitui o PdfRenderer do Android

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.test {
    useJUnit()
}
