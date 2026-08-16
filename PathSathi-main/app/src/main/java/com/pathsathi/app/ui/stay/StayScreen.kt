package com.pathsathi.app.ui.stay

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pathsathi.app.ads.AdSlot
import com.pathsathi.app.ads.AdSurface
import com.pathsathi.app.ads.rememberAdsOrchestrator
import com.pathsathi.app.core.rememberOnlineGate
import com.pathsathi.app.data.repository.DemoDataProvider
import com.pathsathi.app.online.BookingAvailability
import com.pathsathi.app.online.BookingOrchestrator
import com.pathsathi.app.ui.common.SourceBadge
import kotlinx.coroutines.launch

@Composable
fun StayScreen() {
    var destination by remember { mutableStateOf("Your Destination") }
    val options = remember(destination) { DemoDataProvider.stayOptions(destination) }
    val adsOrchestrator = rememberAdsOrchestrator()

    val gate = rememberOnlineGate()
    val bookingOrchestrator = remember(gate) {
        BookingOrchestrator(isOnline = { gate.isOnline }, onlineFeaturesEnabled = { gate.onlineFeaturesEnabled })
    }
    val scope = rememberCoroutineScope()
    var availabilityById by remember { mutableStateOf<Map<String, BookingAvailability>>(emptyMap()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Stay", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = destination, onValueChange = { destination = it },
            label = { Text("Destination") }, modifier = Modifier.fillMaxWidth()
        )
        AdSlot(surface = AdSurface.STAY, orchestrator = adsOrchestrator)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium)
                        Text("${s.type.replaceFirstChar { it.uppercase() }} · ₹${s.pricePerNightInr}/night · ${s.distanceFromCenterKm} km from center", style = MaterialTheme.typography.bodyMedium)
                        Text(s.notes, style = MaterialTheme.typography.labelSmall)
                        SourceBadge(s.source)

                        // Online booking: honest "not configured yet" fallback until a real booking
                        // partner + API key is wired into com.pathsathi.app.online.OnlineBookingProvider.
                        Button(onClick = {
                            scope.launch {
                                val result = bookingOrchestrator.checkAvailability(s.id)
                                availabilityById = availabilityById + (s.id to result)
                            }
                        }) { Text("Check availability") }
                        availabilityById[s.id]?.let { avail ->
                            Text(avail.message, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
