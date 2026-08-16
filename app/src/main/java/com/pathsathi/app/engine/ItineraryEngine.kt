package com.pathsathi.app.engine

import com.pathsathi.app.data.model.ItineraryDay
import com.pathsathi.app.data.model.TripType
import com.pathsathi.app.data.repository.DemoDataProvider

/**
 * Generates a day-wise itinerary fully offline using bundled/demo destination
 * data and simple rule-based sequencing. This is intentionally NOT an AI call —
 * it is deterministic so the app works with zero network/server dependency.
 * A future "Advanced AI" layer (Phase 12) can replace/augment this generator
 * without touching callers, since it returns the same ItineraryDay model.
 */
object ItineraryEngine {

    fun generate(
        destination: String,
        days: Int,
        totalBudgetInr: Int,
        travelers: Int,
        tripType: TripType
    ): List<ItineraryDay> {
        val places = DemoDataProvider.explorePlaces(destination)
        val food = DemoDataProvider.foodOptions(destination)
        val stay = DemoDataProvider.stayOptions(destination)
        val transport = DemoDataProvider.transportOptions(destination)

        val perDayBudget = if (days > 0) totalBudgetInr / days else totalBudgetInr
        val stayPick = pickStayForType(stay, tripType)
        val safeDays = days.coerceAtLeast(1)

        return (1..safeDays).map { dayNum ->
            // Rotate through explore places so each day gets distinct spots.
            val dayPlaces = places
                .shuffledDeterministic(dayNum)
                .take(if (places.isEmpty()) 0 else (2..3).random(dayNum))

            val restMinutes = when (tripType) {
                TripType.RELAXATION -> 90
                TripType.TREKKING, TripType.ADVENTURE, TripType.BACKPACKING -> 30
                else -> 45
            }

            val estCost = (dayPlaces.sumOf { it.estimatedCostInr } +
                (food.firstOrNull()?.avgCostInr ?: 0) * travelers +
                (transport.firstOrNull()?.estimatedCostInr ?: 0))
                .coerceAtMost(perDayBudget.takeIf { it > 0 } ?: Int.MAX_VALUE)

            ItineraryDay(
                dayNumber = dayNum,
                places = dayPlaces.map { it.name },
                travelSequence = dayPlaces.joinToString(" → ") { it.name }
                    .ifEmpty { "Local exploration around $destination" },
                transportation = transport.firstOrNull()?.mode?.replaceFirstChar { it.uppercase() } ?: "Local transport",
                foodStops = food.map { it.name }.take(2),
                restTimeMinutes = restMinutes,
                stayInfo = stayPick?.name ?: "Local stay (to be arranged)",
                estimatedCostInr = estCost,
                scheduleNote = "Day $dayNum of $safeDays — plan is auto-generated and can shift if you fall behind schedule."
            )
        }
    }

    private fun pickStayForType(stay: List<com.pathsathi.app.data.model.StayOption>, tripType: TripType) =
        when (tripType) {
            TripType.BACKPACKING, TripType.ADVENTURE -> stay.firstOrNull { it.type == "hostel" } ?: stay.firstOrNull()
            TripType.TREKKING -> stay.firstOrNull { it.type == "camping" } ?: stay.firstOrNull()
            TripType.FAMILY, TripType.COUPLE, TripType.RELAXATION -> stay.firstOrNull { it.type == "hotel" } ?: stay.firstOrNull()
            else -> stay.firstOrNull()
        }

    // Deterministic pseudo-shuffle so the same day always gets a stable but varied order (no true randomness needed).
    private fun <T> List<T>.shuffledDeterministic(seed: Int): List<T> {
        if (isEmpty()) return this
        val offset = seed % size
        return subList(offset, size) + subList(0, offset)
    }

    private fun IntRange.random(seed: Int): Int {
        if (first >= last) return first
        return first + (seed % (last - first + 1))
    }
}
