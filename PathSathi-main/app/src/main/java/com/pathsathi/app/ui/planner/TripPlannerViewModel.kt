package com.pathsathi.app.ui.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.alerts.AlertScheduler
import com.pathsathi.app.data.db.TravelerEntity
import com.pathsathi.app.data.db.TripEntity
import com.pathsathi.app.data.model.ItineraryDay
import com.pathsathi.app.data.model.TripType
import com.pathsathi.app.engine.ItineraryEngine
import com.pathsathi.app.engine.ItinerarySerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripPlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository
    private val _created = MutableStateFlow<Long?>(null)
    val createdTripId: StateFlow<Long?> = _created
    private val _preview = MutableStateFlow<List<ItineraryDay>>(emptyList())
    val preview: StateFlow<List<ItineraryDay>> = _preview
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun previewTrip(dest: String, days: Int, budget: Int, travelers: Int, type: TripType) {
        if (dest.isBlank() || days <= 0 || budget <= 0 || travelers <= 0) {
            _error.value = "Enter a destination, valid days, budget and traveler count."
            return
        }
        _error.value = null
        _preview.value = ItineraryEngine.generate(dest.trim(), days, budget, travelers, type)
    }

    fun clearPreview() { _preview.value = emptyList() }

    fun confirmTrip(
        dest: String,
        days: Int,
        budget: Int,
        travelers: Int,
        type: TripType,
        startDelayHours: Int
    ) = viewModelScope.launch {
        if (dest.isBlank() || days <= 0 || budget <= 0 || travelers <= 0) return@launch
        val itinerary = if (_preview.value.isNotEmpty()) {
            _preview.value
        } else {
            ItineraryEngine.generate(dest.trim(), days, budget, travelers, type)
        }
        val trip = TripEntity(
            destination = dest.trim(),
            days = days,
            budgetInr = budget,
            travelers = travelers,
            tripType = type.name,
            status = "PLANNED",
            itineraryJson = ItinerarySerializer.encode(itinerary),
            createdAtEpochMs = System.currentTimeMillis()
        )
        val id = repo.saveTrip(trip)
        repeat(travelers) { index ->
            repo.saveTraveler(
                TravelerEntity(
                    tripId = id,
                    name = if (travelers == 1) "Me" else "Traveler ${index + 1}",
                    isGroupLeader = index == 0
                )
            )
        }
        AlertScheduler.scheduleTripReminders(
            getApplication(),
            trip.destination,
            startDelayHours.coerceAtLeast(0) * 60L
        )
        _created.value = id
    }
}
