package com.pathsathi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun getAll(): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: EmergencyContactEntity): Long

    @Update
    suspend fun update(contact: EmergencyContactEntity)

    @Delete
    suspend fun delete(contact: EmergencyContactEntity)

    @Query("UPDATE emergency_contacts SET isPrimary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE emergency_contacts SET isPrimary = 1 WHERE id = :id")
    suspend fun setPrimary(id: Long)
}
