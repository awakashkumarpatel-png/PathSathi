package com.pathsathi.app.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.data.db.TripEntity
import com.pathsathi.app.engine.ItinerarySerializer
import com.pathsathi.app.ui.theme.PsSurfaceAlt

@Composable
fun LiveTripScreen(
    tripId: Long,
    onOpenBudget: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    vm: TripsViewModel = viewModel()
) {
    val trips by vm.trips.collectAsState()
    val guidance by vm.guidance.collectAsState()
    val trip = trips.firstOrNull { it.id == tripId }

    LaunchedEffect(trip?.currentDayIndex, trip?.id) {
        trip?.let { vm.refreshGuidance(it) }
    }

    if (trip == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("Loading trip…", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val days = ItinerarySerializer.decode(trip.itineraryJson)
    val dayIdx = trip.currentDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
    val today = days.getOrNull(dayIdx)

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(trip.destination, style = MaterialTheme.typography.headlineMedium)
        Text("Status: ${trip.status}", style = MaterialTheme.typography.bodyMedium)

        Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sathi Robot guidance", style = MaterialTheme.typography.titleMedium)
                Text(guidance.ifBlank { "Tap refresh to get today's guidance." }, style = MaterialTheme.typography.bodyMedium)
            }
        }

        today?.let { d ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Day ${d.dayNumber} plan", style = MaterialTheme.typography.titleMedium)
                    Text("Route: ${d.travelSequence}", style = MaterialTheme.typography.bodyMedium)
                    Text("Transport: ${d.transportation}", style = MaterialTheme.typography.bodyMedium)
                    Text("Stay: ${d.stayInfo}", style = MaterialTheme.typography.bodyMedium)
                    Text("Estimated cost: ₹${d.estimatedCostInr}", style = MaterialTheme.typography.bodyMedium)
                    Text("Rest time: ${d.restTimeMinutes} min", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (trip.status == "PLANNED") {
                Button(onClick = { vm.startTrip(trip) }) { Text("Start Trip") }
            }
            if (trip.status == "ACTIVE") {
                OutlinedButton(onClick = { vm.advanceDay(trip) }) { Text("Next Day") }
                Button(onClick = { vm.completeTrip(trip) }) { Text("Complete Trip") }
            }
            OutlinedButton(onClick = { vm.refreshGuidance(trip) }) { Text("Refresh Guidance") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenBudget) { Text("Open Budget") }
            OutlinedButton(onClick = onOpenMemory) { Text("Add Memory") }
        }
    }
}
