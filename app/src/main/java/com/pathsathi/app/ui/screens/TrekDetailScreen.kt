package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.FavoriteEntity
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.model.WeatherInfo
import com.pathsathi.app.data.network.WeatherRepository
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.ui.components.WeatherCard
import kotlinx.coroutines.launch

@Composable
fun TrekDetailScreen(
    trekId: String,
    onBack: () -> Unit,
    onStartTracking: (String) -> Unit,
    onViewMap: (String) -> Unit
) {
    val trek = remember { TrekRepository.getById(trekId) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favoriteDao = remember { PathSathiDatabase.getInstance(context).favoriteDao() }
    val favorites by favoriteDao.getAll().collectAsState(initial = emptyList())
    val isFavorite = trekId in favorites

    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Automatically fetch weather at the trek's location - no user action needed
    LaunchedEffect(trekId) {
        if (trek == null) return@LaunchedEffect
        val result = WeatherRepository().getCurrentWeather(trek.latitude, trek.longitude)
        isLoading = false
        result.onSuccess { weather = it }
        result.onFailure { error = it.message }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trek?.name ?: "Trek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            if (isFavorite) favoriteDao.remove(trekId) else favoriteDao.add(FavoriteEntity(trekId))
                        }
                    }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (trek == null) {
            Box(Modifier.padding(padding).fillMaxSize()) { Text(stringResource(R.string.trek_not_found)) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Trail weather", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            WeatherCard(weather = weather, isLoading = isLoading, error = error)

            Text(trek.location, style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatItem("Distance", "${trek.distanceKm} km")
                StatItem("Elevation", "${trek.elevationM} m")
                StatItem("Duration", "${trek.durationDays} days")
            }

            Text("Difficulty: ${trek.difficulty.label}", fontWeight = FontWeight.Bold)

            Divider()

            Text("About this trek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(trek.description, style = MaterialTheme.typography.bodyMedium)

            if (trek.bestSeason.isNotBlank()) {
                DetailRow("Best season", trek.bestSeason)
            }
            if (trek.permitsRequired.isNotBlank()) {
                DetailRow("Permits", trek.permitsRequired)
            }
            if (trek.safetyWarnings.isNotBlank()) {
                DetailRow("Safety", trek.safetyWarnings)
            }
            if (trek.equipmentList.isNotEmpty()) {
                Text("Equipment to carry", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trek.equipmentList) { item ->
                        AssistChip(onClick = {}, label = { Text(item) })
                    }
                }
            }

            OutlinedButton(onClick = { onViewMap(trek.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.trek_view_route_map))
            }

            Button(onClick = { onStartTracking(trek.name) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.trek_start_tracking))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
