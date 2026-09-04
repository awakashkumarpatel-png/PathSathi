package com.pathsathi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import com.pathsathi.app.data.model.Trek
import com.pathsathi.app.data.model.WeatherInfo
import com.pathsathi.app.data.network.NetworkModeManager
import com.pathsathi.app.data.network.WeatherRepository
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.location.LocationHelper
import com.pathsathi.app.ui.components.WeatherCard
import com.pathsathi.app.ui.theme.BlueAccent
import com.pathsathi.app.ui.theme.GreenAccent
import com.pathsathi.app.ui.theme.OrangeAccent
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch

private data class QuickAccessItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onTrekClick: (String) -> Unit,
    onCompassClick: () -> Unit,
    onSosClick: () -> Unit,
    onJournalClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onPlanTripClick: () -> Unit,
    onEmergencyContactsClick: () -> Unit = {},
    onMyTripsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAssistantClick: () -> Unit = {},
    onNearbyHelpClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    fun fetchWeatherAutomatically() {
        if (!NetworkModeManager.isOnlineMode.value) {
            isLoading = false
            error = context.getString(R.string.home_offline_weather_error)
            weather = null
            return
        }
        scope.launch {
            isLoading = true
            error = null
            val location = LocationHelper(context).getCurrentLocation()
            if (location == null) {
                isLoading = false
                error = context.getString(R.string.home_location_error)
                return@launch
            }
            val result = WeatherRepository().getCurrentWeather(location.first, location.second)
            isLoading = false
            result.onSuccess { weather = it }
            result.onFailure { error = it.message }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) fetchWeatherAutomatically() else isLoading = false
    }

    val isOnlineMode by NetworkModeManager.isOnlineMode.collectAsState()

    LaunchedEffect(hasLocationPermission, isOnlineMode) {
        if (hasLocationPermission) {
            fetchWeatherAutomatically()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val filteredTreks = remember(searchQuery) { TrekRepository.search(searchQuery) }

    // 3x3 Quick Access grid — mapped onto the app's real, working screens
    val quickAccessItems = listOf(
        QuickAccessItem(stringResource(R.string.qa_trip_planner), Icons.Default.Map, TealPrimary, onPlanTripClick),
        QuickAccessItem(stringResource(R.string.qa_my_trips), Icons.Default.CardTravel, GreenAccent, onMyTripsClick),
        QuickAccessItem(stringResource(R.string.qa_navigation), Icons.Default.Explore, BlueAccent, onCompassClick),
        QuickAccessItem(stringResource(R.string.qa_tracking), Icons.Default.MyLocation, OrangeAccent, onHistoryClick),
        QuickAccessItem(stringResource(R.string.qa_sos), Icons.Default.Emergency, Color(0xFFE0463D), onSosClick),
        QuickAccessItem(stringResource(R.string.qa_journal), Icons.Default.Book, TealPrimary, onJournalClick),
        QuickAccessItem(stringResource(R.string.qa_favorites), Icons.Default.Favorite, Color(0xFFE0463D), onFavoritesClick),
        QuickAccessItem(stringResource(R.string.qa_contacts), Icons.Default.ContactPhone, BlueAccent, onEmergencyContactsClick),
        QuickAccessItem(stringResource(R.string.qa_nearby_help), Icons.Default.LocalHospital, Color(0xFFE0463D), onNearbyHelpClick),
        QuickAccessItem(stringResource(R.string.qa_history), Icons.Default.History, GreenAccent, onHistoryClick)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAssistantClick,
                containerColor = TealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant")
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_greeting), fontWeight = FontWeight.Bold)
                        Text(
                            "Ready for your next adventure?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // "Let's Explore the World" banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(TealPrimary, GreenAccent)))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Let's Explore\nthe World",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onPlanTripClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = TealPrimary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(stringResource(R.string.home_plan_new_trip))
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            // Quick Access — 3x3 grid
            item {
                Column {
                    Text(stringResource(R.string.home_quick_access), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    quickAccessItems.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                QuickAccessTile(item = item, modifier = Modifier.weight(1f))
                            }
                            // Pad the last row if it has fewer than 3 items
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            item {
                WeatherCard(weather = weather, isLoading = isLoading, error = error)
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            item {
                Text(
                    "Explore Treks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredTreks.isEmpty()) {
                item {
                    Text(
                        "No treks match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(filteredTreks) { trek ->
                    TrekListItem(trek = trek, onClick = { onTrekClick(trek.id) })
                }
            }

            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun QuickAccessTile(item: QuickAccessItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = item.onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(item.tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = item.label, tint = item.tint)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun TrekListItem(trek: Trek, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(trek.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(trek.location, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${trek.distanceKm} km · ${trek.elevationM} m · ${trek.difficulty.label}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
