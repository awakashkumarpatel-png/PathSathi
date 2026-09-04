package com.pathsathi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM checkins ORDER BY scheduledAt DESC")
    fun getAll(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM checkins ORDER BY scheduledAt DESC LIMIT 1")
    suspend fun getLatest(): CheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CheckInEntity): Long

    @Update
    suspend fun update(entry: CheckInEntity)

    @Query("SELECT * FROM checkins WHERE scheduledAt = :scheduledAt LIMIT 1")
    suspend fun getByScheduledAt(scheduledAt: Long): CheckInEntity?

    @Query("DELETE FROM checkins")
    suspend fun clearAll()
}
