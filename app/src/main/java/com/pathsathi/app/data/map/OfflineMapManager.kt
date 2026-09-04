package com.pathsathi.app.data.map

import android.content.Context
import com.pathsathi.app.data.model.Trek
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * Downloads and manages offline map tile regions for treks, using osmdroid's
 * CacheManager. Tiles are written into the same shared tile cache the live
 * MapScreen already reads from (Configuration.osmdroidTileCache), so once a
 * region is downloaded, MapScreen automatically renders it from cache with
 * no network required.
 */
object OfflineMapManager {

    const val MIN_ZOOM = 12
    const val MAX_ZOOM = 16

    fun boundingBoxForTrek(trek: Trek): BoundingBox {
        val points = mutableListOf(
            trek.latitude to trek.longitude,
            trek.destinationLatitude to trek.destinationLongitude
        )
        points.addAll(trek.routeWaypoints)

        val north = points.maxOf { it.first }
        val south = points.minOf { it.first }
        val east = points.maxOf { it.second }
        val west = points.minOf { it.second }

        // Pad the box so the surrounding area is cached too
        val latPad = ((north - south).coerceAtLeast(0.01)) * 0.3 + 0.02
        val lonPad = ((east - west).coerceAtLeast(0.01)) * 0.3 + 0.02

        return BoundingBox(north + latPad, east + lonPad, south - latPad, west - lonPad)
    }

    fun downloadArea(
        context: Context,
        mapView: MapView,
        bb: BoundingBox,
        onProgress: (downloaded: Int, total: Int) -> Unit,
        onComplete: (success: Boolean) -> Unit
    ) {
        val cacheManager = CacheManager(mapView)
        var totalTiles = 0
        cacheManager.downloadAreaAsync(
            context,
            bb,
            MIN_ZOOM,
            MAX_ZOOM,
            object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    onComplete(true)
                }

                override fun onTaskFailed(errors: Int) {
                    onComplete(false)
                }

                override fun downloadStarted() {}

                override fun setPossibleTilesInArea(total: Int) {
                    totalTiles = total
                    onProgress(0, total)
                }

                override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                    onProgress(progress, totalTiles)
                }
            }
        )
    }

    fun deleteArea(context: Context, mapView: MapView, bb: BoundingBox) {
        val cacheManager = CacheManager(mapView)
        cacheManager.cleanAreaAsync(context, bb, MIN_ZOOM, MAX_ZOOM)
    }
}
