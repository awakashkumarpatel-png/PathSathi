package com.pathsathi.app.data.repository

import com.pathsathi.app.data.model.Trek
import com.pathsathi.app.data.model.TripDayPlan
import com.pathsathi.app.data.model.TripItinerary
import com.pathsathi.app.data.network.GeocodingRepository
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Generates a complete day-wise trip plan automatically from a chosen trek
 * and the user's inputs (starting point, travelers, budget). Fully
 * rule-based and offline for the core plan - no external AI service or API
 * key required, so trip generation always works even with no internet.
 * The approach distance/travel-time from the starting point is computed
 * from a real (free, key-less) geocoding lookup when internet is available;
 * if it isn't, those fields are simply left blank rather than guessed.
 */
object TripPlannerEngine {

    suspend fun generate(
        trek: Trek,
        startingPoint: String,
        travelers: Int,
        budgetPerPerson: Double
    ): TripItinerary {
        val days = buildDayPlan(trek)
        val packingList = trek.equipmentList.ifEmpty {
            listOf("Trekking shoes", "Rain jacket", "Water bottle", "First-aid kit", "Headlamp")
        }

        val totalBudget = budgetPerPerson * travelers
        val breakdown = mapOf(
            "Stay" to totalBudget * 0.35,
            "Food" to totalBudget * 0.25,
            "Transport" to totalBudget * 0.25,
            "Permits & Misc" to totalBudget * 0.15
        )

        var approachDistanceKm: Double? = null
        var approachTravelTimeHours: Double? = null
        if (startingPoint.isNotBlank()) {
            try {
                val geocoder = GeocodingRepository()
                val startCoords = geocoder.geocode(startingPoint)
                val destCoords = geocoder.geocode(trek.location)
                if (startCoords != null && destCoords != null) {
                    val distance = haversineKm(
                        startCoords.first, startCoords.second,
                        destCoords.first, destCoords.second
                    )
                    approachDistanceKm = distance
                    // ~40 km/h average accounts for mixed highway/hill-road travel typical
                    // of reaching a trek base - a labeled estimate, not a routed ETA.
                    approachTravelTimeHours = distance / 40.0
                }
            } catch (_: Exception) {
                // No internet / geocoding failed - leave the fields null rather than guessing.
            }
        }

        return TripItinerary(
            trekName = trek.name,
            startingPoint = startingPoint.ifBlank { "Not specified" },
            destination = trek.location,
            totalDistanceKm = trek.distanceKm,
            totalDurationDays = trek.durationDays,
            travelers = travelers,
            days = days,
            packingList = packingList,
            safetyNotes = trek.safetyWarnings.ifBlank { "Follow standard trekking safety precautions and inform someone of your route." },
            bestSeason = trek.bestSeason.ifBlank { "Check seasonal conditions before travel." },
            estimatedCost = totalBudget,
            costBreakdown = breakdown,
            approachDistanceKm = approachDistanceKm,
            approachTravelTimeHours = approachTravelTimeHours
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun buildDayPlan(trek: Trek): List<TripDayPlan> {
        val totalDays = trek.durationDays.coerceAtLeast(1)
        val perDayDistance = trek.distanceKm / totalDays

        return (1..totalDays).map { day ->
            val title = when (day) {
                1 -> "Arrival & Acclimatization"
                totalDays -> "Return Journey"
                totalDays - 1 -> "Summit / Highlight Day"
                else -> "Trail Day $day"
            }
            val activity = when (day) {
                1 -> "Travel to base village, gear check, short orientation walk."
                totalDays -> "Descend to base and depart for ${trek.location.substringBefore(",")}."
                else -> "Trek through the marked route covering approx. ${String.format("%.1f", perDayDistance)} km."
            }
            TripDayPlan(
                dayNumber = day,
                title = title,
                activity = activity,
                distanceKm = if (day == 1 || day == totalDays) 0.0 else perDayDistance,
                restStop = "Designated rest point / campsite ${day}",
                stay = if (day == totalDays) "Journey back (no stay)" else "Camp / guesthouse at trail point $day"
            )
        }
    }
}
