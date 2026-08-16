package com.pathsathi.app.ui.budget
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class BudgetViewModel(app:Application):AndroidViewModel(app){private val repo=(app as PathSathiApp).repository;private val _trip=MutableStateFlow<TripEntity?>(null);val trip:StateFlow<TripEntity?> = _trip;private val _expenses=MutableStateFlow<List<BudgetExpenseEntity>>(emptyList());val expenses:StateFlow<List<BudgetExpenseEntity>> = _expenses;private val _spent=MutableStateFlow(0);val spent:StateFlow<Int> = _spent;private val _travelers=MutableStateFlow<List<TravelerEntity>>(emptyList());val travelers:StateFlow<List<TravelerEntity>> = _travelers
 fun load(id:Long)=viewModelScope.launch{loadInternal(id)};fun loadActive()=viewModelScope.launch{(repo.observeActiveTrip().first()?:repo.observeTrips().first().firstOrNull())?.let{loadInternal(it.id)}}
 private suspend fun loadInternal(id:Long){_trip.value=repo.getTrip(id);viewModelScope.launch{repo.observeExpenses(id).collect{_expenses.value=it}};viewModelScope.launch{repo.observeTotalSpent(id).collect{_spent.value=it}};viewModelScope.launch{repo.observeTravelers(id).collect{_travelers.value=it}}}
 fun addExpense(id:Long,cat:String,amt:Int,note:String,payer:Long?){if(amt<=0)return;viewModelScope.launch{repo.addExpense(BudgetExpenseEntity(tripId=id,category=cat,amountInr=amt,note=note,dateEpochMs=System.currentTimeMillis(),travelerId=payer))}}
 fun addTraveler(id:Long,name:String){if(name.isBlank())return;viewModelScope.launch{repo.saveTraveler(TravelerEntity(tripId=id,name=name.trim()))}}
 fun removeTraveler(t:TravelerEntity)=viewModelScope.launch{repo.deleteTraveler(t)}
 fun deleteExpense(e:BudgetExpenseEntity)=viewModelScope.launch{repo.deleteExpense(e)}
}
