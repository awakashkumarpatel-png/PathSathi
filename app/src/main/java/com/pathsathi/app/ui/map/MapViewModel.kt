package com.pathsathi.app.ui.map

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.SavedPlaceEntity
import com.pathsathi.app.engine.ItinerarySerializer
import com.pathsathi.app.map.DistanceEstimate
import com.pathsathi.app.map.GeoPoint
import com.pathsathi.app.map.MapPlace
import com.pathsathi.app.map.MapProviderFactory
import com.pathsathi.app.map.OfflineMapProvider
import com.pathsathi.app.map.TravelMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MapViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository

    private val _savedPlaces = MutableStateFlow<List<SavedPlaceEntity>>(emptyList())
    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = _savedPlaces

    private val _locationText = MutableStateFlow("")
    val locationText: StateFlow<String> = _locationText

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)

    private val _nearbyPlaces = MutableStateFlow<List<MapPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<MapPlace>> = _nearbyPlaces

    private val _nextDestinationName = MutableStateFlow<String?>(null)
    val nextDestinationName: StateFlow<String?> = _nextDestinationName

    private val _selectedMode = MutableStateFlow(TravelMode.WALKING)
    val selectedMode: StateFlow<TravelMode> = _selectedMode

    private val _nextDestinationEstimate = MutableStateFlow<DistanceEstimate?>(null)
    val nextDestinationEstimate: StateFlow<DistanceEstimate?> = _nextDestinationEstimate

    // Offline-first provider: current location from device GPS fix, nearby places from saved+demo data.
    // See com.pathsathi.app.map.MapProvider for how an online provider would plug in later.
    private val provider = MapProviderFactory.get(
        offline = OfflineMapProvider(
            lastKnownLocation = { _currentLocation.value },
            savedAndDemoPlaces = {
                repo.observeSavedPlaces().first().mapNotNull { p ->
                    if (p.lat == null || p.lng == null) null
                    else MapPlace(p.id.toString(), p.name, GeoPoint(p.lat, p.lng), p.category, isSaved = true)
                }
            }
        )
    )

    init {
        viewModelScope.launch { repo.observeSavedPlaces().collect { _savedPlaces.value = it } }
        viewModelScope.launch {
            repo.observeActiveTrip().collect { trip ->
                if (trip == null) {
                    _nextDestinationName.value = null
                    return@collect
                }
                val days = ItinerarySerializer.decode(trip.itineraryJson)
                val idx = trip.currentDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
                _nextDestinationName.value = days.getOrNull(idx)?.places?.firstOrNull() ?: trip.destination
                recomputeEstimate()
            }
        }
    }

    fun setMode(mode: TravelMode) {
        _selectedMode.value = mode
        recomputeEstimate()
    }

    private fun recomputeEstimate() {
        val origin = _currentLocation.value ?: return
        // Without a geocoded destination point yet (no online geocoding wired up),
        // we don't fabricate a distance — the estimate stays null until we have a real point.
        _nextDestinationEstimate.value = null
    }

    fun refreshLocation(context: Context) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _locationText.value = "Location permission not granted."
            return
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _currentLocation.value = GeoPoint(loc.latitude, loc.longitude)
                    _locationText.value = "Lat ${"%.5f".format(loc.latitude)}, Lng ${"%.5f".format(loc.longitude)}"
                    viewModelScope.launch {
                        _nearbyPlaces.value = provider.nearbyPlaces(_currentLocation.value)
                    }
                } else {
                    _locationText.value = "No recent location fix available yet. Move outdoors or try again."
                }
            }.addOnFailureListener {
                _locationText.value = "Unable to fetch location right now."
            }
        } catch (e: SecurityException) {
            _locationText.value = "Location permission not granted."
        }
    }

    fun saveCurrentAsPlace(name: String, category: String) {
        val loc = _currentLocation.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.savePlace(SavedPlaceEntity(name = name, category = category, lat = loc.lat, lng = loc.lng, note = ""))
        }
    }
}

