package com.pathsathi.app.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.TripEntity
import com.pathsathi.app.engine.AutoTravelEngine
import com.pathsathi.app.engine.ItinerarySerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TripsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository

    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips

    private val _guidance = MutableStateFlow("")
    val guidance: StateFlow<String> = _guidance

    init {
        viewModelScope.launch { repo.observeTrips().collect { _trips.value = it } }
    }

    fun startTrip(trip: TripEntity) {
        viewModelScope.launch {
            repo.updateTrip(trip.copy(status = "ACTIVE", startedAtEpochMs = System.currentTimeMillis(), currentDayIndex = 0))
        }
    }

    fun completeTrip(trip: TripEntity) {
        viewModelScope.launch {
            repo.updateTrip(trip.copy(status = "COMPLETED", completedAtEpochMs = System.currentTimeMillis()))
        }
    }

    fun advanceDay(trip: TripEntity) {
        viewModelScope.launch {
            val days = ItinerarySerializer.decode(trip.itineraryJson)
            val nextIndex = (trip.currentDayIndex + 1).coerceAtMost((days.size - 1).coerceAtLeast(0))
            repo.updateTrip(trip.copy(currentDayIndex = nextIndex))
        }
    }

    fun refreshGuidance(trip: TripEntity) {
        viewModelScope.launch {
            val days = ItinerarySerializer.decode(trip.itineraryJson)
            val dayIdx = trip.currentDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
            val day = days.getOrNull(dayIdx)
            val spent = repo.observeTotalSpent(trip.id).first()
            val perDayBudget = if (trip.days > 0) trip.budgetInr / trip.days else trip.budgetInr

            _guidance.value = AutoTravelEngine.evaluate(
                AutoTravelEngine.ProgressInput(
                    dayNumber = dayIdx + 1,
                    totalDays = trip.days,
                    plannedMinutesElapsedByNow = 120, // offline estimate baseline
                    actualMinutesElapsed = 120, // no live GPS timeline tracking yet; stays neutral offline
                    budgetForDayInr = perDayBudget,
                    spentSoFarInr = spent,
                    nextDestinationName = day?.places?.firstOrNull() ?: trip.destination,
                    nextDestinationDistanceKm = 2.0 // offline estimate placeholder until live GPS distance is wired in
                )
            )
        }
    }
}
