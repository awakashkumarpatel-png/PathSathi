package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.local.TrackingSessionEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun TrackingHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).trackingDao() }
    val scope = rememberCoroutineScope()
    val sessions by dao.getAllSessions().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tracking_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.tracking_history_empty))
                    Text(stringResource(R.string.tracking_history_empty_desc), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(session, onDelete = { scope.launch { dao.deleteSession(session) } })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: TrackingSessionEntity, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val durationMs = session.endTime - session.startTime

    ElevatedCard {
        Column(Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(session.trekName, fontWeight = FontWeight.Bold)
                    Text(dateFormat.format(Date(session.startTime)), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(String.format("%.2f km", session.distanceMeters / 1000.0), style = MaterialTheme.typography.bodySmall)
                Text(formatDuration(durationMs), style = MaterialTheme.typography.bodySmall)
                Text(String.format("Avg %.1f km/h", session.avgSpeedKmh), style = MaterialTheme.typography.bodySmall)
                Text(String.format("+%.0fm elev", session.elevationGainM), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
