package com.pathsathi.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<TrackingSessionEntity>>

    @Insert
    suspend fun insertSession(session: TrackingSessionEntity): Long

    @Update
    suspend fun updateSession(session: TrackingSessionEntity)

    @Delete
    suspend fun deleteSession(session: TrackingSessionEntity)

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<TrackPointEntity>

    @Query("DELETE FROM track_points WHERE sessionId = :sessionId")
    suspend fun deletePointsForSession(sessionId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT trekId FROM favorites")
    fun getAll(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trekId = :trekId")
    suspend fun remove(trekId: String)
}
