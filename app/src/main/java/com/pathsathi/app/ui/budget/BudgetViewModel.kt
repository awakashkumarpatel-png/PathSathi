package com.pathsathi.app.ui.budget

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.BudgetExpenseEntity
import com.pathsathi.app.data.db.TravelerEntity
import com.pathsathi.app.data.db.TripEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository

    private val _trip = MutableStateFlow<TripEntity?>(null)
    val trip: StateFlow<TripEntity?> = _trip

    private val _expenses = MutableStateFlow<List<BudgetExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<BudgetExpenseEntity>> = _expenses

    private val _spent = MutableStateFlow(0)
    val spent: StateFlow<Int> = _spent

    private val _travelers = MutableStateFlow<List<TravelerEntity>>(emptyList())
    val travelers: StateFlow<List<TravelerEntity>> = _travelers

    fun load(tripId: Long) {
        viewModelScope.launch {
            _trip.value = repo.getTrip(tripId)
            repo.observeExpenses(tripId).collect { _expenses.value = it }
        }
        viewModelScope.launch {
            repo.observeTotalSpent(tripId).collect { _spent.value = it }
        }
        viewModelScope.launch {
            repo.observeTravelers(tripId).collect { _travelers.value = it }
        }
    }

    fun addExpense(tripId: Long, category: String, amountInr: Int, note: String, travelerId: Long?) {
        if (amountInr <= 0) return
        viewModelScope.launch {
            repo.addExpense(
                BudgetExpenseEntity(
                    tripId = tripId, category = category, amountInr = amountInr,
                    note = note, dateEpochMs = System.currentTimeMillis(), travelerId = travelerId
                )
            )
        }
    }

    fun deleteExpense(expense: BudgetExpenseEntity) {
        viewModelScope.launch { repo.deleteExpense(expense) }
    }

    fun addTraveler(tripId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveTraveler(TravelerEntity(tripId = tripId, name = name))
        }
    }

    fun removeTraveler(traveler: TravelerEntity) {
        viewModelScope.launch { repo.deleteTraveler(traveler) }
    }
}
