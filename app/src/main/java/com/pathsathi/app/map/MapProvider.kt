package com.pathsathi.app.map

/**
 * Provider abstraction so the app can work fully offline today and swap in a
 * real online map/routing service later WITHOUT changing any UI/ViewModel
 * code — only the provider implementation behind this interface changes.
 */
interface MapProvider {
    /** Current device location, or null if unavailable/denied. Never fabricated. */
    suspend fun currentLocation(): GeoPoint?

    /** Nearby points of interest around [origin]. Offline implementations return bundled/saved data only. */
    suspend fun nearbyPlaces(origin: GeoPoint?, radiusKm: Double = 5.0): List<MapPlace>

    /** Distance + ETA between two points for the given travel mode. */
    fun distanceAndEta(from: GeoPoint, to: GeoPoint, mode: TravelMode): DistanceEstimate

    /** True if this provider can reach a live routing/traffic backend right now. */
    fun isLive(): Boolean
}

/**
 * Fully offline provider: current location comes from the device's last GPS
 * fix (no network lookup), nearby places come from saved/demo data, and
 * distance/ETA are computed locally via [GeoUtils]. This is always safe to
 * use with zero network/server dependency, matching the offline-first
 * requirement.
 */
class OfflineMapProvider(
    private val lastKnownLocation: suspend () -> GeoPoint?,
    private val savedAndDemoPlaces: suspend () -> List<MapPlace>
) : MapProvider {

    override suspend fun currentLocation(): GeoPoint? = lastKnownLocation()

    override suspend fun nearbyPlaces(origin: GeoPoint?, radiusKm: Double): List<MapPlace> {
        val all = savedAndDemoPlaces()
        if (origin == null) return all
        return all.filter { place ->
            val point = place.point ?: return@filter true // untagged saved places always shown
            GeoUtils.distanceKm(origin, point) <= radiusKm
        }
    }

    override fun distanceAndEta(from: GeoPoint, to: GeoPoint, mode: TravelMode): DistanceEstimate =
        GeoUtils.estimate(from, to, mode)

    override fun isLive(): Boolean = false
}

/**
 * Integration point for a real map/routing SDK (e.g. Google Maps Platform,
 * Mapbox, OpenRouteService). intentionally optional — wiring this up
 * requires an API key/billing account, which the "no mandatory backend / no
 * domain required" core requirement rules out for the offline build.
 *
 * To go live later:
 *   1. Add the chosen SDK's Gradle dependency in app/build.gradle.kts.
 *   2. Provide the API key via a non-committed local.properties entry
 *      (e.g. MAPS_API_KEY=...), read through BuildConfig — never hard-code it.
 *   3. Implement this interface (e.g. `GoogleMapsProvider : OnlineMapProvider`)
 *      calling the real routing/places APIs.
 *   4. Swap the provider instance in MapProviderFactory.get() below —
 *      no other file needs to change.
 */
interface OnlineMapProvider : MapProvider {
    /** Should return false gracefully (never throw) when the key/network is unavailable. */
    override fun isLive(): Boolean
}

object MapProviderFactory {
    /**
     * Returns the active provider. Currently always offline — flip this once
     * a real OnlineMapProvider implementation and API key are added, and add
     * a runtime fallback to [offline] when the online provider reports
     * isLive() == false or a call fails.
     */
    fun get(offline: OfflineMapProvider, online: OnlineMapProvider? = null): MapProvider {
        return if (online != null && online.isLive()) online else offline
    }
}
