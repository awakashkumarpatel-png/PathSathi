package com.pathsathi.app.map

data class GeoPoint(val lat: Double, val lng: Double)

enum class TravelMode { WALKING, DRIVING, LOCAL_TRANSIT }

data class MapPlace(
    val id: String,
    val name: String,
    val point: GeoPoint?,
    val category: String,
    val isSaved: Boolean = false
)

data class DistanceEstimate(
    val distanceKm: Double,
    val etaMinutes: Int,
    val mode: TravelMode,
    /** True when this came from straight-line/offline math rather than a real routing engine. */
    val isOfflineEstimate: Boolean
)
