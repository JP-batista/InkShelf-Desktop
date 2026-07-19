package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jotape.inkshelf.data.db.entity.ChapterPaginationCacheEntity

@Dao
interface ChapterPaginationCacheDao {

    @Query(
        """
        SELECT pageCount FROM chapter_pagination_cache
        WHERE fileId = :fileId
            AND chapterIndex = :chapterIndex
            AND textZoom = :textZoom
            AND viewportWidth = :width
            AND viewportHeight = :height
        LIMIT 1
        """
    )
    suspend fun getPageCount(
        fileId: String,
        chapterIndex: Int,
        textZoom: Int,
        width: Int,
        height: Int,
    ): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChapterPaginationCacheEntity)

    @Query("DELETE FROM chapter_pagination_cache WHERE fileId = :fileId")
    suspend fun deleteByFileId(fileId: String)

    @Query("DELETE FROM chapter_pagination_cache WHERE cachedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
