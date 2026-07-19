package com.jotape.inkshelf.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jotape.inkshelf.data.InkPaths
import com.jotape.inkshelf.data.db.dao.BookmarkDao
import com.jotape.inkshelf.data.db.dao.ChapterPaginationCacheDao
import com.jotape.inkshelf.data.db.dao.CoverDao
import com.jotape.inkshelf.data.db.dao.FileCharacterTagDao
import com.jotape.inkshelf.data.db.dao.FileDao
import com.jotape.inkshelf.data.db.dao.FileTeamTagDao
import com.jotape.inkshelf.data.db.dao.FolderDao
import com.jotape.inkshelf.data.db.dao.ReadingHistoryDao
import com.jotape.inkshelf.data.db.dao.SettingsDao
import com.jotape.inkshelf.data.db.dao.StatisticsDao
import com.jotape.inkshelf.data.db.entity.BookmarkEntity
import com.jotape.inkshelf.data.db.entity.ChapterPaginationCacheEntity
import com.jotape.inkshelf.data.db.entity.CoverEntity
import com.jotape.inkshelf.data.db.entity.DailyStatisticsEntity
import com.jotape.inkshelf.data.db.entity.FileCharacterTagEntity
import com.jotape.inkshelf.data.db.entity.FileEntity
import com.jotape.inkshelf.data.db.entity.FileTeamTagEntity
import com.jotape.inkshelf.data.db.entity.FolderEntity
import com.jotape.inkshelf.data.db.entity.ReadingHistoryEntity
import com.jotape.inkshelf.data.db.entity.ReadingSessionEntity
import com.jotape.inkshelf.data.db.entity.SettingsEntity
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers

/**
 * Banco da biblioteca.
 *
 * Nota de porte: o app Android está na versão 14, com 13 migrações acumuladas desde a v1. Aqui o
 * banco **nasce na versão 1 já com o schema final** — não existe instalação anterior no desktop
 * para migrar, então carregar aquele histórico seria custo sem benefício. A primeira migração
 * real deste banco será a v1 → v2, quando o schema mudar depois do primeiro release.
 *
 * Também de propósito: **sem `fallbackToDestructiveMigration()`**. No app Android ele está ativo
 * e apaga a biblioteca inteira em silêncio se uma migração falhar. Aqui uma migração faltando
 * estoura na cara do desenvolvedor, que é o comportamento correto.
 */
@Database(
    entities = [
        FolderEntity::class,
        FileEntity::class,
        CoverEntity::class,
        SettingsEntity::class,
        ReadingHistoryEntity::class,
        DailyStatisticsEntity::class,
        ReadingSessionEntity::class,
        ChapterPaginationCacheEntity::class,
        BookmarkEntity::class,
        FileCharacterTagEntity::class,
        FileTeamTagEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class InkShelfDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun coverDao(): CoverDao
    abstract fun settingsDao(): SettingsDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun chapterPaginationCacheDao(): ChapterPaginationCacheDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun fileCharacterTagDao(): FileCharacterTagDao
    abstract fun fileTeamTagDao(): FileTeamTagDao

    companion object {
        private val instances = ConcurrentHashMap<String, InkShelfDatabase>()

        /**
         * Instância única por nome de banco. O parâmetro existe porque as bibliotecas salvas
         * (snapshots) rodam sobre um `.db` próprio, lado a lado com o principal.
         */
        fun getInstance(dbName: String = InkPaths.MAIN_DB_NAME): InkShelfDatabase =
            instances.getOrPut(dbName) { build(InkPaths.databaseFile(dbName).absolutePath) }

        /**
         * Fecha e esquece todas as instâncias em cache.
         *
         * Existe para os testes: [getInstance] memoriza por nome de banco, e o caminho em disco
         * vem de [InkPaths]. Sem isto, um teste que redireciona o [InkPaths] receberia a
         * instância aberta por um teste anterior, ainda apontando para o diretório antigo.
         */
        fun closeAll() {
            val open = instances.values.toList()
            instances.clear()
            open.forEach { runCatching { it.close() } }
        }

        /** Banco avulso num caminho arbitrário — usado pelos testes. */
        fun build(absolutePath: String): InkShelfDatabase =
            Room.databaseBuilder<InkShelfDatabase>(name = absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
    }
}
