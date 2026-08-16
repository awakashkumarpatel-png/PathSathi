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

    private val provider = MapProviderFactory.get(
        offline = OfflineMapProvider(
            lastKnownLocation = { _currentLocation.value },
            savedAndDemoPlaces = {
                repo.observeSavedPlaces().first().mapNotNull { p ->
                    if (p.lat == null || p.lng == null) null
                    else MapPlace(p.id.toString(), p.name, GeoPoint(p.lat, p.lng), p.category, true)
                }
            }
        )
    )

    init {
        viewModelScope.launch { repo.observeSavedPlaces().collect { _savedPlaces.value = it; recomputeEstimate() } }
        viewModelScope.launch {
            repo.observeActiveTrip().collect { trip ->
                val days = trip?.let { ItinerarySerializer.decode(it.itineraryJson) }.orEmpty()
                val idx = trip?.currentDayIndex?.coerceIn(0, (days.size - 1).coerceAtLeast(0)) ?: 0
                _nextDestinationName.value = days.getOrNull(idx)?.places?.firstOrNull() ?: trip?.destination
                recomputeEstimate()
            }
        }
    }

    fun setMode(mode: TravelMode) { _selectedMode.value = mode; recomputeEstimate() }

    private fun recomputeEstimate() {
        val origin = _currentLocation.value ?: return
        val destinationName = _nextDestinationName.value ?: return
        viewModelScope.launch {
            val saved = repo.observeSavedPlaces().first().firstOrNull {
                it.name.equals(destinationName, ignoreCase = true) && it.lat != null && it.lng != null
            }
            val target = if (saved != null) {
                GeoPoint(saved.lat!!, saved.lng!!)
            } else {
                com.pathsathi.app.map.GeoUtils.geocode(getApplication(), destinationName)
            }
            _nextDestinationEstimate.value = target?.let { provider.distanceAndEta(origin, it, _selectedMode.value) }
        }
    }

    fun refreshLocation(context: Context) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { _locationText.value = "Location permission not granted."; return }
        try {
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc == null) _locationText.value = "No recent location fix available yet. Try again outdoors."
                    else {
                        _currentLocation.value = GeoPoint(loc.latitude, loc.longitude)
                        _locationText.value = "Lat ${"%.5f".format(loc.latitude)}, Lng ${"%.5f".format(loc.longitude)}"
                        viewModelScope.launch { _nearbyPlaces.value = provider.nearbyPlaces(_currentLocation.value); recomputeEstimate() }
                    }
                }
                .addOnFailureListener { _locationText.value = "Unable to fetch location right now." }
        } catch (_: SecurityException) { _locationText.value = "Location permission not granted." }
    }

    fun saveCurrentAsPlace(name: String, category: String) {
        val loc = _currentLocation.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch { repo.savePlace(SavedPlaceEntity(name = name.trim(), category = category, lat = loc.lat, lng = loc.lng, note = "Saved from current GPS location")) }
    }
}
