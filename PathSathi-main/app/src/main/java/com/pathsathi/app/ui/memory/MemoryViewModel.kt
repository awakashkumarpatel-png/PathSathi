package com.pathsathi.app.ui.memory
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class MemoryViewModel(app:Application):AndroidViewModel(app){private val repo=(app as PathSathiApp).repository;private val _entries=MutableStateFlow<List<MemoryEntryEntity>>(emptyList());val entries:StateFlow<List<MemoryEntryEntity>> = _entries;private val _trip=MutableStateFlow<TripEntity?>(null);val trip:StateFlow<TripEntity?> = _trip;fun loadTrip(id:Long?)=viewModelScope.launch{val t=id?.let{repo.getTrip(it)}?:repo.observeActiveTrip().first()?:repo.observeTrips().first().firstOrNull();_trip.value=t;if(t!=null)repo.observeMemoryForTrip(t.id).collect{_entries.value=it}else _entries.value=emptyList()};fun addEntry(id:Long?,place:String,note:String){if(id==null||id<=0||place.isBlank())return;viewModelScope.launch{repo.addMemory(MemoryEntryEntity(tripId=id,place=place.trim(),note=note.trim(),photoUri=null,dateEpochMs=System.currentTimeMillis()))}};fun deleteEntry(entry:MemoryEntryEntity)=viewModelScope.launch{repo.deleteMemory(entry)}
}
