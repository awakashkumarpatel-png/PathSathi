package com.pathsathi.app.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.data.db.TravelerEntity
import com.pathsathi.app.ui.theme.PsSurfaceAlt
import java.util.Calendar

private val CATEGORIES = listOf("Travel", "Stay", "Food", "Tickets", "Activities", "Shopping", "Other")

@Composable
fun BudgetScreen(tripId: Long?, vm: BudgetViewModel = viewModel()) {
    LaunchedEffect(tripId) { if (tripId != null) vm.load(tripId) else vm.loadActive() }

    val trip by vm.trip.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val spent by vm.spent.collectAsState()
    val travelers by vm.travelers.collectAsState()

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var payer by remember { mutableStateOf<TravelerEntity?>(null) }
    var categoryOpen by remember { mutableStateOf(false) }
    var payerOpen by remember { mutableStateOf(false) }
    var travelerName by remember { mutableStateOf("") }

    val currentTrip = trip
    if (currentTrip == null) {
        return Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Smart Budget", style = MaterialTheme.typography.headlineMedium)
            Text("Create a trip first.")
        }
    }

    val remaining = currentTrip.budgetInr - spent
    val now = Calendar.getInstance()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val weekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val today = expenses.filter { it.dateEpochMs >= todayStart }.sumOf { it.amountInr }
    val week = expenses.filter { it.dateEpochMs >= weekStart }.sumOf { it.amountInr }
    val month = expenses.filter { it.dateEpochMs >= monthStart }.sumOf { it.amountInr }
    val categoryTotals = expenses.groupBy { it.category }.mapValues { (_, xs) -> xs.sumOf { it.amountInr } }.toList().sortedByDescending { it.second }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Smart Budget", style = MaterialTheme.typography.headlineMedium)
            Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Budget: ₹${currentTrip.budgetInr}")
                    Text("Spent: ₹$spent")
                    Text("Remaining: ₹$remaining")
                    if (remaining < 0) Text("Budget exceeded by ₹${-remaining}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Text("Expense breakdown", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Today: ₹$today")
                    Text("This week: ₹$week")
                    Text("This month: ₹$month")
                }
            }
        }
        item { Text("By category", style = MaterialTheme.typography.titleMedium) }
        items(categoryTotals) { (cat, total) -> Text("$cat · ₹$total") }

        item {
            OutlinedTextField(
                amount,
                { amount = it.filter(Char::isDigit) },
                label = { Text("Amount (₹)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { OutlinedTextField(note, { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { categoryOpen = true }) { Text(category) }
                    DropdownMenu(categoryOpen, { categoryOpen = false }) {
                        CATEGORIES.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoryOpen = false }) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { payerOpen = true }) { Text(payer?.name ?: "Shared expense") }
                    DropdownMenu(payerOpen, { payerOpen = false }) {
                        DropdownMenuItem(text = { Text("Shared expense") }, onClick = { payer = null; payerOpen = false })
                        travelers.forEach { t -> DropdownMenuItem(text = { Text(t.name) }, onClick = { payer = t; payerOpen = false }) }
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    vm.addExpense(currentTrip.id, category, amount.toIntOrNull() ?: 0, note, payer?.id)
                    amount = ""; note = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (amount.toIntOrNull() ?: 0) > 0
            ) { Text("Add Expense") }
        }

        item {
            Text("Who owes whom", style = MaterialTheme.typography.titleMedium)
            Text(
                if (travelers.size < 2) "Add at least two travelers to calculate settlement."
                else "Shared expenses are split equally. Individual expenses are credited to the selected payer.",
                style = MaterialTheme.typography.labelSmall
            )
        }
        items(travelers) { t ->
            val paid = expenses.filter { it.travelerId == t.id }.sumOf { it.amountInr }
            val share = if (travelers.isEmpty()) 0.0 else spent.toDouble() / travelers.size
            val net = paid - share
            Text("${t.name}: paid ₹$paid · fair share ₹${"%.0f".format(share)} · ${if (net >= 0) "gets" else "owes"} ₹${"%.0f".format(kotlin.math.abs(net))}")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(travelerName, { travelerName = it }, label = { Text("Traveler name") }, modifier = Modifier.weight(1f))
                Button(onClick = { vm.addTraveler(currentTrip.id, travelerName); travelerName = "" }, enabled = travelerName.isNotBlank()) { Text("Add") }
            }
        }
        items(travelers) { t ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(t.name)
                OutlinedButton(onClick = { vm.removeTraveler(t) }) { Text("Remove") }
            }
        }
        item { Text("Expenses", style = MaterialTheme.typography.titleMedium) }
        items(expenses) { e ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(e.category)
                        if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.labelSmall)
                        Text(if (e.travelerId == null) "Shared expense" else "Paid by selected traveler", style = MaterialTheme.typography.labelSmall)
                    }
                    Column {
                        Text("₹${e.amountInr}")
                        TextButton(onClick = { vm.deleteExpense(e) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}
