package com.pathsathi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMapDao {
    @Query("SELECT * FROM offline_maps ORDER BY downloadedAt DESC")
    fun getAll(): Flow<List<OfflineMapEntity>>

    @Query("SELECT * FROM offline_maps WHERE trekId = :trekId LIMIT 1")
    suspend fun getByTrek(trekId: String): OfflineMapEntity?

    @Insert
    suspend fun insert(map: OfflineMapEntity): Long

    @Delete
    suspend fun delete(map: OfflineMapEntity)
}
