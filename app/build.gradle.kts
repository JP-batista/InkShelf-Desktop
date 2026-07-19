import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

dependencies {
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.jotape.inkshelf.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "InkShelf"
            packageVersion = "1.0.0"
            description = "Leitor de quadrinhos e livros digitais"
            vendor = "JP Batista"

            windows {
                // Precisa ser estável entre releases: é ele que faz o instalador ATUALIZAR
                // a versão existente em vez de instalar uma segunda cópia lado a lado.
                upgradeUuid = "8f3d5a41-6c2b-4e7d-9a18-2b5e4c7f1d63"
                menuGroup = "InkShelf"
                perUserInstall = true
                dirChooser = true
            }
        }
    }
}
