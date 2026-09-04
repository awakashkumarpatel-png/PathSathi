package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.model.Trip
import com.pathsathi.app.data.local.toTrip
import com.pathsathi.app.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripViewScreen(
    tripId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).tripDao() }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(tripId) {
        trip = dao.getById(tripId)?.toTrip()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tripview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (trip != null) {
                        IconButton(onClick = { onEdit(tripId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            trip == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tripview_not_found))
            }
            else -> {
                val t = trip!!
                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

                LazyColumn(
                    modifier = Modifier.padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(t.tripName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(t.destination, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    item { DetailRow(Icons.Default.CalendarMonth, "Start", dateFormat.format(Date(t.startDateTime))) }
                    item { DetailRow(Icons.Default.Flag, "End", dateFormat.format(Date(t.endDateTime))) }
                    item { DetailRow(Icons.Default.Group, "Travel With", t.travelWith.ifBlank { "—" }) }

                    item {
                        DetailRow(
                            Icons.Default.Person,
                            "Members (${t.members.size})",
                            if (t.members.isEmpty()) "—" else t.members.joinToString(", ")
                        )
                    }
                    item {
                        DetailRow(
                            Icons.Default.PinDrop,
                            "Stops (${t.stops.size})",
                            if (t.stops.isEmpty()) "—" else t.stops.joinToString(", ")
                        )
                    }
                    item { DetailRow(Icons.Default.Hotel, "Stay", t.stay.ifBlank { "—" }) }
                    item {
                        DetailRow(
                            Icons.Default.CurrencyRupee,
                            "Budget",
                            t.budget?.let { "₹${String.format("%.0f", it)}" } ?: "—"
                        )
                    }
                    item { DetailRow(Icons.Default.Notes, "Notes", t.notes.ifBlank { "—" }) }

                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    ElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TealPrimary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
