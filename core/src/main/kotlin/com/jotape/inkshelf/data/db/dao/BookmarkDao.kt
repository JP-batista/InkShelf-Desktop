package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jotape.inkshelf.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

/** Projeção de marcador + dados do arquivo (LEFT JOIN); fileName/coverPath nulos se o arquivo saiu da biblioteca. */
data class BookmarkWithFile(
    val fileId: String,
    val pageIndex: Int,
    val label: String?,
    val createdAt: Long,
    val fileName: String?,
    val coverPath: String?,
)

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE fileId = :fileId ORDER BY pageIndex ASC")
    fun getForFile(fileId: String): Flow<List<BookmarkEntity>>

    @Query(
        """
        SELECT b.fileId AS fileId, b.pageIndex AS pageIndex, b.label AS label,
               b.createdAt AS createdAt, f.name AS fileName, f.coverPath AS coverPath
        FROM bookmarks b LEFT JOIN files f ON f.id = b.fileId
        ORDER BY b.createdAt DESC
        """,
    )
    fun getAllWithFile(): Flow<List<BookmarkWithFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE fileId = :fileId AND pageIndex = :pageIndex")
    suspend fun delete(fileId: String, pageIndex: Int)

    @Query("UPDATE bookmarks SET label = :label WHERE fileId = :fileId AND pageIndex = :pageIndex")
    suspend fun updateLabel(fileId: String, pageIndex: Int, label: String?)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE fileId = :fileId AND pageIndex = :pageIndex")
    suspend fun countAt(fileId: String, pageIndex: Int): Int

    @Query("DELETE FROM bookmarks WHERE fileId = :fileId")
    suspend fun deleteByFileId(fileId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}
