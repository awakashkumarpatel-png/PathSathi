package com.pathsathi.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Fetches the device's current location automatically so weather
 * and nearby-trek suggestions can load without user input, and
 * streams continuous updates for live trek tracking.
 */
class LocationHelper(private val context: Context) {

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // Guard against SecurityException: Google Play Services' FusedLocationProviderClient
        // throws at runtime (not just lint) if location permission isn't granted. Every
        // caller in the app expects a plain null for "location unavailable" rather than a
        // crash, so both the permission check and a catch-all below enforce that.
        if (!hasLocationPermission()) return null
        return try {
            suspendCancellableCoroutine { cont ->
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            cont.resume(Pair(location.latitude, location.longitude))
                        } else {
                            cont.resume(null)
                        }
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Live stream of location updates for active GPS tracking during a trek.
     * Emits roughly every 3 seconds or 5 meters of movement, whichever comes first.
     * Emits nothing and closes quietly if location permission isn't granted, rather
     * than throwing, so a collector doesn't need its own try/catch.
     */
    @SuppressLint("MissingPermission")
    fun trackLocation(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }
}
