package com.pathsathi.app.engine

import com.pathsathi.app.data.model.ItineraryDay
import com.pathsathi.app.data.model.TripType
import com.pathsathi.app.data.repository.DemoDataProvider

/** Deterministic offline itinerary generator. No fake live data and no network required. */
object ItineraryEngine {
    fun generate(destination: String, days: Int, totalBudgetInr: Int, travelers: Int, tripType: TripType): List<ItineraryDay> {
        val safeDays = days.coerceAtLeast(1)
        val people = travelers.coerceAtLeast(1)
        val places = DemoDataProvider.explorePlaces(destination)
        val food = DemoDataProvider.foodOptions(destination)
        val stay = DemoDataProvider.stayOptions(destination)
        val transport = DemoDataProvider.transportOptions(destination)
        val perDayBudget = if (totalBudgetInr > 0) totalBudgetInr / safeDays else 0
        val stayPick = pickStayForType(stay, tripType)
        return (1..safeDays).map { day ->
            val count = when { places.isEmpty() -> 0; places.size == 1 -> 1; else -> 2 + ((day - 1) % 2) }
            val dayPlaces = rotate(places, day - 1).take(count)
            val rest = when (tripType) { TripType.RELAXATION -> 90; TripType.TREKKING, TripType.ADVENTURE, TripType.BACKPACKING -> 30; else -> 45 }
            val foodCost = (food.firstOrNull()?.avgCostInr ?: 0) * people
            val transportCost = transport.firstOrNull()?.estimatedCostInr ?: 0
            val rawCost = dayPlaces.sumOf { it.estimatedCostInr } + foodCost + transportCost
            val cost = if (perDayBudget > 0) rawCost.coerceAtMost(perDayBudget) else rawCost
            val route = dayPlaces.joinToString(" → ").ifBlank { "Local exploration around $destination" }
            val time = when (day) { 1 -> "09:00 start · 13:00 food · 16:00 explore · 19:00 rest"; else -> "08:30 start · 12:30 food · 15:30 explore · 19:00 rest" }
            ItineraryDay(day, dayPlaces.map { it.name }, route,
                transport.firstOrNull()?.mode?.replaceFirstChar { it.uppercase() } ?: "Local transport",
                food.map { it.name }.take(2), rest,
                stayPick?.name ?: "Arrange local stay",
                cost,
                "Day $day of $safeDays · $time · automatic guidance can adapt when you fall behind schedule.")
        }
    }
    private fun <T> rotate(list: List<T>, offset: Int): List<T> {
        if (list.isEmpty()) return list
        val n = ((offset % list.size) + list.size) % list.size
        return list.drop(n) + list.take(n)
    }
    private fun pickStayForType(stay: List<com.pathsathi.app.data.model.StayOption>, type: TripType) = when (type) {
        TripType.BACKPACKING, TripType.ADVENTURE -> stay.firstOrNull { it.type == "hostel" } ?: stay.firstOrNull()
        TripType.TREKKING -> stay.firstOrNull { it.type == "camping" } ?: stay.firstOrNull()
        TripType.FAMILY, TripType.COUPLE, TripType.RELAXATION -> stay.firstOrNull { it.type == "hotel" } ?: stay.firstOrNull()
        else -> stay.firstOrNull()
    }
}
