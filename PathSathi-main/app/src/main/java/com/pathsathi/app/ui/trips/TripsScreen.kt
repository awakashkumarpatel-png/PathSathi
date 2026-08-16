package com.pathsathi.app.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TripsScreen(onOpenTrip: (Long) -> Unit, vm: TripsViewModel = viewModel()) {
    val trips by vm.trips.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("My Trips", style = MaterialTheme.typography.headlineMedium)

        if (trips.isEmpty()) {
            Text("No trips yet. Plan one from Home.", style = MaterialTheme.typography.bodyMedium)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(trips) { trip ->
                var confirmDelete by remember(trip.id) { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth(), onClick = { onOpenTrip(trip.id) }) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(trip.destination, style = MaterialTheme.typography.titleMedium)
                            Text(trip.status, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("${trip.days} days · ₹${trip.budgetInr} · ${trip.travelers} traveler(s) · ${trip.tripType}", style = MaterialTheme.typography.bodyMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (trip.status == "PLANNED" || trip.status == "ACTIVE") {
                                TextButton(onClick = { vm.cancelTrip(trip) }) { Text("Cancel") }
                            }
                            TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                        }
                    }
                }
                if (confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        title = { Text("Delete trip?") },
                        text = { Text("This removes the trip and its linked offline data. This cannot be undone.") },
                        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.deleteTrip(trip) }) { Text("Delete") } },
                        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } }
                    )
                }
            }
        }
    }
}
