package com.pathsathi.app.location

import android.location.Location
import kotlin.math.*

/**
 * Small geo-math toolkit used for Route Deviation detection during live
 * tracking: how far is the hiker from the planned trek route, and which
 * direction should they head to get back on it.
 */
object GeoUtils {

    private const val EARTH_RADIUS_M = 6_371_000.0

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /** Bearing in degrees (0 = North, 90 = East) from point 1 to point 2. */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLon)
        val theta = atan2(y, x)
        return ((Math.toDegrees(theta) + 360) % 360).toFloat()
    }

    fun bearingToCompassLabel(deg: Float): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((deg + 22.5f) / 45f).toInt()) % 8
        return directions[if (index < 0) index + 8 else index]
    }

    /**
     * Shortest distance in meters from [point] to the segment between
     * [segStart] and [segEnd], using an equirectangular flat-earth
     * approximation (accurate enough at trek scale, cheap to compute on
     * every GPS fix).
     */
    private fun distanceToSegmentMeters(
        point: Pair<Double, Double>,
        segStart: Pair<Double, Double>,
        segEnd: Pair<Double, Double>
    ): Double {
        val refLat = Math.toRadians(point.first)
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = 111_320.0 * cos(refLat)

        fun toXY(p: Pair<Double, Double>) = Pair(
            (p.second - point.second) * metersPerDegLon,
            (p.first - point.first) * metersPerDegLat
        )

        val p0 = Pair(0.0, 0.0) // point itself, origin
        val a = toXY(segStart)
        val b = toXY(segEnd)

        val dx = b.first - a.first
        val dy = b.second - a.second
        val lengthSq = dx * dx + dy * dy

        if (lengthSq == 0.0) {
            return hypot(a.first - p0.first, a.second - p0.second)
        }

        var t = ((p0.first - a.first) * dx + (p0.second - a.second) * dy) / lengthSq
        t = t.coerceIn(0.0, 1.0)

        val projX = a.first + t * dx
        val projY = a.second + t * dy
        return hypot(p0.first - projX, p0.second - projY)
    }

    data class DeviationResult(
        val distanceMeters: Double,
        val nearestPoint: Pair<Double, Double>
    )

    /**
     * Finds the minimum distance from [current] to any segment of the
     * [routeWaypoints] polyline, and the point on that route nearest to the
     * hiker (used to compute a "head this way" bearing).
     */
    fun distanceToRoute(
        current: Pair<Double, Double>,
        routeWaypoints: List<Pair<Double, Double>>
    ): DeviationResult? {
        if (routeWaypoints.isEmpty()) return null
        if (routeWaypoints.size == 1) {
            val wp = routeWaypoints[0]
            return DeviationResult(haversineMeters(current.first, current.second, wp.first, wp.second), wp)
        }

        var minDist = Double.MAX_VALUE
        var nearest = routeWaypoints[0]

        for (i in 0 until routeWaypoints.size - 1) {
            val segStart = routeWaypoints[i]
            val segEnd = routeWaypoints[i + 1]
            val dist = distanceToSegmentMeters(current, segStart, segEnd)
            if (dist < minDist) {
                minDist = dist
                // Nearest point on this segment, for the bearing hint - use
                // the closer of the two segment endpoints as a simple proxy.
                val distToStart = haversineMeters(current.first, current.second, segStart.first, segStart.second)
                val distToEnd = haversineMeters(current.first, current.second, segEnd.first, segEnd.second)
                nearest = if (distToStart <= distToEnd) segStart else segEnd
            }
        }
        return DeviationResult(minDist, nearest)
    }

    fun distanceMeters(loc: Location, lat: Double, lon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, lat, lon, results)
        return results[0]
    }
}
