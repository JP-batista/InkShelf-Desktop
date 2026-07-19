package com.jotape.inkshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jotape.inkshelf.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

/** Projeção enxuta da árvore de pastas (só o necessário para agregar progresso). */
data class FolderTreeRow(
    val id: String,
    val parentId: String?,
)

/** Projeção enxuta para atualizar itemCount somente quando mudou. */
data class FolderItemCountRow(
    val id: String,
    val itemCount: Int,
)

@Dao
interface FolderDao {

    @Query("SELECT id, parentId FROM folders WHERE isHidden = 0")
    fun getVisibleFolderTree(): Flow<List<FolderTreeRow>>

    @Query("SELECT id, parentId FROM folders")
    fun getFolderTree(): Flow<List<FolderTreeRow>>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersOnce(): List<FolderEntity>

    @Query("SELECT id, itemCount FROM folders")
    suspend fun getItemCountsOnce(): List<FolderItemCountRow>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name ASC")
    fun getFoldersByParent(parentId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isHidden = 0 ORDER BY name ASC")
    fun getVisibleFoldersByParent(parentId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: String): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isHidden = 0 ORDER BY name ASC")
    fun getVisibleAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isFavorite = 1 AND isHidden = 0 ORDER BY name ASC")
    fun getVisibleFavoriteFolders(): Flow<List<FolderEntity>>

    @Query("SELECT COUNT(*) FROM folders WHERE isFavorite = 1")
    suspend fun countFavoriteFolders(): Int

    @Query("SELECT COUNT(*) FROM folders WHERE isFavorite = 1 AND isHidden = 0")
    suspend fun countVisibleFavoriteFolders(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("UPDATE folders SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE folders SET isHidden = :isHidden WHERE id = :id")
    suspend fun setHidden(id: String, isHidden: Boolean)

    @Query("UPDATE folders SET customCoverId = :coverId WHERE id = :id")
    suspend fun setCustomCover(id: String, coverId: String?)

    @Query("UPDATE folders SET itemCount = :count WHERE id = :id")
    suspend fun updateItemCount(id: String, count: Int)

    @Query("DELETE FROM folders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM folders")
    suspend fun deleteAll()

    @Query("SELECT id FROM folders")
    suspend fun getAllIds(): List<String>

    @Query("UPDATE folders SET coverPath = :path WHERE id = :id")
    suspend fun updateCoverPath(id: String, path: String)

    @Query("UPDATE folders SET coverPath = NULL WHERE id = :id")
    suspend fun clearCoverPath(id: String)

    @Query("UPDATE folders SET coverPath = NULL")
    suspend fun clearAllCoverPaths()

    @Query("SELECT id FROM folders WHERE parentId = :parentId")
    suspend fun getSubFolderIds(parentId: String): List<String>

    @Query("SELECT id FROM folders WHERE parentId = :parentId AND isHidden = 0")
    suspend fun getVisibleSubFolderIds(parentId: String): List<String>

    @Query(
        """
        SELECT * FROM folders 
        WHERE name LIKE '%' || :query || '%' 
        ORDER BY name ASC
        """
    )
    fun searchFolders(query: String): Flow<List<FolderEntity>>

    @Query(
        """
        SELECT * FROM folders 
        WHERE isHidden = 0 AND name LIKE '%' || :query || '%' 
        ORDER BY name ASC
        """
    )
    fun searchVisibleFolders(query: String): Flow<List<FolderEntity>>
}
