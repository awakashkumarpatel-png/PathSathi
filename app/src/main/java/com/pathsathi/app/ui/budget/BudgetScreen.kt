package com.pathsathi.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
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
import com.pathsathi.app.data.db.TravelerEntity
import com.pathsathi.app.ui.theme.PsSurfaceAlt

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
    val id = trip?.id
    if (id == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Smart Budget", style = MaterialTheme.typography.headlineMedium)
            Text("Create a trip first.")
        }
        return
    }
    val sharedTotal = expenses.filter { it.travelerId == null }.sumOf { it.amountInr }
    val sharedShare = if (travelers.isNotEmpty()) sharedTotal.toDouble() / travelers.size else 0.0
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Smart Budget", style = MaterialTheme.typography.headlineMedium)
            Card(colors = CardDefaults.cardColors(containerColor = PsSurfaceAlt), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Total: ₹${trip?.budgetInr}")
                    Text("Spent: ₹$spent")
                    Text("Remaining: ₹${(trip?.budgetInr ?: 0) - spent}")
                }
            }
        }
        item {
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount (₹)") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
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
                    OutlinedButton(onClick = { payerOpen = true }) { Text(payer?.name ?: travelers.firstOrNull()?.name ?: "Select payer") }
                    DropdownMenu(payerOpen, { payerOpen = false }) {
                        travelers.forEach { t -> DropdownMenuItem(text = { Text(t.name) }, onClick = { payer = t; payerOpen = false }) }
                    }
                }
            }
        }
        item {
            Button(onClick = { vm.addExpense(id, category, amount.toIntOrNull() ?: 0, note, (payer ?: travelers.firstOrNull())?.id); amount = ""; note = "" }, modifier = Modifier.fillMaxWidth()) { Text("Add Expense") }
        }
        item { Text("Who owes whom", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                if (travelers.size < 2) "Add at least two travelers to calculate group settlement."
                else "Each expense is treated as shared equally; the payer is credited automatically.",
                style = MaterialTheme.typography.labelSmall
            )
        }
        items(travelers) { t ->
            val paid = expenses.filter { it.travelerId == t.id }.sumOf { it.amountInr }
            val share = if (travelers.isNotEmpty()) spent.toDouble() / travelers.size else 0.0
            val net = paid - share
            Text("${t.name}: paid ₹$paid · share ₹${"%.0f".format(share)} · balance ${if (net >= 0) "+" else "-"}₹${"%.0f".format(kotlin.math.abs(net))}")
        }
        item {
            var name by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Traveler name") }, modifier = Modifier.weight(1f))
                Button(onClick = { vm.addTraveler(id, name); name = "" }) { Text("Add") }
            }
        }
        items(travelers) { t -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(t.name); OutlinedButton(onClick = { vm.removeTraveler(t) }) { Text("Remove") } } }
        item { Text("Expenses", style = MaterialTheme.typography.titleMedium) }
        items(expenses) { e ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(e.category); if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.labelSmall); Text(if (e.travelerId == null) "Shared" else "Paid by traveler", style = MaterialTheme.typography.labelSmall) }
                    Text("₹${e.amountInr}")
                }
            }
        }
    }
}
