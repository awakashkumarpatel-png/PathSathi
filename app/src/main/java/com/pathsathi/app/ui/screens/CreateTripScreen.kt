@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.local.toEntity
import com.pathsathi.app.data.local.toTrip
import com.pathsathi.app.data.model.Trip
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class TripStep {
    NAME, DESTINATION, START_DATE, END_DATE, TRAVEL_WITH, MEMBERS, STOPS, STAY, BUDGET, NOTES, PREVIEW
}

private val travelWithOptions = listOf("Solo", "Family", "Friends", "Partner", "Group")

@Composable
fun CreateTripScreen(
    editTripId: Long?,
    onBack: () -> Unit,
    onSaved: (tripId: Long, isNewTrip: Boolean) -> Unit
) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).tripDao() }
    val scope = rememberCoroutineScope()

    var loadedExisting by remember { mutableStateOf(editTripId == null) }
    var existingId by rememberSaveable { mutableStateOf(0L) }

    var tripName by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var startDateTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDateTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var travelWith by rememberSaveable { mutableStateOf("") }
    val members = remember { mutableStateListOf<String>() }
    val stops = remember { mutableStateListOf<String>() }
    var stay by rememberSaveable { mutableStateOf("") }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    var stepIndex by rememberSaveable { mutableStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Load existing trip when editing
    LaunchedEffect(editTripId) {
        if (editTripId != null) {
            val existing = dao.getById(editTripId)
            if (existing != null) {
                val trip = existing.toTrip()
                existingId = trip.id
                tripName = trip.tripName
                destination = trip.destination
                startDateTime = trip.startDateTime
                endDateTime = trip.endDateTime
                travelWith = trip.travelWith
                members.clear(); members.addAll(trip.members)
                stops.clear(); stops.addAll(trip.stops)
                stay = trip.stay
                budgetText = trip.budget?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
                notes = trip.notes
                stepIndex = TripStep.PREVIEW.ordinal
            }
            loadedExisting = true
        }
    }

    // Effective step order — Members is skipped automatically for Solo trips
    val steps = remember(travelWith) {
        if (travelWith == "Solo") {
            TripStep.values().filter { it != TripStep.MEMBERS }
        } else {
            TripStep.values().toList()
        }
    }
    val safeIndex = stepIndex.coerceIn(0, steps.size - 1)
    val currentStep = steps[safeIndex]

    fun goTo(step: TripStep) {
        errorText = null
        val idx = steps.indexOf(step)
        stepIndex = if (idx >= 0) idx else safeIndex
    }

    fun advance() {
        errorText = null
        if (safeIndex < steps.size - 1) stepIndex = safeIndex + 1
    }

    fun back() {
        errorText = null
        if (safeIndex > 0) stepIndex = safeIndex - 1 else onBack()
    }

    fun trySave() {
        val name = tripName.trim()
        val dest = destination.trim()
        val start = startDateTime
        val end = endDateTime
        val budgetValue = budgetText.trim().let { if (it.isEmpty()) null else it.toDoubleOrNull() }

        when {
            name.isBlank() -> { goTo(TripStep.NAME); errorText = "Trip name is required" }
            dest.isBlank() -> { goTo(TripStep.DESTINATION); errorText = "Destination is required" }
            start == null -> { goTo(TripStep.START_DATE); errorText = "Start date & time is required" }
            end == null -> { goTo(TripStep.END_DATE); errorText = "End date & time is required" }
            end <= start -> { goTo(TripStep.END_DATE); errorText = "End date must be after start date" }
            travelWith.isBlank() -> { goTo(TripStep.TRAVEL_WITH); errorText = "Please select who you're travelling with" }
            budgetText.isNotBlank() && budgetValue == null -> { goTo(TripStep.BUDGET); errorText = "Enter a valid budget amount" }
            budgetValue != null && budgetValue < 0 -> { goTo(TripStep.BUDGET); errorText = "Budget cannot be negative" }
            else -> {
                val trip = Trip(
                    id = existingId,
                    tripName = name,
                    destination = dest,
                    startDateTime = start,
                    endDateTime = end,
                    travelWith = travelWith,
                    members = members.map { it.trim() }.filter { it.isNotBlank() },
                    stops = stops.map { it.trim() }.filter { it.isNotBlank() },
                    stay = stay.trim(),
                    budget = budgetValue,
                    notes = notes.trim()
                )
                scope.launch {
                    if (existingId != 0L) {
                        dao.update(trip.toEntity())
                        onSaved(existingId, false)
                    } else {
                        val newId = dao.insert(trip.toEntity())
                        onSaved(newId, true)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingId != 0L) "Edit Trip" else "Create Trip") },
                navigationIcon = {
                    IconButton(onClick = { back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!loadedExisting) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (currentStep != TripStep.PREVIEW) {
                LinearProgressIndicator(
                    progress = (safeIndex + 1f) / steps.size,
                    modifier = Modifier.fillMaxWidth(),
                    color = TealPrimary
                )
                Text(
                    "Step ${safeIndex + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    TripStep.NAME -> StepTextField(
                        icon = Icons.Default.Sell,
                        label = "Trip Name",
                        value = tripName,
                        onValueChange = { tripName = it },
                        onNext = ::advance,
                        errorText = errorText
                    )
                    TripStep.DESTINATION -> StepTextField(
                        icon = Icons.Default.LocationOn,
                        label = "Destination",
                        value = destination,
                        onValueChange = { destination = it },
                        onNext = ::advance,
                        errorText = errorText
                    )
                    TripStep.START_DATE -> StepDateTimeField(
                        icon = Icons.Default.CalendarMonth,
                        label = "Start Date & Time",
                        value = startDateTime,
                        onValueChange = { startDateTime = it; advance() },
                        errorText = errorText
                    )
                    TripStep.END_DATE -> StepDateTimeField(
                        icon = Icons.Default.Flag,
                        label = "End Date & Time",
                        value = endDateTime,
                        minMillis = startDateTime,
                        onValueChange = { endDateTime = it; advance() },
                        errorText = errorText
                    )
                    TripStep.TRAVEL_WITH -> StepChoiceField(
                        icon = Icons.Default.Group,
                        label = "Travel With",
                        options = travelWithOptions,
                        selected = travelWith,
                        onSelect = { travelWith = it; advance() },
                        errorText = errorText
                    )
                    TripStep.MEMBERS -> StepListField(
                        icon = Icons.Default.Person,
                        label = "Members",
                        hint = "Add member name",
                        items = members,
                        onAdd = { members.add(it) },
                        onRemove = { members.removeAt(it) },
                        onNext = ::advance,
                        onSkip = ::advance
                    )
                    TripStep.STOPS -> StepListField(
                        icon = Icons.Default.PinDrop,
                        label = "Stops / Places to Visit",
                        hint = "Add a stop or place",
                        items = stops,
                        onAdd = { stops.add(it) },
                        onRemove = { stops.removeAt(it) },
                        onNext = ::advance,
                        onSkip = ::advance
                    )
                    TripStep.STAY -> StepTextField(
                        icon = Icons.Default.Hotel,
                        label = "Stay Details",
                        value = stay,
                        onValueChange = { stay = it },
                        onNext = ::advance,
                        onSkip = ::advance,
                        optional = true
                    )
                    TripStep.BUDGET -> StepTextField(
                        icon = Icons.Default.CurrencyRupee,
                        label = "Budget / Estimated Cost",
                        value = budgetText,
                        onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                        onNext = ::advance,
                        onSkip = ::advance,
                        optional = true,
                        keyboardType = KeyboardType.Number,
                        errorText = errorText
                    )
                    TripStep.NOTES -> StepTextField(
                        icon = Icons.Default.Notes,
                        label = "Notes",
                        value = notes,
                        onValueChange = { notes = it },
                        onNext = ::advance,
                        onSkip = ::advance,
                        optional = true,
                        singleLine = false
                    )
                    TripStep.PREVIEW -> TripPreviewContent(
                        tripName = tripName,
                        destination = destination,
                        startDateTime = startDateTime,
                        endDateTime = endDateTime,
                        travelWith = travelWith,
                        members = members,
                        stops = stops,
                        stay = stay,
                        budgetText = budgetText,
                        notes = notes,
                        errorText = errorText,
                        onEditStep = { goTo(it) },
                        onCreate = { trySave() },
                        isEdit = existingId != 0L
                    )
                }
            }
        }
    }
}

@Composable
private fun StepScaffold(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    errorText: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        content()
        if (errorText != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StepTextField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: (() -> Unit)? = null,
    optional: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(label) { focusRequester.requestFocus() }

    StepScaffold(icon = icon, label = label, errorText = errorText) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            placeholder = { Text(if (optional) "$label (optional)" else label) }
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (optional && onSkip != null) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_skip)) }
            }
            Button(
                onClick = onNext,
                enabled = optional || value.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_next))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun StepChoiceField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    errorText: String?
) {
    StepScaffold(icon = icon, label = label, errorText = errorText) {
        FlowRowSimple {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowSimple(content: @Composable () -> Unit) {
    // Simple wrap layout without extra dependencies
    androidx.compose.foundation.layout.FlowRow(content = { content() })
}

@Composable
private fun StepListField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    hint: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    var entry by remember { mutableStateOf("") }
    StepScaffold(icon = icon, label = label, errorText = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = entry,
                onValueChange = { entry = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(hint) },
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = {
                if (entry.isNotBlank()) { onAdd(entry.trim()); entry = "" }
            }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = TealPrimary)
            }
        }
        Spacer(Modifier.height(12.dp))
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("• $item", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_skip)) }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_next))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun StepDateTimeField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Long?,
    minMillis: Long? = null,
    onValueChange: (Long) -> Unit,
    errorText: String?
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    StepScaffold(icon = icon, label = label, errorText = errorText) {
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(value?.let { formatDateTime(it) } ?: "Select $label")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value ?: minMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        pendingDateMillis = millis
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = value ?: System.currentTimeMillis() }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.action_select_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val base = pendingDateMillis ?: value ?: System.currentTimeMillis()
                    val combined = Calendar.getInstance().apply {
                        timeInMillis = base
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                    onValueChange(combined)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_done_wizard)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun TripPreviewContent(
    tripName: String,
    destination: String,
    startDateTime: Long?,
    endDateTime: Long?,
    travelWith: String,
    members: List<String>,
    stops: List<String>,
    stay: String,
    budgetText: String,
    notes: String,
    errorText: String?,
    onEditStep: (TripStep) -> Unit,
    onCreate: () -> Unit,
    isEdit: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(stringResource(R.string.ct_trip_preview), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ct_review_edit), style = MaterialTheme.typography.bodySmall)
        }
        item { PreviewRow(Icons.Default.Sell, "Trip Name", tripName.ifBlank { "—" }) { onEditStep(TripStep.NAME) } }
        item { PreviewRow(Icons.Default.LocationOn, "Destination", destination.ifBlank { "—" }) { onEditStep(TripStep.DESTINATION) } }
        item { PreviewRow(Icons.Default.CalendarMonth, "Start", startDateTime?.let { formatDateTime(it) } ?: "—") { onEditStep(TripStep.START_DATE) } }
        item { PreviewRow(Icons.Default.Flag, "End", endDateTime?.let { formatDateTime(it) } ?: "—") { onEditStep(TripStep.END_DATE) } }
        item { PreviewRow(Icons.Default.Group, "Travel With", travelWith.ifBlank { "—" }) { onEditStep(TripStep.TRAVEL_WITH) } }
        item {
            PreviewRow(
                Icons.Default.Person,
                "Members",
                if (members.isEmpty()) "—" else "${members.size}: ${members.joinToString(", ")}"
            ) { onEditStep(TripStep.MEMBERS) }
        }
        item {
            PreviewRow(
                Icons.Default.PinDrop,
                "Stops",
                if (stops.isEmpty()) "—" else stops.joinToString(", ")
            ) { onEditStep(TripStep.STOPS) }
        }
        item { PreviewRow(Icons.Default.Hotel, "Stay", stay.ifBlank { "—" }) { onEditStep(TripStep.STAY) } }
        item { PreviewRow(Icons.Default.CurrencyRupee, "Budget", budgetText.ifBlank { "—" }.let { if (it != "—") "₹$it" else it }) { onEditStep(TripStep.BUDGET) } }
        item { PreviewRow(Icons.Default.Notes, "Notes", notes.ifBlank { "—" }) { onEditStep(TripStep.NOTES) } }

        if (errorText != null) {
            item { Text(errorText, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isEdit) "Save Changes" else "Create Trip")
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun PreviewRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onEdit: () -> Unit
) {
    ElevatedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TealPrimary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit $label")
            }
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(millis))
