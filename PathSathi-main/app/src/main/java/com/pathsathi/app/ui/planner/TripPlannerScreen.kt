package com.pathsathi.app.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.data.model.TripType

@Composable
fun TripPlannerScreen(initialDestination: String, onTripCreated: (Long) -> Unit, vm: TripPlannerViewModel = viewModel()) {
    var destination by remember { mutableStateOf(initialDestination) }
    var days by remember { mutableStateOf("3") }
    var budget by remember { mutableStateOf("6000") }
    var travelers by remember { mutableStateOf("1") }
    var startDelay by remember { mutableStateOf("24") }
    var selectedType by remember { mutableStateOf(TripType.ADVENTURE) }
    val preview by vm.preview.collectAsState()
    val error by vm.error.collectAsState()
    val createdId by vm.createdTripId.collectAsState()
    LaunchedEffect(createdId) { createdId?.let(onTripCreated) }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Trip Planner", style = MaterialTheme.typography.headlineMedium) }
        item { OutlinedTextField(destination, { destination = it }, label = { Text("Destination") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text("Number of days") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(budget, { budget = it.filter(Char::isDigit) }, label = { Text("Budget (₹)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(travelers, { travelers = it.filter(Char::isDigit) }, label = { Text("Travelers") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(startDelay, { startDelay = it.filter(Char::isDigit) }, label = { Text("Start reminder in hours") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
        item {
            Text("Trip type", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(TripType.values().toList()) { type -> FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        }
        if (error != null) item { Text(error!!, color = MaterialTheme.colorScheme.error) }
        item { Button(onClick = { vm.previewTrip(destination, days.toIntOrNull() ?: 0, budget.toIntOrNull() ?: 0, (travelers.toIntOrNull() ?: 1).coerceAtLeast(1), selectedType) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("PREVIEW MY TRIP") } }
        if (preview.isNotEmpty()) {
            item { Text("Complete trip preview", style = MaterialTheme.typography.headlineSmall) }
            items(preview) { day ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text("Day ${day.dayNumber}", style = MaterialTheme.typography.titleMedium); Text("Route: ${day.travelSequence}"); Text("Transport: ${day.transportation}"); Text("Food: ${day.foodStops.joinToString()}"); Text("Stay: ${day.stayInfo}"); Text("Estimated cost: ₹${day.estimatedCostInr}") } }
            }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { vm.clearPreview() }, modifier = Modifier.weight(1f)) { Text("Edit") }; Button(onClick = { vm.confirmTrip(destination, days.toIntOrNull() ?: 0, budget.toIntOrNull() ?: 0, (travelers.toIntOrNull() ?: 1).coerceAtLeast(1), selectedType, startDelay.toIntOrNull() ?: 24) }, modifier = Modifier.weight(1f)) { Text("CONFIRM TRIP") } } }
        }
    }
}
