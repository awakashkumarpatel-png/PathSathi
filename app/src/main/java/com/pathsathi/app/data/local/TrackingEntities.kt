package com.pathsathi.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracking_sessions")
data class TrackingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trekName: String,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val maxSpeedKmh: Double,
    val avgSpeedKmh: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val maxAltitudeM: Double
)

@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long
)
