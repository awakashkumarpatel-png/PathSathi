package com.pathsathi.app.ui.trips

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.engine.ItinerarySerializer
import com.pathsathi.app.ui.theme.PsSurfaceAlt

@Composable
fun LiveTripScreen(
    tripId: Long,
    onOpenBudget: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    vm: TripsViewModel = viewModel()
) {
    val context = LocalContext.current
    val trips by vm.trips.collectAsState()
    val guidance by vm.guidance.collectAsState()
    val distance by vm.distanceKm.collectAsState()
    val eta by vm.etaMinutes.collectAsState()
    val tracking by vm.tracking.collectAsState()
    val trip = trips.firstOrNull { it.id == tripId }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            trip?.let { vm.startAutoTracking(context, it) }
        }
    }

    DisposableEffect(trip?.id, trip?.status) {
        if (trip?.status == "ACTIVE") {
            val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (allowed) vm.startAutoTracking(context, trip) else {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
        onDispose { vm.stopAutoTracking() }
    }

    LaunchedEffect(trip?.id, trip?.currentDayIndex) { trip?.let(vm::refreshGuidance) }

    if (trip == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Loading trip…") }
        return
    }

    val days = ItinerarySerializer.decode(trip.itineraryJson)
    val index = trip.currentDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
    val day = days.getOrNull(index)

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(trip.destination, style = MaterialTheme.typography.headlineMedium)
        Text("Status: ${trip.status}")

        Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sathi Auto Mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (tracking) "Automatic GPS tracking is active."
                    else "Automatic GPS tracking is not active."
                )
                Text(guidance.ifBlank { "Start the trip and allow location permission for live guidance." })
                if (distance != null) Text("Next stop: %.1f km · ETA %d min".format(distance, eta ?: 0))
            }
        }

        day?.let { currentDay ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Day ${currentDay.dayNumber} plan", style = MaterialTheme.typography.titleMedium)
                    Text("Route: ${currentDay.travelSequence}")
                    Text("Transport: ${currentDay.transportation}")
                    Text("Food: ${currentDay.foodStops.joinToString()}")
                    Text("Stay: ${currentDay.stayInfo}")
                    Text("Estimated cost: ₹${currentDay.estimatedCostInr}")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (trip.status == "PLANNED") Button(onClick = { vm.startTrip(trip) }) { Text("Start Trip") }
            if (trip.status == "ACTIVE") {
                OutlinedButton(onClick = { vm.advanceDay(trip) }) { Text("Next Day") }
                Button(onClick = { vm.completeTrip(trip) }) { Text("Complete Trip") }
            }
            OutlinedButton(onClick = { vm.refreshGuidance(trip) }) { Text("Refresh") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenBudget) { Text("Open Budget") }
            OutlinedButton(onClick = onOpenMemory) { Text("Add Memory") }
        }
    }
}
