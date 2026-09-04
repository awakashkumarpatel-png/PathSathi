package com.pathsathi.app.data.model

data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

data class TrackingSession(
    val trekName: String,
    val points: List<TrackPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val startTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = false,
    val isPaused: Boolean = false
)
