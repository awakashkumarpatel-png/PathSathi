package com.pathsathi.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.map.TravelMode

@Composable
fun MapScreen(vm: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val savedPlaces by vm.savedPlaces.collectAsState()
    val nearbyPlaces by vm.nearbyPlaces.collectAsState()
    val locationText by vm.locationText.collectAsState()
    val nextDestination by vm.nextDestinationName.collectAsState()
    val selectedMode by vm.selectedMode.collectAsState()
    var placeName by remember { mutableStateOf("") }

    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Map", style = MaterialTheme.typography.headlineMedium) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current location", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (hasPermission) locationText.ifBlank { "Fetching location…" }
                        else "Location permission not granted. Grant it in system settings to see your position.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (hasPermission) {
                        Button(onClick = { vm.refreshLocation(context) }) { Text("Refresh Location") }
                    }
                }
            }
        }

        nextDestination?.let { dest ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Next destination", style = MaterialTheme.typography.titleMedium)
                        Text(dest, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Distance/ETA appear once this destination has a saved map point — add it under Saved Places, or wait for an online map provider to be configured.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Travel mode", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TravelMode.values().forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { vm.setMode(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Live turn-by-turn navigation, traffic-aware routing, and satellite/street map tiles require an online map SDK and are not included in this offline build. Distances shown anywhere in the app are straight-line offline estimates, not real routes.",
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (nearbyPlaces.isNotEmpty()) {
            item { Text("Nearby (within 5 km of last fix)", style = MaterialTheme.typography.titleMedium) }
            items(nearbyPlaces) { place ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text(place.category, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Save current location", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = placeName, onValueChange = { placeName = it },
                    label = { Text("Place name") }, modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { vm.saveCurrentAsPlace(placeName, "custom"); placeName = "" },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Place") }
            }
        }

        item { Text("Saved places", style = MaterialTheme.typography.titleMedium) }
        items(savedPlaces) { place ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    Text(place.category, style = MaterialTheme.typography.bodyMedium)
                    if (place.note.isNotBlank()) Text(place.note, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
