package com.pathsathi.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pathsathi.app.core.AppConfig
import com.pathsathi.app.core.LanguageManager
import com.pathsathi.app.core.findActivity
import com.pathsathi.app.core.ConnectivityObserver
import com.pathsathi.app.online.CloudSyncOrchestrator
import com.pathsathi.app.online.SyncResult
import kotlinx.coroutines.launch

/**
 * Language selection here is app-UI-language (via string resources / Locale).
 * Robot voice language is set independently inside the Sathi screen, per spec
 * section 18 (robot voice language and app language configurable independently).
 *
 * The Online/Ads/Web section below controls the optional layers added on top
 * of the offline-first core (see com.pathsathi.app.core.AppConfig,
 * com.pathsathi.app.online.OnlineServices, com.pathsathi.app.ads). Every
 * toggle here defaults to off; turning it on only takes effect once a real
 * provider is configured behind the matching Online*Provider interface —
 * flipping these switches today does not connect to any live service.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedCode = LanguageManager.getLanguage(context)
    val languages = listOf("English" to "en", "Hindi" to "hi")

    val onlineFeaturesEnabled by AppConfig.onlineFeaturesEnabled(context).collectAsState(initial = false)
    val onlineAiEnabled by AppConfig.onlineAiEnabled(context).collectAsState(initial = false)
    val adsEnabled by AppConfig.adsEnabled(context).collectAsState(initial = false)
    val websiteUrl by AppConfig.websiteUrl(context).collectAsState(initial = AppConfig.DEFAULT_WEBSITE_URL)
    var websiteUrlDraft by remember(websiteUrl) { mutableStateOf(websiteUrl) }

    val isOnline by ConnectivityObserver.isOnline(context).collectAsState(initial = false)
    val cloudSyncOrchestrator = remember(isOnline, onlineFeaturesEnabled) {
        CloudSyncOrchestrator(isOnline = { isOnline }, onlineFeaturesEnabled = { onlineFeaturesEnabled })
    }
    var syncResult by remember { mutableStateOf<SyncResult?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App language", style = MaterialTheme.typography.titleMedium)
                languages.forEach { (label, code) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedCode == code, onClick = { LanguageManager.setLanguage(context, code); context.findActivity()?.recreate() })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Online & Offline", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Path Sathi works fully offline: saved trips, itineraries, budget, saved places, emergency info, and Sathi Robot's basic replies all keep working without internet. Turning this on only enables optional extras when they're online — it never replaces offline functionality.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    SettingsSwitchRow(
                        label = "Enable online features",
                        checked = onlineFeaturesEnabled,
                        onCheckedChange = { enabled -> scope.launch { AppConfig.setOnlineFeaturesEnabled(context, enabled) } }
                    )
                    SettingsSwitchRow(
                        label = "Online AI (optional, advanced)",
                        checked = onlineAiEnabled,
                        onCheckedChange = { enabled -> scope.launch { AppConfig.setOnlineAiEnabled(context, enabled) } }
                    )
                    Text(
                        "No online AI provider is configured yet, so Sathi Robot keeps using its offline replies either way. This switch only takes effect once a real provider is added.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "Cloud sync (backup of trips/budget/memory across devices) also lives behind this switch. No sync backend is configured yet, so your data always stays saved locally on this device regardless.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedButton(onClick = {
                        scope.launch { syncResult = cloudSyncOrchestrator.syncNow() }
                    }) { Text("Sync now") }
                    syncResult?.let { result ->
                        Text(result.message, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ads", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ads only ever load when you're online and this is turned on, and never appear on Safety, Sathi, Map, or Live Trip screens. No ad provider is configured yet, so this currently shows nothing either way.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    SettingsSwitchRow(
                        label = "Allow ads when online",
                        checked = adsEnabled,
                        onCheckedChange = { enabled -> scope.launch { AppConfig.setAdsEnabled(context, enabled) } }
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Web presence", style = MaterialTheme.typography.titleMedium)
                    val configured = AppConfig.isWebsiteConfigured(websiteUrl)
                    Text(
                        if (configured) "Official website configured." else "Using the configured Path Sathi web address.",
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedTextField(
                        value = websiteUrlDraft,
                        onValueChange = { websiteUrlDraft = it },
                        label = { Text("Website URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = { scope.launch { AppConfig.setWebsiteUrl(context, websiteUrlDraft) } }) {
                        Text("Save URL")
                    }
                    Text(
                        "Privacy policy and support links will use this same URL once it's configured — nothing else needs to change.",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("Path Sathi v1.0.0 — offline-first travel companion.", style = MaterialTheme.typography.bodyMedium)
                    Text("Additional regional and international languages can be added here without rewriting the app, by adding new values-<locale> resource sets.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
