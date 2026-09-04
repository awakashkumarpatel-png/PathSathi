package com.pathsathi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches a real driving route between two points using the public OSRM
 * demo routing server (router.project-osrm.org) - free, no API key needed,
 * same no-hardcoded-key approach as the rest of the app's network features.
 * Because it's a shared public demo server (not meant for heavy production
 * traffic), failures are treated as a normal, expected case: callers should
 * fall back gracefully (e.g. a plain straight-line indicator) rather than
 * treat a failed lookup as a crash-worthy error.
 */
class RoutingRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Returns a list of (lat, lon) points along the route, or null if routing failed. */
    suspend fun getDrivingRoute(
        fromLat: Double, fromLon: Double,
        toLat: Double, toLon: Double
    ): Result<List<Pair<Double, Double>>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Routing service returned HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty routing response"))
                val json = JSONObject(body)
                if (json.optString("code") != "Ok") {
                    return@withContext Result.failure(Exception(json.optString("message", "No route found")))
                }
                val routes = json.getJSONArray("routes")
                if (routes.length() == 0) return@withContext Result.failure(Exception("No route found"))
                val coords = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                val points = mutableListOf<Pair<Double, Double>>()
                for (i in 0 until coords.length()) {
                    val pair = coords.getJSONArray(i)
                    // GeoJSON is [lon, lat]
                    points.add(Pair(pair.getDouble(1), pair.getDouble(0)))
                }
                Result.success(points)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
