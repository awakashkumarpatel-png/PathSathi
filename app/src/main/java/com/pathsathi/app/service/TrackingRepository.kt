package com.pathsathi.app.service

import com.pathsathi.app.data.model.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LiveTrackingState(
    val trekName: String = "",
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val points: List<TrackPoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elapsedMs: Long = 0L,
    val currentSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val currentAltitudeM: Double = 0.0,
    val maxAltitudeM: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val elevationLossM: Double = 0.0,
    val gpsAccuracyM: Float = 0f,
    val isOffRoute: Boolean = false,
    val deviationDistanceM: Double = 0.0,
    val bearingToRouteDeg: Float = 0f
)

/**
 * Bridges the foreground TrackingService (which keeps running when the
 * screen is locked or the app is backgrounded) with the Compose UI, which
 * simply observes this StateFlow - it never touches location APIs directly
 * once tracking has started.
 */
object TrackingRepository {
    private val _state = MutableStateFlow(LiveTrackingState())
    val state: StateFlow<LiveTrackingState> = _state

    fun update(transform: (LiveTrackingState) -> LiveTrackingState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = LiveTrackingState()
    }
}
