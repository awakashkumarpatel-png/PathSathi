package com.pathsathi.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pathsathi.app.data.local.EmergencyContactEntity
import com.pathsathi.app.data.local.PathSathiDatabase
import kotlinx.coroutines.launch

@Composable
fun EmergencyContactsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).emergencyContactDao() }
    val scope = rememberCoroutineScope()

    val contacts by dao.getAll().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContactEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contacts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add contact")
            }
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.contacts_none))
                    Text(stringResource(R.string.contacts_none_desc), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts) { contact ->
                    ElevatedCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(contact.name, fontWeight = FontWeight.Bold)
                                    if (contact.isPrimary) {
                                        Spacer(Modifier.width(6.dp))
                                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.contacts_primary)) })
                                    }
                                }
                                Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall)
                            }
                            Row {
                                IconButton(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")))
                                }) {
                                    Icon(Icons.Default.Call, contentDescription = "Call")
                                }
                                IconButton(onClick = { editingContact = contact }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    scope.launch { dao.delete(contact) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                        if (!contact.isPrimary) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        dao.clearPrimary()
                                        dao.setPrimary(contact.id)
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                            ) {
                                Text(stringResource(R.string.contacts_set_primary))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingContact != null) {
        ContactEditDialog(
            existing = editingContact,
            onDismiss = { showAddDialog = false; editingContact = null },
            onSave = { name, phone ->
                scope.launch {
                    val existing = editingContact
                    if (existing != null) {
                        dao.update(existing.copy(name = name, phoneNumber = phone))
                    } else {
                        dao.insert(EmergencyContactEntity(name = name, phoneNumber = phone))
                    }
                }
                showAddDialog = false
                editingContact = null
            }
        )
    }
}

@Composable
private fun ContactEditDialog(
    existing: EmergencyContactEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var phone by remember { mutableStateOf(existing?.phoneNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) stringResource(R.string.contacts_add_title) else stringResource(R.string.contacts_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.contacts_name_label)) })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.contacts_phone_label)) })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && phone.isNotBlank()) onSave(name, phone) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
