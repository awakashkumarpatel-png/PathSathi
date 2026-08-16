package com.pathsathi.app.data.model

/** Source label so the UI can honestly show whether data is offline/demo/live — never fabricated. */
enum class DataSource { OFFLINE_SAVED, DEMO, LIVE, ESTIMATED }

enum class TripType {
    TREKKING, ADVENTURE, RELAXATION, FAMILY, COUPLE, RELIGIOUS, PHOTOGRAPHY, BACKPACKING
}

enum class TripStatus { PLANNED, ACTIVE, COMPLETED, CANCELLED }

data class ItineraryDay(
    val dayNumber: Int,
    val places: List<String>,
    val travelSequence: String,
    val transportation: String,
    val foodStops: List<String>,
    val restTimeMinutes: Int,
    val stayInfo: String,
    val estimatedCostInr: Int,
    val scheduleNote: String
)

data class ExplorePlace(
    val id: String,
    val name: String,
    val category: String, // attraction, nature, mountain, historical, religious, adventure, hidden, market, food, photography
    val description: String,
    val distanceKm: Double,
    val suggestedDurationHours: Double,
    val estimatedCostInr: Int,
    val usefulInfo: String,
    val source: DataSource = DataSource.DEMO
)

data class StayOption(
    val id: String,
    val name: String,
    val type: String, // hotel, guest house, hostel, camping
    val pricePerNightInr: Int,
    val distanceFromCenterKm: Double,
    val notes: String,
    val source: DataSource = DataSource.DEMO
)

data class FoodOption(
    val id: String,
    val name: String,
    val cuisine: String,
    val avgCostInr: Int,
    val popularDishes: List<String>,
    val budgetFriendly: Boolean,
    val source: DataSource = DataSource.DEMO
)

data class TransportOption(
    val id: String,
    val mode: String, // bus, train, taxi, local, walking
    val fromTo: String,
    val estimatedCostInr: Int,
    val estimatedDurationMinutes: Int,
    val notes: String,
    val source: DataSource = DataSource.DEMO
)

data class EmergencyInfo(
    val id: String,
    val category: String, // hospital, police, helpline, local info
    val name: String,
    val phone: String,
    val notes: String,
    val source: DataSource = DataSource.DEMO
)
