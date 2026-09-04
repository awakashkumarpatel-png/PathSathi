package com.pathsathi.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.stringResource
import com.pathsathi.app.R
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
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.local.TripPlanEntity
import com.pathsathi.app.ui.theme.TealPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    onPlanTrip: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { PathSathiDatabase.getInstance(context).tripPlanDao() }
    val scope = rememberCoroutineScope()

    val plans by dao.getAll().collectAsState(initial = null)
    var pendingDelete by remember { mutableStateOf<TripPlanEntity?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPlanTrip,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_plan_a_trip)) },
                containerColor = TealPrimary,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        }
    ) { padding ->
        val list = plans
        when {
            list == null -> {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            list.isEmpty() -> {
                Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = TealPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.preview_empty), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.preview_empty_desc), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onPlanTrip,
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_plan_a_trip))
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { plan ->
                        PlanPreviewCard(
                            plan = plan,
                            expanded = expandedId == plan.id,
                            onToggleExpand = {
                                expandedId = if (expandedId == plan.id) null else plan.id
                            },
                            onDelete = { pendingDelete = plan }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    pendingDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.preview_delete_title)) },
            text = { Text(stringResource(R.string.preview_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.delete(plan) }
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun PlanPreviewCard(
    plan: TripPlanEntity,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dayEntries = remember(plan.itineraryJson) {
        plan.itineraryJson.split("|").filter { it.isNotBlank() }
    }

    ElevatedCard(
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plan.trekName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.preview_from, plan.startingPoint), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${dateFormat.format(Date(plan.startDate))} – ${dateFormat.format(Date(plan.endDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatChipPreview(Icons.Default.Group, "${plan.travelers} travelers")
                StatChipPreview(Icons.Default.CurrencyRupee, "₹${String.format("%.0f", plan.estimatedCost)}")
                StatChipPreview(Icons.Default.CalendarMonth, "${dayEntries.size} days")
            }

            if (expanded && dayEntries.isNotEmpty()) {
                Divider(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                Text(stringResource(R.string.tp_day_wise_plan), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                dayEntries.forEach { entry ->
                    val parts = entry.split(":", limit = 2)
                    val text = if (parts.size == 2) "Day ${parts[0]}: ${parts[1]}" else entry
                    Text("• $text", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                if (expanded) "Tap to collapse" else "Tap to view day-wise plan",
                style = MaterialTheme.typography.labelSmall,
                color = TealPrimary
            )
        }
    }
}

@Composable
private fun StatChipPreview(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
