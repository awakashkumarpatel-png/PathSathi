package com.pathsathi.app.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.R
import com.pathsathi.app.data.model.TripType

@Composable
fun TripPlannerScreen(
    initialDestination: String,
    onTripCreated: (Long) -> Unit,
    vm: TripPlannerViewModel = viewModel()
) {
    var destination by remember { mutableStateOf(initialDestination) }
    var days by remember { mutableStateOf("3") }
    var budget by remember { mutableStateOf("6000") }
    var travelers by remember { mutableStateOf("1") }
    var selectedType by remember { mutableStateOf(TripType.ADVENTURE) }

    val createdTripId by vm.createdTripId.collectAsState()
    LaunchedEffect(createdTripId) {
        createdTripId?.let { onTripCreated(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Trip Planner", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = destination, onValueChange = { destination = it },
            label = { Text(stringResource(R.string.trip_destination)) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = days, onValueChange = { days = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.trip_days)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = budget, onValueChange = { budget = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.trip_budget) + " (₹)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = travelers, onValueChange = { travelers = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.trip_travelers)) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text(stringResource(R.string.trip_type), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TripType.values().toList()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Button(
            onClick = {
                vm.planTrip(
                    destination = destination,
                    days = days.toIntOrNull() ?: 0,
                    budgetInr = budget.toIntOrNull() ?: 0,
                    travelers = (travelers.toIntOrNull() ?: 1).coerceAtLeast(1),
                    tripType = selectedType
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(stringResource(R.string.plan_my_trip))
        }
    }
}
