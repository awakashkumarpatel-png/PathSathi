package com.pathsathi.app.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pathsathi.app.data.network.NetworkModeManager
import com.pathsathi.app.data.network.RoutingRepository
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.location.LocationHelper
import com.pathsathi.app.service.TrackingRepository
import com.pathsathi.app.ui.theme.BlueAccent
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen(trekId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val trek = remember { TrekRepository.getById(trekId) }
    val liveState by TrackingRepository.state.collectAsState()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var liveRouteOverlay by remember { mutableStateOf<Polyline?>(null) }
    var approachRouteOverlay by remember { mutableStateOf<Polyline?>(null) }
    var isRouting by remember { mutableStateOf(false) }
    val isOnline by NetworkModeManager.isOnlineMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val locationErrorMsg = stringResource(R.string.map_location_error)
    val routeErrorMsg = stringResource(R.string.map_route_error)

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().osmdroidTileCache = context.getExternalFilesDir("osmdroid_tiles")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(trek?.name ?: stringResource(R.string.map_default_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text(if (isOnline) stringResource(R.string.map_online) else stringResource(R.string.map_offline)) }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (isOnline) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val t = trek ?: return@ExtendedFloatingActionButton
                            scope.launch {
                                isRouting = true
                                val loc = LocationHelper(context).getCurrentLocation()
                                if (loc == null) {
                                    isRouting = false
                                    snackbarHostState.showSnackbar(locationErrorMsg)
                                    return@launch
                                }
                                val result = RoutingRepository().getDrivingRoute(
                                    loc.first, loc.second, t.latitude, t.longitude
                                )
                                isRouting = false
                                result.onSuccess { points ->
                                    approachRouteOverlay?.let { mapViewRef?.overlays?.remove(it) }
                                    val line = Polyline().apply {
                                        setPoints(points.map { GeoPoint(it.first, it.second) })
                                        outlinePaint.strokeWidth = 9f
                                        outlinePaint.color = BlueAccent.toArgb()
                                    }
                                    mapViewRef?.overlays?.add(line)
                                    approachRouteOverlay = line
                                    mapViewRef?.invalidate()
                                    mapViewRef?.controller?.animateTo(GeoPoint(loc.first, loc.second))
                                }
                                result.onFailure {
                                    snackbarHostState.showSnackbar(routeErrorMsg)
                                }
                            }
                        },
                        icon = {
                            if (isRouting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Directions, contentDescription = null)
                            }
                        },
                        text = { Text(if (isRouting) stringResource(R.string.map_routing) else stringResource(R.string.map_route_from_me)) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                FloatingActionButton(onClick = {
                    scope.launch {
                        val loc = LocationHelper(context).getCurrentLocation()
                        if (loc != null) {
                            mapViewRef?.controller?.animateTo(GeoPoint(loc.first, loc.second))
                        } else {
                            snackbarHostState.showSnackbar(locationErrorMsg)
                        }
                    }
                }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter")
                }
            }
        }
    ) { padding ->
        if (trek == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.map_trek_not_found))
            }
            return@Scaffold
        }

        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(12.0)

                    val startPoint = GeoPoint(trek.latitude, trek.longitude)
                    controller.setCenter(startPoint)

                    // Start marker
                    overlays.add(Marker(this).apply {
                        position = startPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Start: ${trek.name}"
                    })

                    // Destination marker (if different from start)
                    if (trek.destinationLatitude != trek.latitude || trek.destinationLongitude != trek.longitude) {
                        overlays.add(Marker(this).apply {
                            position = GeoPoint(trek.destinationLatitude, trek.destinationLongitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Destination"
                        })
                    }

                    // Route polyline from waypoints
                    if (trek.routeWaypoints.isNotEmpty()) {
                        val line = Polyline().apply {
                            setPoints(trek.routeWaypoints.map { GeoPoint(it.first, it.second) })
                            outlinePaint.strokeWidth = 8f
                        }
                        overlays.add(line)

                        trek.routeWaypoints.forEachIndexed { index, wp ->
                            overlays.add(Marker(this).apply {
                                position = GeoPoint(wp.first, wp.second)
                                title = "Waypoint ${index + 1}"
                            })
                        }
                    }

                    mapViewRef = this
                }
            },
            update = { mapView ->
                // Draw live tracking route on top, if a tracking session is active/recorded
                liveRouteOverlay?.let { mapView.overlays.remove(it) }
                if (liveState.points.isNotEmpty()) {
                    val liveLine = Polyline().apply {
                        setPoints(liveState.points.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(liveLine)
                    liveRouteOverlay = liveLine
                    mapView.invalidate()
                }
            }
        )
    }
}
