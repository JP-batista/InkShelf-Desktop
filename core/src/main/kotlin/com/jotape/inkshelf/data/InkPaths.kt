package com.jotape.inkshelf.data

import java.io.File

/**
 * Diretórios do app no disco.
 *
 * No Android o `Context` resolvia isso sozinho (`filesDir`, `cacheDir`) e o sistema apagava tudo
 * junto com o app. No desktop não existe esse contrato: escolhemos os diretórios e somos
 * responsáveis por eles. O padrão do Windows é separar dados de cache —
 * `%LOCALAPPDATA%\InkShelf\data` guarda o banco (não pode ser perdido), `...\cache` guarda
 * páginas e capas extraídas (descartável, o app regenera).
 */
object InkPaths {

    /** Raiz de tudo. Sobrescrevível em teste via [overrideRoot]. */
    private val defaultRoot: File by lazy {
        val localAppData = System.getenv("LOCALAPPDATA")
            ?: System.getProperty("user.home")?.let { "$it/AppData/Local" }
            // Último recurso: mesmo fora do Windows o app precisa de algum lugar para escrever.
            ?: System.getProperty("java.io.tmpdir")
        File(localAppData, "InkShelf")
    }

    private var overrideRoot: File? = null

    val root: File
        get() = overrideRoot ?: defaultRoot

    val dataDir: File
        get() = File(root, "data").ensureExists()

    val cacheDir: File
        get() = File(root, "cache").ensureExists()

    /** Banco principal da biblioteca. */
    val mainDatabaseFile: File
        get() = File(dataDir, MAIN_DB_NAME)

    fun databaseFile(dbName: String): File = File(dataDir, dbName)

    /**
     * Redireciona todos os diretórios para [dir]. Existe para os testes rodarem contra um
     * diretório temporário em vez do perfil real do usuário.
     */
    fun useRootForTesting(dir: File) {
        overrideRoot = dir
    }

    private fun File.ensureExists(): File = apply { mkdirs() }

    const val MAIN_DB_NAME = "inkshelf.db"
}
