package com.pathsathi.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val estimate by vm.nextDestinationEstimate.collectAsState()
    var placeName by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true || granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true) vm.refreshLocation(context)
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) { if (hasPermission) vm.refreshLocation(context) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Map & Location", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current location", style = MaterialTheme.typography.titleMedium)
                    Text(if (hasPermission) locationText.ifBlank { "Fetching location…" } else "Allow location to enable live trip distance and nearby saved places.")
                    Button(onClick = {
                        if (hasPermission) vm.refreshLocation(context)
                        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }) { Text(if (hasPermission) "Refresh Location" else "Allow Location") }
                }
            }
        }
        nextDestination?.let { dest -> item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Next destination", style = MaterialTheme.typography.titleMedium)
                Text(dest)
                if (estimate != null) Text("${"%.1f".format(estimate!!.distanceKm)} km · ~${estimate!!.etaMinutes} min (${estimate!!.mode.name.lowercase()})")
                else Text("Save a map point with the same name to calculate an offline distance and ETA.", style = MaterialTheme.typography.labelSmall)
            }}
        }}
        item { Text("Travel mode", style = MaterialTheme.typography.titleMedium) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TravelMode.values().forEach { mode -> FilterChip(selected = selectedMode == mode, onClick = { vm.setMode(mode) }, label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }) } } }
        if (nearbyPlaces.isNotEmpty()) { item { Text("Nearby saved places", style = MaterialTheme.typography.titleMedium) }; items(nearbyPlaces) { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(p.name, style = MaterialTheme.typography.titleMedium); Text(p.category) } } } }
        item {
            Text("Save current location", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(placeName, { placeName = it }, label = { Text("Place name") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.saveCurrentAsPlace(placeName, "custom"); placeName = "" }, enabled = hasPermission && placeName.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save Place") }
        }
        item { Text("Saved places", style = MaterialTheme.typography.titleMedium) }
        items(savedPlaces) { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(p.name, style = MaterialTheme.typography.titleMedium); Text(p.category); Text(if (p.lat != null) "GPS saved" else "Name only", style = MaterialTheme.typography.labelSmall) } } }
    }
}
