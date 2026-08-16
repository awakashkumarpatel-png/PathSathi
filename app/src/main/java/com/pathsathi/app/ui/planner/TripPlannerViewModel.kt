package com.pathsathi.app.ui.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.TripEntity
import com.pathsathi.app.data.model.TripType
import com.pathsathi.app.engine.ItineraryEngine
import com.pathsathi.app.engine.ItinerarySerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripPlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository

    private val _createdTripId = MutableStateFlow<Long?>(null)
    val createdTripId: StateFlow<Long?> = _createdTripId

    fun planTrip(
        destination: String,
        days: Int,
        budgetInr: Int,
        travelers: Int,
        tripType: TripType
    ) {
        if (destination.isBlank() || days <= 0) return
        viewModelScope.launch {
            val itinerary = ItineraryEngine.generate(destination, days, budgetInr, travelers, tripType)
            val trip = TripEntity(
                destination = destination.trim(),
                days = days,
                budgetInr = budgetInr,
                travelers = travelers,
                tripType = tripType.name,
                status = "PLANNED",
                itineraryJson = ItinerarySerializer.encode(itinerary),
                createdAtEpochMs = System.currentTimeMillis()
            )
            val id = repo.saveTrip(trip)
            _createdTripId.value = id
        }
    }
}
