package com.pathsathi.app.ui.sathi

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.R
import com.pathsathi.app.ui.theme.PsGreen
import com.pathsathi.app.ui.theme.PsSurface
import com.pathsathi.app.ui.theme.PsSurfaceAlt

@Composable
fun SathiScreen(vm: SathiViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    val isHindi by vm.isHindi.collectAsState()
    val onlineAiRequestedAndReachable by vm.onlineAiRequestedAndReachable.collectAsState()
    var input by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val listening by vm.isListening.collectAsState()
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) vm.startVoiceListening() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sathi Robot", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { vm.toggleLanguage() }) {
                    Icon(Icons.Filled.Language, contentDescription = "Toggle language", tint = PsGreen)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt)
            ) {
                Text(
                    stringResource(R.string.sathi_offline_notice),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
            if (onlineAiRequestedAndReachable) {
                Text(
                    "Online AI is turned on and you're online, but no online AI provider is configured yet — Sathi is still answering offline.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(colors = CardDefaults.cardColors(containerColor = if (msg.fromUser) PsGreen else PsSurface)) {
                            Text(
                                msg.text,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.sathi_input_hint)) }
                )
                IconButton(onClick = {
                    if (!vm.voiceEngine.isDeviceRecognitionAvailable()) return@IconButton
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.startVoiceListening() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Icon(Icons.Filled.Mic, contentDescription = if(listening) stringResource(R.string.sathi_listening) else "Voice input", tint = if(listening) PsGreen else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {
                    vm.sendMessage(input)
                    input = ""
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = PsGreen)
                }
            }
        }
    }
}
