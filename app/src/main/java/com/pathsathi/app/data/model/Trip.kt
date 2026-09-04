package com.pathsathi.app.data.model

/**
 * A fully user-created trip (Create Trip -> My Trips flow).
 * Distinct from [TripItinerary], which is the auto-generated day-wise
 * plan produced by TripPlannerEngine for the existing Trip Planner screen.
 */
data class Trip(
    val id: Long = 0,
    val tripName: String,
    val destination: String,
    val startDateTime: Long,
    val endDateTime: Long,
    val travelWith: String,
    val members: List<String>,
    val stops: List<String>,
    val stay: String,
    val budget: Double?,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)
