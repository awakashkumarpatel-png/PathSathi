package com.pathsathi.app.ui.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.MemoryEntryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MemoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository

    private val _entries = MutableStateFlow<List<MemoryEntryEntity>>(emptyList())
    val entries: StateFlow<List<MemoryEntryEntity>> = _entries

    init {
        viewModelScope.launch { repo.observeMemory().collect { _entries.value = it } }
    }

    fun addEntry(tripId: Long, place: String, note: String) {
        if (place.isBlank()) return
        viewModelScope.launch {
            repo.addMemory(
                MemoryEntryEntity(
                    tripId = tripId, place = place, note = note,
                    photoUri = null, dateEpochMs = System.currentTimeMillis()
                )
            )
        }
    }
}
