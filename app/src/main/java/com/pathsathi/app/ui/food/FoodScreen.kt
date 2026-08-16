package com.pathsathi.app.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pathsathi.app.ads.AdSlot
import com.pathsathi.app.ads.AdSurface
import com.pathsathi.app.ads.rememberAdsOrchestrator
import com.pathsathi.app.data.repository.DemoDataProvider
import com.pathsathi.app.ui.common.SourceBadge

@Composable
fun FoodScreen() {
    var destination by remember { mutableStateOf("Your Destination") }
    val options = remember(destination) { DemoDataProvider.foodOptions(destination) }
    val adsOrchestrator = rememberAdsOrchestrator()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Food", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = destination, onValueChange = { destination = it },
            label = { Text("Destination") }, modifier = Modifier.fillMaxWidth()
        )
        AdSlot(surface = AdSurface.FOOD, orchestrator = adsOrchestrator)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { f ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(f.name, style = MaterialTheme.typography.titleMedium)
                        Text("${f.cuisine} · avg ₹${f.avgCostInr}" + if (f.budgetFriendly) " · Budget friendly" else "", style = MaterialTheme.typography.bodyMedium)
                        Text("Popular: ${f.popularDishes.joinToString(", ")}", style = MaterialTheme.typography.labelSmall)
                        SourceBadge(f.source)
                    }
                }
            }
        }
    }
}
