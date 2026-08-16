package com.pathsathi.app.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.pathsathi.app.online.TouristInfoOrchestrator
import com.pathsathi.app.online.TouristInfoUpdate
import com.pathsathi.app.online.WeatherInfo
import com.pathsathi.app.online.WeatherOrchestrator
import com.pathsathi.app.ui.common.SourceBadge
import com.pathsathi.app.ui.theme.PsSurfaceAlt

@Composable
fun ExploreScreen(
    onOpenStay: () -> Unit = {},
    onOpenFood: () -> Unit = {},
    onOpenTransport: () -> Unit = {},
) {
    var destination by remember { mutableStateOf("Your Destination") }
    val places = remember(destination) { DemoDataProvider.explorePlaces(destination) }

    val gate = rememberOnlineGate()
    val adsOrchestrator = rememberAdsOrchestrator()

    val weatherOrchestrator = remember(gate) {
        WeatherOrchestrator(isOnline = { gate.isOnline }, onlineFeaturesEnabled = { gate.onlineFeaturesEnabled })
    }
    val touristInfoOrchestrator = remember(gate) {
        TouristInfoOrchestrator(isOnline = { gate.isOnline }, onlineFeaturesEnabled = { gate.onlineFeaturesEnabled })
    }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var touristInfo by remember { mutableStateOf<TouristInfoUpdate?>(null) }

    LaunchedEffect(destination, gate) {
        weather = weatherOrchestrator.current(destination)
        touristInfo = touristInfoOrchestrator.updatesFor(destination)
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Explore", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = destination, onValueChange = { destination = it },
            label = { Text("Destination") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenStay) { Text("Stay") }
            OutlinedButton(onClick = onOpenFood) { Text("Food") }
            OutlinedButton(onClick = onOpenTransport) { Text("Transport") }
        }

        // Weather: real offline fallback message when not online/enabled/configured — never a fabricated forecast.
        weather?.let { w ->
            Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Weather", style = MaterialTheme.typography.titleMedium)
                    Text(w.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Updated tourist info: same honest offline/online split as weather.
        touristInfo?.let { info ->
            Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Latest info", style = MaterialTheme.typography.titleMedium)
                    Text(info.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Ads never appear here unless online + enabled + a real provider is configured — renders nothing otherwise.
        AdSlot(surface = AdSurface.EXPLORE, orchestrator = adsOrchestrator)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(places) { place ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(place.description, style = MaterialTheme.typography.bodyMedium)
                        Text("${place.distanceKm} km · ~${place.suggestedDurationHours}h · ₹${place.estimatedCostInr}", style = MaterialTheme.typography.labelSmall)
                        Text(place.usefulInfo, style = MaterialTheme.typography.labelSmall)
                        SourceBadge(place.source)
                    }
                }
            }
        }
    }
}
