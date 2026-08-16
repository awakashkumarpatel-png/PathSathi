package com.pathsathi.app.map

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Optional online provider using public OpenStreetMap/Nominatim + OSRM services.
 * Offline provider remains the fallback, so no API key/server is mandatory.
 */
class OnlineOsmMapProvider : OnlineMapProvider {
    private val userAgent = "PathSathi/1.0 (travel companion app)"

    override suspend fun currentLocation(): GeoPoint? = null

    override suspend fun nearbyPlaces(origin: GeoPoint?, radiusKm: Double): List<MapPlace> = withContext(Dispatchers.IO) {
        if (origin == null) return@withContext emptyList()
        runCatching {
            val q = URLEncoder.encode("tourist attraction", StandardCharsets.UTF_8.toString())
            val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=12&q=$q&viewbox=${origin.lng-radiusKm/80},${origin.lat+radiusKm/110},${origin.lng+radiusKm/80},${origin.lat-radiusKm/110}&bounded=1")
            val json = request(url)
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val lat = o.optDouble("lat", Double.NaN)
                    val lon = o.optDouble("lon", Double.NaN)
                    if (lat.isFinite() && lon.isFinite()) add(MapPlace(o.optString("place_id", i.toString()), o.optString("display_name", "Place"), GeoPoint(lat, lon), "Tourist", false))
                }
            }
        }.getOrDefault(emptyList())
    }

    override fun distanceAndEta(from: GeoPoint, to: GeoPoint, mode: TravelMode): DistanceEstimate {
        return GeoUtils.estimate(from, to, mode)
    }

    override fun isLive(): Boolean = true

    suspend fun route(from: GeoPoint, to: GeoPoint, mode: TravelMode): DistanceEstimate? = withContext(Dispatchers.IO) {
        runCatching {
            val profile = when (mode) { TravelMode.WALKING -> "foot"; TravelMode.DRIVING -> "driving"; TravelMode.LOCAL_TRANSIT -> "driving" }
            val url = URL("https://router.project-osrm.org/route/v1/$profile/${from.lng},${from.lat};${to.lng},${to.lat}?overview=false")
            val o = org.json.JSONObject(request(url))
            val route = o.getJSONArray("routes").getJSONObject(0)
            DistanceEstimate(route.getDouble("distance") / 1000.0, (route.getDouble("duration") / 60.0).toInt().coerceAtLeast(1), mode, false)
        }.getOrNull()
    }

    private fun request(url: URL): String {
        val c = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10000; readTimeout = 15000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() }
    }
}
