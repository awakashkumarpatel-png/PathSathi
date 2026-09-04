package com.pathsathi.app.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.CheckInEntity
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.service.CheckInScheduler
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val intervalOptions = listOf(30, 60, 120, 180)
private val graceOptions = listOf(10, 15, 30)

@Composable
fun CheckInSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { PathSathiDatabase.getInstance(context).checkInDao() }
    val log by dao.getAll().collectAsState(initial = emptyList())

    val settings by AppPreferences.checkInSettings(context).collectAsState(
        initial = AppPreferences.CheckInSettings()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkin_title)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.checkin_enable), fontWeight = FontWeight.Medium)
                                Text(
                                    stringResource(R.string.checkin_enable_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.enabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        AppPreferences.saveCheckInSettings(context, settings.copy(enabled = enabled))
                                        if (enabled) {
                                            CheckInScheduler.scheduleNextTrigger(context, settings.intervalMinutes)
                                        } else {
                                            CheckInScheduler.cancelAll(context)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = TealPrimary)
                            )
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.checkin_interval), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervalOptions.forEach { minutes ->
                        FilterChip(
                            selected = settings.intervalMinutes == minutes,
                            onClick = {
                                scope.launch {
                                    val updated = settings.copy(intervalMinutes = minutes)
                                    AppPreferences.saveCheckInSettings(context, updated)
                                    if (updated.enabled) CheckInScheduler.scheduleNextTrigger(context, minutes)
                                }
                            },
                            label = { Text(if (minutes < 60) "${minutes}m" else "${minutes / 60}h") }
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.checkin_grace), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    graceOptions.forEach { minutes ->
                        FilterChip(
                            selected = settings.graceMinutes == minutes,
                            onClick = {
                                scope.launch {
                                    AppPreferences.saveCheckInSettings(context, settings.copy(graceMinutes = minutes))
                                }
                            },
                            label = { Text("${minutes}m") }
                        )
                    }
                }
            }

            item {
                Text(stringResource(R.string.checkin_log), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (log.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.checkin_none_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(log) { entry -> CheckInLogRow(entry) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CheckInLogRow(entry: CheckInEntity) {
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val confirmedLabel = stringResource(R.string.checkin_confirmed)
    val missedLabel = stringResource(R.string.checkin_missed)
    val pendingLabel = stringResource(R.string.checkin_pending)
    val (icon, tint, label) = when (entry.status) {
        "confirmed" -> Triple(Icons.Default.CheckCircle, TealPrimary, confirmedLabel)
        "missed" -> Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, missedLabel)
        else -> Triple(Icons.Default.Schedule, MaterialTheme.colorScheme.onSurfaceVariant, pendingLabel)
    }
    ElevatedCard(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontWeight = FontWeight.Medium)
                Text(fmt.format(Date(entry.scheduledAt)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
