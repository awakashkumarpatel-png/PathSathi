package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import com.pathsathi.app.data.local.AppLanguage
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.GpsAccuracyMode
import com.pathsathi.app.data.local.ThemeMode
import com.pathsathi.app.data.local.UnitSystem
import com.pathsathi.app.data.network.NetworkModeManager
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOfflineMapsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onCheckInClick: () -> Unit = {},
    onNearbyHelpClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOnlineMode by NetworkModeManager.isOnlineMode.collectAsState()

    val language by AppPreferences.language(context).collectAsState(initial = AppLanguage.ENGLISH)
    val units by AppPreferences.units(context).collectAsState(initial = UnitSystem.METRIC)
    val themeMode by AppPreferences.themeMode(context).collectAsState(initial = ThemeMode.SYSTEM)
    val notificationsEnabled by AppPreferences.notificationsEnabled(context).collectAsState(initial = true)
    val gpsMode by AppPreferences.gpsMode(context).collectAsState(initial = GpsAccuracyMode.HIGH)
    val batterySaver by AppPreferences.batterySaver(context).collectAsState(initial = false)
    val sosCountdown by AppPreferences.sosCountdownSeconds(context).collectAsState(initial = 5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsCard(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.settings_profile_title),
                    subtitle = stringResource(R.string.settings_profile_subtitle),
                    onClick = onProfileClick
                )
            }

            item { SectionLabel(stringResource(R.string.section_app)) }
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledRow(stringResource(R.string.label_language)) {
                            SegmentedChoice(
                                options = listOf("English" to AppLanguage.ENGLISH, "हिन्दी" to AppLanguage.HINDI),
                                selected = language,
                                onSelect = { scope.launch { AppPreferences.setLanguage(context, it) } }
                            )
                        }
                        LabeledRow(stringResource(R.string.label_units)) {
                            SegmentedChoice(
                                options = listOf(stringResource(R.string.units_metric) to UnitSystem.METRIC, stringResource(R.string.units_imperial) to UnitSystem.IMPERIAL),
                                selected = units,
                                onSelect = { scope.launch { AppPreferences.setUnits(context, it) } }
                            )
                        }
                        LabeledRow(stringResource(R.string.label_theme)) {
                            SegmentedChoice(
                                options = listOf(stringResource(R.string.theme_light) to ThemeMode.LIGHT, stringResource(R.string.theme_dark) to ThemeMode.DARK, stringResource(R.string.theme_system) to ThemeMode.SYSTEM),
                                selected = themeMode,
                                onSelect = { scope.launch { AppPreferences.setThemeMode(context, it) } }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.label_notifications), fontWeight = FontWeight.Medium)
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { scope.launch { AppPreferences.setNotificationsEnabled(context, it) } },
                                colors = SwitchDefaults.colors(checkedTrackColor = TealPrimary)
                            )
                        }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.section_network)) }
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                if (isOnlineMode) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = TealPrimary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(if (isOnlineMode) stringResource(R.string.network_online_title) else stringResource(R.string.network_offline_title), fontWeight = FontWeight.Medium)
                                Text(
                                    if (isOnlineMode) stringResource(R.string.network_online_desc)
                                    else stringResource(R.string.network_offline_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isOnlineMode,
                            onCheckedChange = { NetworkModeManager.setOnlineMode(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = TealPrimary)
                        )
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.section_gps_battery)) }
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LabeledRow(stringResource(R.string.label_gps_accuracy)) {
                            SegmentedChoice(
                                options = listOf(
                                    stringResource(R.string.gps_high) to GpsAccuracyMode.HIGH,
                                    stringResource(R.string.gps_balanced) to GpsAccuracyMode.BALANCED,
                                    stringResource(R.string.gps_battery_saver) to GpsAccuracyMode.BATTERY_SAVER
                                ),
                                selected = gpsMode,
                                onSelect = { scope.launch { AppPreferences.setGpsMode(context, it) } }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_battery_saver), fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(R.string.battery_saver_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = batterySaver,
                                onCheckedChange = { scope.launch { AppPreferences.setBatterySaver(context, it) } },
                                colors = SwitchDefaults.colors(checkedTrackColor = TealPrimary)
                            )
                        }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.section_safety)) }
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LabeledRow(stringResource(R.string.label_sos_countdown)) {
                            SegmentedChoice(
                                options = listOf("3s" to 3, "5s" to 5, "10s" to 10),
                                selected = sosCountdown,
                                onSelect = { scope.launch { AppPreferences.setSosCountdownSeconds(context, it) } }
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard(
                    icon = Icons.Default.HealthAndSafety,
                    title = stringResource(R.string.settings_checkin_title),
                    subtitle = stringResource(R.string.settings_checkin_subtitle),
                    onClick = onCheckInClick
                )
            }
            item {
                SettingsCard(
                    icon = Icons.Default.LocalHospital,
                    title = stringResource(R.string.settings_nearby_title),
                    subtitle = stringResource(R.string.settings_nearby_subtitle),
                    onClick = onNearbyHelpClick
                )
            }

            item { SectionLabel(stringResource(R.string.section_ai_assistant)) }
            item {
                val aiSettings by AppPreferences.onlineAiSettings(context).collectAsState(
                    initial = AppPreferences.OnlineAiSettings()
                )
                var apiKeyInput by remember(aiSettings.apiKey) { mutableStateOf(aiSettings.apiKey) }
                var modelInput by remember(aiSettings.model) { mutableStateOf(aiSettings.model) }
                var showKey by remember { mutableStateOf(false) }

                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.ai_online_toggle), fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(R.string.ai_online_toggle_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = aiSettings.enabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        AppPreferences.saveOnlineAiSettings(context, aiSettings.copy(enabled = enabled))
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = TealPrimary)
                            )
                        }

                        if (aiSettings.enabled) {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text(stringResource(R.string.ai_api_key_label)) },
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showKey = !showKey }) {
                                        Icon(
                                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = modelInput,
                                onValueChange = { modelInput = it },
                                label = { Text(stringResource(R.string.ai_model_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        AppPreferences.saveOnlineAiSettings(
                                            context,
                                            aiSettings.copy(apiKey = apiKeyInput.trim(), model = modelInput.trim())
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                            Text(
                                stringResource(R.string.ai_key_privacy_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.section_maps)) }
            item {
                SettingsCard(
                    icon = Icons.Default.Map,
                    title = stringResource(R.string.settings_offline_maps_title),
                    subtitle = stringResource(R.string.settings_offline_maps_subtitle),
                    onClick = onOfflineMapsClick
                )
            }

            item { SectionLabel(stringResource(R.string.section_about)) }
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Path Sathi", fontWeight = FontWeight.Bold)
                        Text(
                            "Your Automatic Travel Companion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.app_version), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { SectionLabel(stringResource(R.string.settings_legal_section)) }
            item {
                SettingsCard(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.legal_terms_link),
                    subtitle = "",
                    onClick = onTermsClick
                )
            }
            item {
                SettingsCard(
                    icon = Icons.Default.PrivacyTip,
                    title = stringResource(R.string.legal_privacy_link),
                    subtitle = "",
                    onClick = onPrivacyClick
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun <T> SegmentedChoice(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = TealPrimary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Medium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}
