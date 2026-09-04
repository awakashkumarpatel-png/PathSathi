package com.pathsathi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NearbyPlace(
    val name: String,
    val category: String, // hospital, police, pharmacy, fire_station, rescue
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double
)

/**
 * Finds nearby emergency help points (hospitals, police stations, pharmacies,
 * fire stations, and mountain-rescue points) using the free, key-less
 * OpenStreetMap Overpass API - consistent with the rest of the app's
 * no-API-key approach (osmdroid maps, Open-Meteo weather).
 */
class NearbyHelpRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun findNearby(lat: Double, lon: Double, radiusMeters: Int = 10_000): Result<List<NearbyPlace>> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    [out:json][timeout:20];
                    (
                      node["amenity"="hospital"](around:$radiusMeters,$lat,$lon);
                      node["amenity"="police"](around:$radiusMeters,$lat,$lon);
                      node["amenity"="pharmacy"](around:$radiusMeters,$lat,$lon);
                      node["amenity"="fire_station"](around:$radiusMeters,$lat,$lon);
                      node["emergency"="ambulance_station"](around:$radiusMeters,$lat,$lon);
                      node["emergency"="mountain_rescue"](around:$radiusMeters,$lat,$lon);
                    );
                    out body 40;
                """.trimIndent()

                val request = Request.Builder()
                    .url("https://overpass-api.de/api/interpreter")
                    .post(query.toRequestBody())
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("HTTP ${response.code}"))
                    }
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty response"))
                    val json = JSONObject(body)
                    val elements = json.getJSONArray("elements")
                    val places = mutableListOf<NearbyPlace>()
                    for (i in 0 until elements.length()) {
                        val el = elements.getJSONObject(i)
                        val tags = el.optJSONObject("tags")
                        val elLat = el.optDouble("lat")
                        val elLon = el.optDouble("lon")
                        if (elLat.isNaN() || elLon.isNaN()) continue
                        val category = when {
                            tags?.optString("amenity") == "hospital" -> "hospital"
                            tags?.optString("amenity") == "police" -> "police"
                            tags?.optString("amenity") == "pharmacy" -> "pharmacy"
                            tags?.optString("amenity") == "fire_station" -> "fire_station"
                            tags?.optString("emergency") == "mountain_rescue" -> "rescue"
                            tags?.optString("emergency") == "ambulance_station" -> "rescue"
                            else -> "other"
                        }
                        val name = tags?.optString("name").takeUnless { it.isNullOrBlank() }
                            ?: defaultNameFor(category)
                        places.add(
                            NearbyPlace(
                                name = name,
                                category = category,
                                latitude = elLat,
                                longitude = elLon,
                                distanceMeters = haversineMeters(lat, lon, elLat, elLon)
                            )
                        )
                    }
                    Result.success(places.sortedBy { it.distanceMeters })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun defaultNameFor(category: String) = when (category) {
        "hospital" -> "Hospital"
        "police" -> "Police Station"
        "pharmacy" -> "Pharmacy"
        "fire_station" -> "Fire Station"
        "rescue" -> "Rescue Point"
        else -> "Help Point"
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
