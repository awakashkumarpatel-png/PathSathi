package com.pathsathi.app.ui.transport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pathsathi.app.ads.AdSlot
import com.pathsathi.app.ads.AdSurface
import com.pathsathi.app.ads.rememberAdsOrchestrator
import com.pathsathi.app.core.rememberOnlineGate
import com.pathsathi.app.data.repository.DemoDataProvider
import com.pathsathi.app.online.TransportUpdate
import com.pathsathi.app.online.TransportUpdateOrchestrator
import com.pathsathi.app.ui.common.SourceBadge
import com.pathsathi.app.ui.theme.PsSurfaceAlt

@Composable
fun TransportScreen() {
    var destination by remember { mutableStateOf("Your Destination") }
    val options = remember(destination) { DemoDataProvider.transportOptions(destination) }
    val adsOrchestrator = rememberAdsOrchestrator()

    val gate = rememberOnlineGate()
    val transportUpdateOrchestrator = remember(gate) {
        TransportUpdateOrchestrator(isOnline = { gate.isOnline }, onlineFeaturesEnabled = { gate.onlineFeaturesEnabled })
    }
    var update by remember { mutableStateOf<TransportUpdate?>(null) }

    LaunchedEffect(destination, gate) {
        update = transportUpdateOrchestrator.updatesFor(destination)
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Transport", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = destination, onValueChange = { destination = it },
            label = { Text("Destination") }, modifier = Modifier.fillMaxWidth()
        )

        // Live schedules/fares when online + enabled + a real transport-data provider is configured;
        // otherwise an honest "showing saved/sample info" note — never a fabricated live schedule.
        update?.let { u ->
            Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(if (u.isLive) "Live update" else "Offline note", style = MaterialTheme.typography.titleMedium)
                    Text(u.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        AdSlot(surface = AdSurface.TRANSPORT, orchestrator = adsOrchestrator)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { t ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t.mode.replaceFirstChar { it.uppercase() } + " · " + t.fromTo, style = MaterialTheme.typography.titleMedium)
                        Text("₹${t.estimatedCostInr} · ~${t.estimatedDurationMinutes} min", style = MaterialTheme.typography.bodyMedium)
                        Text(t.notes, style = MaterialTheme.typography.labelSmall)
                        SourceBadge(t.source)
                    }
                }
            }
        }
    }
}
