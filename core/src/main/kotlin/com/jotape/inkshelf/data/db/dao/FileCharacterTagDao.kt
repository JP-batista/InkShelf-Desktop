package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jotape.inkshelf.data.db.entity.FileCharacterTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileCharacterTagDao {

    @Query("SELECT * FROM file_character_tags WHERE fileId = :fileId")
    suspend fun getForFile(fileId: String): List<FileCharacterTagEntity>

    @Query("SELECT * FROM file_character_tags WHERE characterId = :characterId")
    fun getForCharacter(characterId: String): Flow<List<FileCharacterTagEntity>>

    @Query("SELECT * FROM file_character_tags WHERE characterId IN (:characterIds)")
    suspend fun getForCharacters(characterIds: List<String>): List<FileCharacterTagEntity>

    @Query("SELECT DISTINCT characterId FROM file_character_tags")
    fun getDistinctCharacterIds(): Flow<List<String>>

    @Query("SELECT * FROM file_character_tags")
    fun getAll(): Flow<List<FileCharacterTagEntity>>

    @Query("SELECT COUNT(*) FROM file_character_tags WHERE fileId = :fileId")
    suspend fun countForFile(fileId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: FileCharacterTagEntity)

    @Query("DELETE FROM file_character_tags WHERE fileId = :fileId AND characterId = :characterId")
    suspend fun delete(fileId: String, characterId: String)

    @Query("DELETE FROM file_character_tags WHERE fileId = :fileId")
    suspend fun deleteByFileId(fileId: String)

    @Query("DELETE FROM file_character_tags")
    suspend fun deleteAll()

    /** Remove tags de arquivos que não existem mais na biblioteca (arquivo/pasta removidos). */
    @Query("DELETE FROM file_character_tags WHERE fileId NOT IN (SELECT id FROM files)")
    suspend fun deleteOrphaned()
}
