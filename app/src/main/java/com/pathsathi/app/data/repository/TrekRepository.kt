package com.pathsathi.app.data.repository

import com.pathsathi.app.data.model.Difficulty
import com.pathsathi.app.data.model.Trek

/**
 * Offline-first local trek catalog. Works with no network at all.
 * Later this can be swapped for a Room database without touching the UI.
 * All original treks preserved; route waypoints and extra detail fields added.
 */
object TrekRepository {

    val treks: List<Trek> = listOf(
        Trek(
            id = "kedarkantha",
            name = "Kedarkantha Trek",
            location = "Uttarakhand, India",
            distanceKm = 20.0,
            elevationM = 3810,
            difficulty = Difficulty.EASY,
            durationDays = 5,
            description = "A snow-laden winter trek famous for its summit sunrise views over the Himalayan range.",
            latitude = 31.0224,
            longitude = 78.1911,
            destinationLatitude = 31.0512,
            destinationLongitude = 78.2354,
            routeWaypoints = listOf(
                31.0224 to 78.1911,
                31.0310 to 78.2050,
                31.0410 to 78.2180,
                31.0512 to 78.2354
            ),
            bestSeason = "December - April",
            permitsRequired = "Forest entry permit (arranged locally at Sankri)",
            equipmentList = listOf("Trekking shoes", "Down jacket", "Gaiters", "Trekking poles", "Headlamp"),
            safetyWarnings = "Heavy snowfall possible above 2800m; carry microspikes in peak winter."
        ),
        Trek(
            id = "valley-of-flowers",
            name = "Valley of Flowers",
            location = "Uttarakhand, India",
            distanceKm = 38.0,
            elevationM = 3658,
            difficulty = Difficulty.MODERATE,
            durationDays = 6,
            description = "A UNESCO World Heritage site bursting with alpine flowers during the monsoon season.",
            latitude = 30.7280,
            longitude = 79.6046,
            destinationLatitude = 30.7280,
            destinationLongitude = 79.6046,
            routeWaypoints = listOf(
                30.6900 to 79.5700,
                30.7050 to 79.5850,
                30.7280 to 79.6046
            ),
            bestSeason = "July - September",
            permitsRequired = "National Park entry fee at Ghangaria checkpoint",
            equipmentList = listOf("Rain jacket", "Waterproof boots", "Camera", "Trekking poles"),
            safetyWarnings = "Trail can be slippery in monsoon; avoid picking flowers (protected park)."
        ),
        Trek(
            id = "roopkund",
            name = "Roopkund Trek",
            location = "Uttarakhand, India",
            distanceKm = 53.0,
            elevationM = 5029,
            difficulty = Difficulty.HARD,
            durationDays = 8,
            description = "A mysterious high-altitude glacial lake trek through dense forests and ridges.",
            latitude = 30.2600,
            longitude = 79.7325,
            destinationLatitude = 30.2600,
            destinationLongitude = 79.7325,
            routeWaypoints = listOf(
                30.1500 to 79.6800,
                30.1900 to 79.7000,
                30.2300 to 79.7150,
                30.2600 to 79.7325
            ),
            bestSeason = "May - June, September - October",
            permitsRequired = "Forest permit via registered trek operator",
            equipmentList = listOf("Crampons", "Down jacket", "Thermal layers", "Trekking poles", "First-aid kit"),
            safetyWarnings = "High altitude sickness risk above 4500m; acclimatization days recommended."
        ),
        Trek(
            id = "everest-base-camp",
            name = "Everest Base Camp",
            location = "Solukhumbu, Nepal",
            distanceKm = 130.0,
            elevationM = 5364,
            difficulty = Difficulty.EXTREME,
            durationDays = 14,
            description = "The classic trek to the base of the world's tallest mountain through Sherpa villages.",
            latitude = 28.0026,
            longitude = 86.8528,
            destinationLatitude = 28.0026,
            destinationLongitude = 86.8528,
            routeWaypoints = listOf(
                27.6869 to 86.7314,
                27.8167 to 86.7150,
                27.9167 to 86.7833,
                28.0026 to 86.8528
            ),
            bestSeason = "March - May, September - November",
            permitsRequired = "Sagarmatha National Park permit + local area permit",
            equipmentList = listOf("Down suit", "Sleeping bag (-20C)", "Altitude medication kit", "Sunglasses (UV)", "Trekking poles"),
            safetyWarnings = "Serious altitude sickness risk; consult a doctor before attempting."
        )
    )

    fun getById(id: String): Trek? = treks.find { it.id == id }

    fun search(query: String): List<Trek> {
        if (query.isBlank()) return treks
        val q = query.trim().lowercase()
        return treks.filter {
            it.name.lowercase().contains(q) || it.location.lowercase().contains(q)
        }
    }

    fun filterAndSort(
        difficulty: Difficulty? = null,
        maxDistanceKm: Double? = null,
        sortBy: SortOption = SortOption.NAME
    ): List<Trek> {
        var result = treks
        if (difficulty != null) result = result.filter { it.difficulty == difficulty }
        if (maxDistanceKm != null) result = result.filter { it.distanceKm <= maxDistanceKm }
        return when (sortBy) {
            SortOption.NAME -> result.sortedBy { it.name }
            SortOption.DISTANCE -> result.sortedBy { it.distanceKm }
            SortOption.ELEVATION -> result.sortedBy { it.elevationM }
            SortOption.DIFFICULTY -> result.sortedBy { it.difficulty.ordinal }
        }
    }
}

enum class SortOption(val label: String) {
    NAME("Name"),
    DISTANCE("Distance"),
    ELEVATION("Elevation"),
    DIFFICULTY("Difficulty")
}
