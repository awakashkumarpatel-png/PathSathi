package com.pathsathi.app.data.model

data class TripDayPlan(
    val dayNumber: Int,
    val title: String,
    val activity: String,
    val distanceKm: Double,
    val restStop: String,
    val stay: String
)

data class TripItinerary(
    val trekName: String,
    val startingPoint: String,
    val destination: String,
    val totalDistanceKm: Double,
    val totalDurationDays: Int,
    val travelers: Int,
    val days: List<TripDayPlan>,
    val packingList: List<String>,
    val safetyNotes: String,
    val bestSeason: String,
    val estimatedCost: Double,
    val costBreakdown: Map<String, Double>,
    /** Straight-line distance from the traveler's starting point to the trek, in km - only
     *  populated when the starting point could be geocoded (needs internet); never fabricated. */
    val approachDistanceKm: Double? = null,
    /** Rough travel-time estimate for the approach distance, assuming typical road travel. */
    val approachTravelTimeHours: Double? = null
)
