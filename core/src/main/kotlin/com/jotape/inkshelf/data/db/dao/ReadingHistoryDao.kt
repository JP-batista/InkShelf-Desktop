package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jotape.inkshelf.data.db.entity.ReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {

    @Query("SELECT * FROM reading_history ORDER BY completedAt DESC")
    fun getAll(): Flow<List<ReadingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ReadingHistoryEntity)

    @Query("SELECT COUNT(*) FROM reading_history")
    suspend fun count(): Int

    @Query("SELECT * FROM reading_history ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLatest(): ReadingHistoryEntity?

    @Query("DELETE FROM reading_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM reading_history")
    suspend fun deleteAll()
}
