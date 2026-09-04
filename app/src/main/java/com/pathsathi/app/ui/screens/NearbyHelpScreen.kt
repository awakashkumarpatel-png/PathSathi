package com.pathsathi.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import com.pathsathi.app.data.network.NearbyHelpRepository
import com.pathsathi.app.data.network.NearbyPlace
import com.pathsathi.app.location.LocationHelper
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch

private val categoryFilters = listOf("all", "hospital", "police", "pharmacy", "fire_station", "rescue")

@Composable
fun NearbyHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var places by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf("all") }

    fun loadNearby() {
        scope.launch {
            isLoading = true
            error = null
            val location = LocationHelper(context).getCurrentLocation()
            if (location == null) {
                isLoading = false
                error = context.getString(R.string.nearby_load_error)
                return@launch
            }
            val result = NearbyHelpRepository().findNearby(location.first, location.second)
            isLoading = false
            result.onSuccess { places = it }
            result.onFailure { error = it.message ?: context.getString(R.string.nearby_load_error_generic) }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) loadNearby()
    }

    val filtered = remember(places, selectedFilter) {
        if (selectedFilter == "all") places else places.filter { it.category == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nearby_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { if (hasLocationPermission) loadNearby() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categoryFilters.forEach { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { selectedFilter = cat },
                        label = { Text(categoryLabel(cat)) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            when {
                !hasLocationPermission -> {
                    Text(
                        stringResource(R.string.nearby_location_needed),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Text(
                        stringResource(R.string.nearby_couldnt_load, error ?: ""),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { loadNearby() }) { Text(stringResource(R.string.action_retry)) }
                }
                filtered.isEmpty() -> {
                    Text(stringResource(R.string.nearby_none_found), style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filtered) { place ->
                            NearbyPlaceCard(place)
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryLabel(cat: String): String = when (cat) {
    "all" -> stringResource(R.string.nearby_cat_all)
    "hospital" -> stringResource(R.string.nearby_cat_hospital)
    "police" -> stringResource(R.string.nearby_cat_police)
    "pharmacy" -> stringResource(R.string.nearby_cat_pharmacy)
    "fire_station" -> stringResource(R.string.nearby_cat_fire)
    "rescue" -> stringResource(R.string.nearby_cat_rescue)
    else -> cat
}

private fun categoryIcon(cat: String): ImageVector = when (cat) {
    "hospital" -> Icons.Default.LocalHospital
    "police" -> Icons.Default.LocalPolice
    "pharmacy" -> Icons.Default.LocalPharmacy
    "fire_station" -> Icons.Default.LocalFireDepartment
    "rescue" -> Icons.Default.Hiking
    else -> Icons.Default.Place
}

private fun categoryColor(cat: String): Color = when (cat) {
    "hospital" -> Color(0xFFE0463D)
    "police" -> Color(0xFF4E8FF7)
    "pharmacy" -> TealPrimary
    "fire_station" -> Color(0xFFFF9F45)
    "rescue" -> Color(0xFF8BD44C)
    else -> Color.Gray
}

@Composable
private fun NearbyPlaceCard(place: NearbyPlace) {
    val context = LocalContext.current
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor(place.category).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(place.category), contentDescription = null, tint = categoryColor(place.category))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(place.name, fontWeight = FontWeight.Bold)
                Text(
                    "${categoryLabel(place.category)} · ${formatDistance(place.distanceMeters)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = {
                val uri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}(${Uri.encode(place.name)})")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }) {
                Icon(Icons.Default.Directions, contentDescription = "Navigate", tint = TealPrimary)
            }
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else String.format("%.1f km", meters / 1000.0)
