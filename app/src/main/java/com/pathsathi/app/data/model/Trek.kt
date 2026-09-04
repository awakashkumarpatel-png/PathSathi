package com.pathsathi.app.data.model

data class Trek(
    val id: String,
    val name: String,
    val location: String,
    val distanceKm: Double,
    val elevationM: Int,
    val difficulty: Difficulty,
    val durationDays: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val destinationLatitude: Double = latitude,
    val destinationLongitude: Double = longitude,
    val routeWaypoints: List<Pair<Double, Double>> = emptyList(),
    val bestSeason: String = "",
    val permitsRequired: String = "",
    val equipmentList: List<String> = emptyList(),
    val safetyWarnings: String = ""
)

enum class Difficulty(val label: String) {
    EASY("Easy"),
    MODERATE("Moderate"),
    HARD("Hard"),
    EXTREME("Extreme")
}

data class JournalEntry(
    val id: String,
    val trekName: String,
    val note: String,
    val timestamp: Long
)
