package com.pathsathi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TripPlanDao {
    @Query("SELECT * FROM trip_plans ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TripPlanEntity>>

    @Insert
    suspend fun insert(plan: TripPlanEntity): Long

    @Delete
    suspend fun delete(plan: TripPlanEntity)
}
