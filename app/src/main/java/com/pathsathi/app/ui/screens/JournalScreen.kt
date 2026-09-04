package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.JournalEntryEntity
import com.pathsathi.app.data.local.PathSathiDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Journal entries now persist in Room (pathsathi.db) instead of in-memory
 * state, so they survive app restarts. Existing UI shape (note field + Add
 * button + list) is preserved.
 */
@Composable
fun JournalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).journalDao() }
    val scope = rememberCoroutineScope()

    val entries by dao.getAll().collectAsState(initial = emptyList())
    var noteText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.journal_placeholder)) }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (noteText.isNotBlank()) {
                        scope.launch {
                            dao.insert(
                                JournalEntryEntity(
                                    trekName = "Untitled",
                                    title = noteText.take(30),
                                    note = noteText,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        noteText = ""
                    }
                }) {
                    Text(stringResource(R.string.action_add))
                }
            }

            Spacer(Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text(stringResource(R.string.journal_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries) { entry ->
                        ElevatedCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.note, fontWeight = FontWeight.Bold)
                                    Text(dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { scope.launch { dao.delete(entry) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
