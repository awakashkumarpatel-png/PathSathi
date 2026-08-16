package com.pathsathi.app.map

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance in km. Pure math, no network/API required. */
    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)

        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(h), sqrt(1 - h))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Offline ETA model using fixed average speeds per mode. This is a rough
     * estimate, not live traffic-aware routing — callers should surface that
     * distinction to the user (see DistanceEstimate.isOfflineEstimate).
     */
    fun estimateMinutes(distanceKm: Double, mode: TravelMode): Int {
        val avgSpeedKmh = when (mode) {
            TravelMode.WALKING -> 4.5
            TravelMode.LOCAL_TRANSIT -> 15.0
            TravelMode.DRIVING -> 30.0
        }
        return ((distanceKm / avgSpeedKmh) * 60).toInt().coerceAtLeast(1)
    }

    fun estimate(a: GeoPoint, b: GeoPoint, mode: TravelMode): DistanceEstimate {
        val d = distanceKm(a, b)
        return DistanceEstimate(
            distanceKm = d,
            etaMinutes = estimateMinutes(d, mode),
            mode = mode,
            isOfflineEstimate = true
        )
    }
}
