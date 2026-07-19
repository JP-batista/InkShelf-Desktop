package com.jotape.inkshelf.data.db.dao

import androidx.room.*
import com.jotape.inkshelf.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun getValueFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(setting: SettingsEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteKey(key: String)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}