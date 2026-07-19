package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jotape.inkshelf.data.db.entity.FileTeamTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileTeamTagDao {

    @Query("SELECT * FROM file_team_tags WHERE fileId = :fileId")
    suspend fun getForFile(fileId: String): List<FileTeamTagEntity>

    @Query("SELECT * FROM file_team_tags WHERE teamId = :teamId")
    suspend fun getForTeam(teamId: String): List<FileTeamTagEntity>

    @Query("SELECT * FROM file_team_tags")
    fun getAll(): Flow<List<FileTeamTagEntity>>

    @Query("SELECT COUNT(*) FROM file_team_tags WHERE fileId = :fileId")
    suspend fun countForFile(fileId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: FileTeamTagEntity)

    @Query("DELETE FROM file_team_tags WHERE fileId = :fileId AND teamId = :teamId")
    suspend fun delete(fileId: String, teamId: String)

    @Query("DELETE FROM file_team_tags WHERE fileId = :fileId")
    suspend fun deleteByFileId(fileId: String)

    @Query("DELETE FROM file_team_tags")
    suspend fun deleteAll()

    /** Remove tags de arquivos que não existem mais na biblioteca (arquivo/pasta removidos). */
    @Query("DELETE FROM file_team_tags WHERE fileId NOT IN (SELECT id FROM files)")
    suspend fun deleteOrphaned()
}
