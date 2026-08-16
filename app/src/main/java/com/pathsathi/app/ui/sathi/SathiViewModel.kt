package com.pathsathi.app.ui.sathi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pathsathi.app.PathSathiApp
import com.pathsathi.app.ai.AIOrchestrator
import com.pathsathi.app.ai.NLRequest
import com.pathsathi.app.ai.OfflineAIFallback
import com.pathsathi.app.ai.TripContext
import com.pathsathi.app.core.AppConfig
import com.pathsathi.app.core.ConnectivityObserver
import com.pathsathi.app.data.db.ChatMessageEntity
import com.pathsathi.app.engine.ItinerarySerializer
import com.pathsathi.app.engine.SathiEngine
import com.pathsathi.app.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SathiViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as PathSathiApp).repository
    val voiceEngine = VoiceEngine(app)

    // Context-aware Sathi: conversation always goes through AIOrchestrator, which
    // routes to an offline rule-based fallback today and can route to a real
    // online AI later (see com.pathsathi.app.ai.OnlineAIProvider) with zero
    // change to this screen/ViewModel. No OnlineAIProvider is passed in yet, so
    // AIOrchestrator always resolves to the offline path regardless of the
    // person's "Online AI" setting below — that setting is stored for when a
    // real provider exists, and is surfaced here so the UI can be honest about it.
    private val aiService = AIOrchestrator(
        offline = OfflineAIFallback(
            activeTripProvider = { repo.observeActiveTrip().first() ?: repo.observeTrips().first().firstOrNull() },
            spentProvider = { tripId -> repo.observeTotalSpent(tripId).first() }
        )
    )

    private val _isHindi = MutableStateFlow(false)
    val isHindi: StateFlow<Boolean> = _isHindi

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    /** True only if the person turned Online AI on AND is currently online — still doesn't mean a real provider exists yet. */
    val onlineAiRequestedAndReachable: StateFlow<Boolean> = combineToBooleanState(
        combine(AppConfig.onlineAiEnabled(app), ConnectivityObserver.isOnline(app)) { enabled, online -> enabled && online }
    )

    val messages: StateFlow<List<ChatMessageEntity>> =
        combineToState(repo.observeChat())

    init {
        voiceEngine.init { ready -> if (ready) voiceEngine.setLanguage(_isHindi.value) }
    }

    fun toggleLanguage() {
        _isHindi.value = !_isHindi.value
        voiceEngine.setLanguage(_isHindi.value)
    }

    fun setListening(listening: Boolean) { _isListening.value = listening }
    fun startVoiceListening(){_isListening.value=true;voiceEngine.startListening(_isHindi.value,{sendMessage(it)}){_isListening.value=false}}

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.addChatMessage(ChatMessageEntity(fromUser = true, text = text, timestampEpochMs = System.currentTimeMillis()))

            val activeTrip = repo.observeActiveTrip().first() ?: repo.observeTrips().first().firstOrNull()
            val expenseCommand = SathiEngine.parseExpenseCommand(text)
            if (activeTrip != null && expenseCommand != null) {
                repo.addExpense(
                    com.pathsathi.app.data.db.BudgetExpenseEntity(
                        tripId = activeTrip.id,
                        category = expenseCommand.category,
                        amountInr = expenseCommand.amountInr,
                        note = expenseCommand.note,
                        dateEpochMs = System.currentTimeMillis(),
                        travelerId = null
                    )
                )
                val confirmation = if (_isHindi.value) {
                    "₹${expenseCommand.amountInr} का ${expenseCommand.category} खर्च दर्ज कर दिया है${if (expenseCommand.note.isNotBlank()) " — ${expenseCommand.note}" else ""}."
                } else {
                    "Added ₹${expenseCommand.amountInr} ${expenseCommand.category} expense${if (expenseCommand.note.isNotBlank()) " — ${expenseCommand.note}" else ""}."
                }
                repo.addChatMessage(ChatMessageEntity(fromUser = false, text = confirmation, timestampEpochMs = System.currentTimeMillis()))
                voiceEngine.speak(confirmation)
                return@launch
            }
            val spent = if (activeTrip != null) repo.observeTotalSpent(activeTrip.id).first() else 0
            val context = TripContext(
                tripId = activeTrip?.id,
                destination = activeTrip?.destination,
                dayNumber = activeTrip?.currentDayIndex,
                totalDays = activeTrip?.days,
                budgetInr = activeTrip?.budgetInr,
                spentInr = spent,
                tripType = activeTrip?.tripType,
                nextDestinationName = activeTrip?.let { ItinerarySerializer.decode(it.itineraryJson).getOrNull(it.currentDayIndex.coerceAtLeast(0))?.places?.firstOrNull() ?: it.destination }
            )
            val response = aiService.converse(NLRequest(text, _isHindi.value), context)

            repo.addChatMessage(ChatMessageEntity(fromUser = false, text = response.text, timestampEpochMs = System.currentTimeMillis()))
            voiceEngine.speak(response.text)
        }
    }

    private fun combineToState(flow: kotlinx.coroutines.flow.Flow<List<ChatMessageEntity>>): StateFlow<List<ChatMessageEntity>> {
        val state = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        return state
    }

    private fun combineToBooleanState(flow: kotlinx.coroutines.flow.Flow<Boolean>): StateFlow<Boolean> {
        val state = MutableStateFlow(false)
        viewModelScope.launch { flow.collect { state.value = it } }
        return state
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
    }
}
