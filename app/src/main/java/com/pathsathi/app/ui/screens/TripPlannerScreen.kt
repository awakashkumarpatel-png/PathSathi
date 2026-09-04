package com.pathsathi.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.local.TripPlanEntity
import com.pathsathi.app.data.model.Trek
import com.pathsathi.app.data.model.TripItinerary
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.data.repository.TripPlannerEngine
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
import kotlinx.coroutines.launch

@Composable
fun TripPlannerScreen(onBack: () -> Unit, onViewSavedItineraries: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { PathSathiDatabase.getInstance(context).tripPlanDao() }

    var selectedTrek by remember { mutableStateOf<Trek?>(null) }
    var startingPoint by remember { mutableStateOf("") }
    var travelers by remember { mutableStateOf("2") }
    var budget by remember { mutableStateOf("5000") }
    var itinerary by remember { mutableStateOf<TripItinerary?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tp_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onViewSavedItineraries) {
                        Icon(Icons.Default.History, contentDescription = "Saved Itineraries")
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
                Text(
                    "Fill in the details — your itinerary generates automatically.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedTrek?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.tp_destination_label)) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TrekRepository.treks.forEach { trek ->
                            DropdownMenuItem(
                                text = { Text("${trek.name} (${trek.location})") },
                                onClick = { selectedTrek = trek; expanded = false }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = startingPoint,
                    onValueChange = { startingPoint = it },
                    label = { Text(stringResource(R.string.tp_starting_point)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = travelers,
                        onValueChange = { travelers = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.tp_travelers)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.tp_budget)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val trek = selectedTrek ?: return@Button
                        isGenerating = true
                        scope.launch {
                            itinerary = TripPlannerEngine.generate(
                                trek = trek,
                                startingPoint = startingPoint,
                                travelers = travelers.toIntOrNull() ?: 1,
                                budgetPerPerson = budget.toDoubleOrNull() ?: 0.0
                            )
                            isGenerating = false
                            savedMessage = null
                        }
                    },
                    enabled = selectedTrek != null && !isGenerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isGenerating) stringResource(R.string.tp_generating) else stringResource(R.string.tp_generate))
                }
            }

            itinerary?.let { plan ->
                item { Divider() }
                item {
                    Text(
                        "${plan.trekName} · ${plan.totalDurationDays} days · ${plan.travelers} travelers",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(stringResource(R.string.tp_from_to, plan.startingPoint, plan.destination), style = MaterialTheme.typography.bodySmall)
                    if (plan.approachDistanceKm != null && plan.approachTravelTimeHours != null) {
                        Text(
                            stringResource(
                                R.string.tp_approx_travel,
                                String.format("%.0f", plan.approachDistanceKm),
                                plan.startingPoint,
                                String.format("%.1f", plan.approachTravelTimeHours)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (plan.startingPoint != "Not specified") {
                        Text(
                            stringResource(R.string.tp_travel_unavailable, plan.startingPoint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Text(stringResource(R.string.tp_day_wise_plan), fontWeight = FontWeight.Bold)
                }
                items(plan.days) { day ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.tp_day_label, day.dayNumber, day.title), fontWeight = FontWeight.Bold)
                            Text(day.activity, style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.tp_stay_label, day.stay), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Text(stringResource(R.string.tp_estimated_cost, String.format("%.0f", plan.estimatedCost)), fontWeight = FontWeight.Bold)
                    plan.costBreakdown.forEach { (label, amount) ->
                        Text("• $label: ₹${String.format("%.0f", amount)}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                item {
                    Text(stringResource(R.string.tp_packing_list), fontWeight = FontWeight.Bold)
                    Text(plan.packingList.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }

                item {
                    Text(stringResource(R.string.tp_safety_notes), fontWeight = FontWeight.Bold)
                    Text(plan.safetyNotes, style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.tp_best_season, plan.bestSeason), style = MaterialTheme.typography.bodySmall)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    dao.insert(
                                        TripPlanEntity(
                                            trekId = selectedTrek!!.id,
                                            trekName = plan.trekName,
                                            startingPoint = plan.startingPoint,
                                            startDate = System.currentTimeMillis(),
                                            endDate = System.currentTimeMillis() + plan.totalDurationDays * 86_400_000L,
                                            travelers = plan.travelers,
                                            budget = plan.estimatedCost,
                                            itineraryJson = plan.days.joinToString("|") { "${it.dayNumber}:${it.title}" },
                                            estimatedCost = plan.estimatedCost
                                        )
                                    )
                                    savedMessage = "Trip saved"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.action_save_trip)) }

                        OutlinedButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("${plan.trekName} Trip Plan")
                                    appendLine("${plan.startingPoint} -> ${plan.destination}")
                                    appendLine("${plan.totalDurationDays} days, ${plan.travelers} travelers")
                                    plan.days.forEach { appendLine("Day ${it.dayNumber}: ${it.title}") }
                                    appendLine("Estimated cost: ₹${String.format("%.0f", plan.estimatedCost)}")
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share trip plan"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_share))
                        }
                    }
                }

                savedMessage?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
