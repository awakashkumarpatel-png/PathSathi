package com.pathsathi.app.ui.safety

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.data.db.EmergencyContactEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SafetyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository

    private val _contacts = MutableStateFlow<List<EmergencyContactEntity>>(emptyList())
    val contacts: StateFlow<List<EmergencyContactEntity>> = _contacts

    init {
        viewModelScope.launch { repo.observeEmergencyContacts().collect { _contacts.value = it } }
    }

    fun addTrustedContact(name: String, phone: String, relation: String) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            repo.saveEmergencyContact(EmergencyContactEntity(name = name, phone = phone, relation = relation, isTrusted = true))
        }
    }

    fun removeContact(contact: EmergencyContactEntity) {
        viewModelScope.launch { repo.deleteEmergencyContact(contact) }
    }
}
