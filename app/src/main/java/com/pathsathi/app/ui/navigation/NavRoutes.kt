package com.pathsathi.app.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val TRIPS = "trips"
    const val MAP = "map"
    const val SATHI = "sathi"

    const val PLANNER = "planner"
    const val LIVE_TRIP = "live_trip/{tripId}"
    fun liveTrip(tripId: Long) = "live_trip/$tripId"

    const val BUDGET = "budget/{tripId}"
    fun budget(tripId: Long) = "budget/$tripId"

    const val STAY = "stay"
    const val FOOD = "food"
    const val TRANSPORT = "transport"
    const val SAFETY = "safety"
    const val SETTINGS = "settings"
    const val MEMORY = "memory"
}
