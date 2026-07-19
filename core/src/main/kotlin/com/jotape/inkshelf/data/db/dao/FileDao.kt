package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jotape.inkshelf.data.db.entity.FileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Projeção enxuta usada para agregar o progresso das pastas. Não inclui
 * coverPath/comicInfo de propósito: além de reduzir a memória (30k linhas),
 * permite que um distinctUntilChanged suprima o recálculo da árvore de
 * progresso quando só as capas mudam (o que acontece continuamente durante a
 * geração de capas em bibliotecas grandes).
 */
data class FileProgressRow(
    val folderId: String,
    val pages: Int,
    val currentPage: Int,
    val isRead: Boolean,
)

@Dao
interface FileDao {

    @Query("SELECT folderId, pages, currentPage, isRead FROM files WHERE isHidden = 0")
    fun getVisibleFileProgressRows(): Flow<List<FileProgressRow>>

    @Query("SELECT folderId, pages, currentPage, isRead FROM files")
    fun getFileProgressRows(): Flow<List<FileProgressRow>>

    @Query("SELECT * FROM files WHERE folderId = :folderId ORDER BY name ASC")
    fun getFilesByFolder(folderId: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE folderId = :folderId AND isHidden = 0 ORDER BY name ASC")
    fun getVisibleFilesByFolder(folderId: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: String): FileEntity?

    @Query("SELECT * FROM files WHERE currentPage > 0 AND currentPage < pages AND isRead = 0 ORDER BY name ASC")
    fun getInProgressFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE currentPage > 0 AND currentPage < pages AND isRead = 0 AND isHidden = 0 ORDER BY name ASC")
    fun getVisibleInProgressFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE currentPage > 0 AND currentPage < pages AND isRead = 0 ORDER BY currentPage DESC, name ASC LIMIT 1")
    suspend fun getFirstInProgressFile(): FileEntity?

    @Query("SELECT * FROM files WHERE currentPage > 0 AND currentPage < pages AND isRead = 0 ORDER BY name ASC")
    suspend fun getInProgressFilesOnce(): List<FileEntity>

    @Query("SELECT * FROM files WHERE currentPage > 0 AND currentPage < pages AND isRead = 0 AND isHidden = 0 ORDER BY name ASC")
    suspend fun getVisibleInProgressFilesOnce(): List<FileEntity>

    @Query("SELECT * FROM files WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isFavorite = 1 AND isHidden = 0 ORDER BY name ASC")
    fun getVisibleFavoriteFiles(): Flow<List<FileEntity>>

    @Query("SELECT COUNT(*) FROM files WHERE isFavorite = 1")
    suspend fun countFavoriteFiles(): Int

    @Query("SELECT COUNT(*) FROM files WHERE isFavorite = 1 AND isHidden = 0")
    suspend fun countVisibleFavoriteFiles(): Int

    @Query("SELECT * FROM files WHERE folderId = :folderId LIMIT 1")
    suspend fun getFirstFileInFolder(folderId: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<FileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: FileEntity)

    @Update
    suspend fun update(file: FileEntity)

    @Query("UPDATE files SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean)

    @Query("UPDATE files SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE files SET isHidden = :isHidden WHERE id = :id")
    suspend fun setHidden(id: String, isHidden: Boolean)

    @Query("UPDATE files SET currentPage = :page WHERE id = :id")
    suspend fun updateProgress(id: String, page: Int)

    @Query("UPDATE files SET pages = :pages WHERE id = :id")
    suspend fun updatePages(id: String, pages: Int)

    @Query("UPDATE files SET coverPath = :path WHERE id = :id")
    suspend fun updateCoverPath(id: String, path: String)

    @Query("UPDATE files SET coverPath = NULL WHERE id = :id")
    suspend fun clearCoverPath(id: String)

    @Query("UPDATE files SET coverPath = NULL")
    suspend fun clearAllCoverPaths()

    @Query(
        """
        UPDATE files SET
            comicInfoTitle = :title,
            comicInfoSeries = :series,
            comicInfoNumber = :number,
            comicInfoWriter = :writer,
            comicInfoPenciller = :penciller,
            comicInfoArtist = :artist,
            comicInfoColorist = :colorist,
            comicInfoPublisher = :publisher,
            comicInfoYear = :year,
            comicInfoSummary = :summary,
            comicInfoScanned = :scanned
        WHERE id = :id
        """
    )
    suspend fun updateComicInfo(
        id: String,
        title: String?,
        series: String?,
        number: String?,
        writer: String?,
        penciller: String?,
        artist: String?,
        colorist: String?,
        publisher: String?,
        year: Int?,
        summary: String?,
        scanned: Boolean,
    )

    @Query("DELETE FROM files")
    suspend fun deleteAll()

    @Query("SELECT id FROM files")
    suspend fun getAllIds(): List<String>

    @Query("DELETE FROM files WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE files SET epubScrollPercent = :scrollPercent WHERE id = :id")
    suspend fun updateEpubScrollPercent(id: String, scrollPercent: Int)

    @Query("UPDATE files SET epubGlobalCurrentPage = :currentPage, epubTotalPages = :totalPages, epubLastTextZoom = :textZoom WHERE id = :id")
    suspend fun updateEpubPageProgress(id: String, currentPage: Int, totalPages: Int, textZoom: Int)

    @Query("UPDATE files SET epubGlobalCurrentPage = 0, epubTotalPages = 0, epubLastTextZoom = 0 WHERE id = :id")
    suspend fun clearEpubPageProgress(id: String)

    @Query("DELETE FROM files WHERE folderId IN (:folderIds)")
    suspend fun deleteByFolderIds(folderIds: List<String>)

    @Query("SELECT * FROM files WHERE folderId = :folderId AND coverPath IS NOT NULL LIMIT 1")
    suspend fun getFirstFileWithCoverInFolder(folderId: String): FileEntity?

    @Query("SELECT * FROM files WHERE folderId = :folderId ORDER BY name ASC")
    suspend fun getFilesByFolderOnce(folderId: String): List<FileEntity>

    @Query("SELECT * FROM files WHERE folderId = :folderId AND isHidden = 0 ORDER BY name ASC")
    suspend fun getVisibleFilesByFolderOnce(folderId: String): List<FileEntity>

    @Query("SELECT * FROM files")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isHidden = 0")
    fun getVisibleAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY folderId ASC, name ASC")
    suspend fun getAllFilesOnce(): List<FileEntity>

    @Query("SELECT * FROM files WHERE isHidden = 0 ORDER BY folderId ASC, name ASC")
    suspend fun getVisibleAllFilesOnce(): List<FileEntity>

    @Query(
        """
        SELECT * FROM files
        WHERE name LIKE '%' || :query || '%'
            OR comicInfoTitle LIKE '%' || :query || '%'
            OR comicInfoSeries LIKE '%' || :query || '%'
            OR comicInfoNumber LIKE '%' || :query || '%'
            OR comicInfoWriter LIKE '%' || :query || '%'
            OR comicInfoPenciller LIKE '%' || :query || '%'
            OR comicInfoArtist LIKE '%' || :query || '%'
            OR comicInfoColorist LIKE '%' || :query || '%'
            OR comicInfoPublisher LIKE '%' || :query || '%'
            OR comicInfoSummary LIKE '%' || :query || '%'
            OR CAST(comicInfoYear AS TEXT) LIKE '%' || :query || '%'
        ORDER BY name ASC
        """
    )
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Query(
        """
        SELECT * FROM files
        WHERE isHidden = 0 AND (
            name LIKE '%' || :query || '%'
            OR comicInfoTitle LIKE '%' || :query || '%'
            OR comicInfoSeries LIKE '%' || :query || '%'
            OR comicInfoNumber LIKE '%' || :query || '%'
            OR comicInfoWriter LIKE '%' || :query || '%'
            OR comicInfoPenciller LIKE '%' || :query || '%'
            OR comicInfoArtist LIKE '%' || :query || '%'
            OR comicInfoColorist LIKE '%' || :query || '%'
            OR comicInfoPublisher LIKE '%' || :query || '%'
            OR comicInfoSummary LIKE '%' || :query || '%'
            OR CAST(comicInfoYear AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
        """
    )
    fun searchVisibleFiles(query: String): Flow<List<FileEntity>>

    @Query("SELECT COUNT(*) FROM files WHERE folderId = :folderId")
    suspend fun countFilesInFolder(folderId: String): Int
}
