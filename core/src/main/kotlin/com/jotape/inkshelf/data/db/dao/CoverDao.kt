package com.jotape.inkshelf.data.db.dao

import androidx.room.*
import com.jotape.inkshelf.data.db.entity.CoverEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoverDao {

    @Query("SELECT * FROM covers WHERE fileId = :fileId")
    suspend fun getCoverByFileId(fileId: String): CoverEntity?

    @Query("SELECT * FROM covers ORDER BY createdAt DESC")
    fun getAllCovers(): Flow<List<CoverEntity>>

    @Query("SELECT fileId FROM covers")
    suspend fun getAllFileIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cover: CoverEntity)

    @Query("DELETE FROM covers WHERE fileId = :fileId")
    suspend fun deleteCover(fileId: String)

    @Query("DELETE FROM covers WHERE fileId IN (:fileIds)")
    suspend fun deleteByIds(fileIds: List<String>)

    @Query("DELETE FROM covers")
    suspend fun deleteAll()

    @Query("SELECT SUM(sizeBytes) FROM covers")
    suspend fun getTotalSizeBytes(): Long?
}