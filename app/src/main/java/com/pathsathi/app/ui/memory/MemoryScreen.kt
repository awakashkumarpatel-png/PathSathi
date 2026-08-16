package com.pathsathi.app.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryScreen(activeTripId: Long?, vm: MemoryViewModel = viewModel()) {
    val entries by vm.entries.collectAsState()
    var place by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Travel Memory", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("Place") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                vm.addEntry(activeTripId ?: 0L, place, note)
                place = ""; note = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Memory") }

        Text("Diary", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { e ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(e.place, style = MaterialTheme.typography.titleMedium)
                        if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodyMedium)
                        Text(fmt.format(Date(e.dateEpochMs)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
