@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pathsathi.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pathsathi.app.R
import com.pathsathi.app.ai.*
import com.pathsathi.app.data.network.NetworkModeManager
import com.pathsathi.app.ui.components.NetworkImage
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(
    onBack: () -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOnlineMode by NetworkModeManager.isOnlineMode.collectAsState()
    val language by AssistantLanguageManager.language.collectAsState()

    val engine = remember { AssistantEngine(context, onNavigate = onNavigateRoute) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var lastFailedText by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var engineRef: TextToSpeech? = null
        engineRef = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        engineRef
    }
    DisposableEffect(Unit) {
        onDispose { tts.stop(); tts.shutdown() }
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            // Permission just granted from the prompt - let the user tap the mic again
            // rather than auto-launching recognition mid-composition.
        } else {
            messages.add(ChatMessage(fromUser = false, text = AssistantStrings.micPermissionNeeded(language)))
        }
    }

    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(ChatMessage(fromUser = false, text = AssistantStrings.greeting(language)))
        }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        tts.language = language.locale
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun send(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return
        messages.add(ChatMessage(fromUser = true, text = text))
        input = ""
        lastFailedText = null

        isThinking = true
        scope.launch {
            val replies = try {
                engine.handleUserMessage(text, language)
            } catch (e: Exception) {
                lastFailedText = text
                listOf(ChatMessage(fromUser = false, text = AssistantStrings.genericError(language), canRetry = true))
            }
            messages.addAll(replies)
            isThinking = false
            if (replies.any { it.canRetry }) lastFailedText = text
            replies.lastOrNull()?.let { if (it.text.isNotBlank()) speak(it.text) }
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val recognized = results?.firstOrNull()
        if (!recognized.isNullOrBlank()) send(recognized)
    }

    fun startVoiceInput() {
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.locale.toLanguageTag())
        }
        val launched = runCatching { voiceLauncher.launch(intent) }.isSuccess
        if (!launched) {
            messages.add(ChatMessage(fromUser = false, text = AssistantStrings.voiceUnavailable(language)))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assistant_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        messages.add(ChatMessage(fromUser = false, text = AssistantStrings.helpText(language)))
                        scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "What can you do?")
                    }
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = "Language")
                        }
                        DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                            AssistantLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.displayName) },
                                    onClick = {
                                        AssistantLanguageManager.setLanguage(lang)
                                        showLanguageMenu = false
                                    },
                                    trailingIcon = {
                                        if (lang == language) Icon(Icons.Default.Check, contentDescription = null, tint = TealPrimary)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (!isOnlineMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            AssistantStrings.offlineNotice(language),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.assistant_input_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = { startVoiceInput() }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = TealPrimary)
                    }
                    IconButton(onClick = { send(input) }, enabled = input.isNotBlank()) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = TealPrimary)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    language = language,
                    onQuickReply = { send(it) },
                    onRetry = { lastFailedText?.let { send(it) } }
                )
            }
            if (isThinking) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TealPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    language: AssistantLanguage,
    onQuickReply: (String) -> Unit,
    onRetry: () -> Unit
) {
    val bubbleColor = if (message.fromUser) TealPrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.fromUser) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = if (message.fromUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }

        if (message.canRetry) {
            Spacer(Modifier.height(6.dp))
            AssistChip(
                onClick = onRetry,
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(AssistantStrings.retryLabel(language)) }
            )
        }

        if (message.viewpoints.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(message.viewpoints) { vp -> ViewpointCard(vp) }
            }
        }

        if (message.tripPreview != null) {
            Spacer(Modifier.height(8.dp))
            TripPreviewCard(message.tripPreview)
        }

        if (!message.fromUser && message.quickReplies.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                message.quickReplies.forEach { option ->
                    AssistChip(
                        onClick = { onQuickReply(option) },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewpointCard(vp: ViewpointSuggestion) {
    ElevatedCard(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (vp.imageUrl != null) {
                NetworkImage(
                    url = vp.imageUrl,
                    contentDescription = vp.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TealPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Landscape, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(vp.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Text(vp.roleInTrip, style = MaterialTheme.typography.labelSmall, color = TealPrimary)
            Spacer(Modifier.height(4.dp))
            Text(vp.info, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}

@Composable
private fun TripPreviewCard(trip: com.pathsathi.app.data.model.Trip) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(0.85f)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(trip.tripName, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text("\u2192 ${trip.destination}", style = MaterialTheme.typography.bodySmall)
            if (trip.travelWith.isNotBlank()) {
                Text(trip.travelWith, style = MaterialTheme.typography.bodySmall)
            }
            if (trip.stops.isNotEmpty()) {
                Text("Stops: ${trip.stops.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            if (trip.stay.isNotBlank()) {
                Text("Stay: ${trip.stay}", style = MaterialTheme.typography.bodySmall)
            }
            trip.budget?.let {
                Text("\u20b9${it.toInt()}", style = MaterialTheme.typography.bodySmall)
            }
            if (trip.notes.isNotBlank()) {
                Text(trip.notes, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}
