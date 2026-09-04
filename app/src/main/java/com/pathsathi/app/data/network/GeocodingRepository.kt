package com.pathsathi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Geocodes a free-text place name to coordinates using OpenStreetMap's
 * Nominatim search API - free, no API key, consistent with the rest of the
 * app's approach (osmdroid maps, Overpass nearby-help, Open-Meteo weather).
 * Used by TripPlannerEngine to compute a real distance/travel-time estimate
 * from the traveler's starting point to the trek, instead of making one up.
 */
class GeocodingRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun geocode(place: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (place.isBlank()) return@withContext null
        try {
            val url = "https://nominatim.openstreetmap.org/search" +
                    "?q=${java.net.URLEncoder.encode(place, "UTF-8")}&format=json&limit=1"
            val request = Request.Builder()
                .url(url)
                // Nominatim's usage policy requires an identifying User-Agent.
                .header("User-Agent", "PathSathi-Android-App/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val array = JSONArray(body)
                if (array.length() == 0) return@withContext null
                val first = array.getJSONObject(0)
                val lat = first.optString("lat").toDoubleOrNull() ?: return@withContext null
                val lon = first.optString("lon").toDoubleOrNull() ?: return@withContext null
                Pair(lat, lon)
            }
        } catch (_: Exception) {
            null
        }
    }
}
