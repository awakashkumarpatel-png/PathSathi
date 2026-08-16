package com.pathsathi.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pathsathi.app.R
import com.pathsathi.app.ads.AdSlot
import com.pathsathi.app.ads.AdSurface
import com.pathsathi.app.ads.AdsOrchestrator
import com.pathsathi.app.core.AppConfig
import com.pathsathi.app.core.ConnectivityObserver
import com.pathsathi.app.ui.theme.PsGreen
import com.pathsathi.app.ui.theme.PsSurface
import com.pathsathi.app.ui.theme.PsTurquoise

@Composable
fun HomeScreen(
    onPlanTrip: (String) -> Unit,
    onOpenBudget: () -> Unit,
    onOpenSafety: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSathi: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var destination by remember { mutableStateOf("") }
    val context = LocalContext.current
    val isOnline by ConnectivityObserver.isOnline(context).collectAsState(initial = false)
    val adsEnabled by AppConfig.adsEnabled(context).collectAsState(initial = false)
    val adsOrchestrator = remember(isOnline, adsEnabled) {
        AdsOrchestrator(isOnline = { isOnline }, adsEnabledInSettings = { adsEnabled })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Official Path Sathi logo — used exactly as provided (app/src/main/res/drawable-nodpi/path_sathi_logo.png),
        // shown at full aspect ratio with ContentScale.Fit so no part of the artwork is cropped.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.path_sathi_logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(56.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = PsTurquoise)
            }
        }

        // Sathi Robot preview card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = PsSurface),
            onClick = onOpenSathi
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.SmartToy, contentDescription = null, tint = PsGreen)
                Text(stringResource(R.string.sathi_greeting), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Destination input + plan trip
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text(stringResource(R.string.destination_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onPlanTrip(destination) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.plan_my_trip))
        }

        // Quick access grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessCard(Icons.Filled.AccountBalanceWallet, stringResource(R.string.quick_budget), Modifier.weight(1f), onOpenBudget)
            QuickAccessCard(Icons.Filled.Shield, stringResource(R.string.quick_safety), Modifier.weight(1f), onOpenSafety)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessCard(Icons.Filled.Explore, stringResource(R.string.quick_explore), Modifier.weight(1f), onOpenExplore)
            QuickAccessCard(Icons.Filled.PhotoLibrary, stringResource(R.string.quick_memory), Modifier.weight(1f), onOpenMemory)
        }

        // Ads never appear here unless the person is online AND has ads enabled AND a real
        // provider is configured — see com.pathsathi.app.ads. Renders nothing otherwise.
        AdSlot(surface = AdSurface.HOME, orchestrator = adsOrchestrator)
    }
}

@Composable
private fun QuickAccessCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PsTurquoise)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
