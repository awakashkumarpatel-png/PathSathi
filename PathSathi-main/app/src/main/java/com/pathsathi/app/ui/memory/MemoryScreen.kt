package com.pathsathi.app.ui.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryScreen(activeTripId: Long?, vm: MemoryViewModel = viewModel()) {
    LaunchedEffect(activeTripId) { vm.loadTrip(activeTripId) }
    val entries by vm.entries.collectAsState()
    val trip by vm.trip.collectAsState()
    var place by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val id = trip?.id
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Travel Memory", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (id != null) "Saving memories for ${trip?.destination}."
            else "Create a trip before saving memories."
        )
        OutlinedTextField(place, { place = it }, label = { Text("Place") }, enabled = id != null, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Note") }, enabled = id != null, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { vm.addEntry(id, place, note); place = ""; note = "" },
            enabled = id != null && place.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Memory") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.place, style = MaterialTheme.typography.titleMedium)
                        if (e.note.isNotBlank()) Text(e.note)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fmt.format(Date(e.dateEpochMs)), style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { vm.deleteEntry(e) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
