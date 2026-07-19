package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jotape.inkshelf.data.db.entity.DailyStatisticsEntity
import com.jotape.inkshelf.data.db.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {

    @Query("SELECT * FROM daily_statistics ORDER BY dayKey ASC")
    fun getDailyStatistics(): Flow<List<DailyStatisticsEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC")
    fun getReadingSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM daily_statistics WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getDailyStatistic(dayKey: String): DailyStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyStatistic(stat: DailyStatisticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: ReadingSessionEntity)

    @Query("SELECT COUNT(*) FROM daily_statistics")
    suspend fun countTrackedDays(): Int

    @Query("SELECT COUNT(*) FROM reading_sessions")
    suspend fun countSessions(): Int

    @Query("DELETE FROM daily_statistics")
    suspend fun deleteAllDailyStatistics()

    @Query("DELETE FROM reading_sessions")
    suspend fun deleteAllReadingSessions()
}
