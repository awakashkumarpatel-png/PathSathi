package com.pathsathi.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pathsathi.app.ui.theme.PsSurfaceAlt

private val CATEGORIES = listOf("Travel", "Stay", "Food", "Tickets", "Activities", "Shopping", "Other")

@Composable
fun BudgetScreen(tripId: Long, vm: BudgetViewModel = viewModel()) {
    LaunchedEffect(tripId) { vm.load(tripId) }

    val trip by vm.trip.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val spent by vm.spent.collectAsState()

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Smart Budget", style = MaterialTheme.typography.headlineMedium)

        val totalBudget = trip?.budgetInr ?: 0
        val remaining = totalBudget - spent
        Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Total: ₹$totalBudget", style = MaterialTheme.typography.titleMedium)
                Text("Spent: ₹$spent", style = MaterialTheme.typography.bodyMedium)
                Text("Remaining: ₹$remaining", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("Amount (₹)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("Note") }, modifier = Modifier.fillMaxWidth()
        )
        Row {
            OutlinedButton(onClick = { categoryMenuOpen = true }) { Text(category) }
            DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                CATEGORIES.forEach { c ->
                    DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoryMenuOpen = false })
                }
            }
        }
        Button(onClick = {
            vm.addExpense(tripId, category, amount.toIntOrNull() ?: 0, note, null)
            amount = ""; note = ""
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Expense")
        }

        val travelers by vm.travelers.collectAsState()
        var travelerName by remember { mutableStateOf("") }
        Text("Group / Family travelers", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add travelers to this trip for shared vs. individual expense tracking.",
            style = MaterialTheme.typography.labelSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = travelerName, onValueChange = { travelerName = it },
                label = { Text("Traveler name") }, modifier = Modifier.weight(1f)
            )
            Button(onClick = { vm.addTraveler(tripId, travelerName); travelerName = "" }) { Text("Add") }
        }
        travelers.forEach { t ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(t.name, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { vm.removeTraveler(t) }) { Text("Remove") }
            }
        }

        Text("Expenses", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses) { e ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(e.category, style = MaterialTheme.typography.titleMedium)
                            if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("₹${e.amountInr}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
