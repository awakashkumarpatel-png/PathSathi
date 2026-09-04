package com.pathsathi.app.ui.screens

import android.Manifest
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pathsathi.app.location.GeoUtils
import com.pathsathi.app.service.TrackingRepository
import com.pathsathi.app.service.TrackingService
import java.util.concurrent.TimeUnit

/**
 * UI only observes TrackingRepository's state - the actual GPS work happens
 * in TrackingService (a foreground service), so tracking keeps running when
 * the screen locks or the app goes to background.
 */
@Composable
fun TrackingScreen(trekName: String, onBack: () -> Unit, onFinished: (distanceKm: Double, durationMs: Long) -> Unit) {
    val context = LocalContext.current
    val state by TrackingRepository.state.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_title, trekName)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBlock(stringResource(R.string.stat_distance), String.format("%.2f km", state.distanceMeters / 1000.0))
                        StatBlock(stringResource(R.string.stat_time), formatDuration(state.elapsedMs))
                        StatBlock(stringResource(R.string.stat_speed), String.format("%.1f km/h", state.currentSpeedKmh))
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBlock(stringResource(R.string.stat_max_speed), String.format("%.1f km/h", state.maxSpeedKmh))
                        StatBlock(stringResource(R.string.stat_elev_gain), String.format("%.0f m", state.elevationGainM))
                        StatBlock(stringResource(R.string.stat_altitude), String.format("%.0f m", state.currentAltitudeM))
                    }
                }
            }

            if (state.isTracking && state.gpsAccuracyM == 0f && state.points.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.tracking_waiting_gps),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isTracking && state.isOffRoute) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.tracking_off_route, state.deviationDistanceM.toInt()),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                stringResource(R.string.tracking_head_direction, GeoUtils.bearingToCompassLabel(state.bearingToRouteDeg)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (!hasLocationPermission) {
                Text(
                    stringResource(R.string.tracking_permission_needed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    stringResource(R.string.tracking_background_note),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!state.isTracking) {
                    Button(
                        enabled = hasLocationPermission,
                        onClick = { TrackingService.start(context, trekName) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_start))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            TrackingService.sendAction(
                                context,
                                if (state.isPaused) TrackingService.ACTION_RESUME else TrackingService.ACTION_PAUSE
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (state.isPaused) stringResource(R.string.action_resume) else stringResource(R.string.action_pause))
                    }
                    Button(
                        onClick = {
                            TrackingService.sendAction(context, TrackingService.ACTION_STOP)
                            onFinished(state.distanceMeters / 1000.0, state.elapsedMs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_stop))
                    }
                }
            }

            Text(stringResource(R.string.tracking_route_log), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.points.takeLast(50).reversed()) { point ->
                    Text(
                        "${String.format("%.5f", point.latitude)}, ${String.format("%.5f", point.longitude)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatDuration(ms: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
