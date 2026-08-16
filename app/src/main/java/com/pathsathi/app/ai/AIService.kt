package com.pathsathi.app.ai

import com.pathsathi.app.data.db.TripEntity
import com.pathsathi.app.engine.SathiEngine

/**
 * Single interface both the offline fallback and any future online AI provider
 * implement. UI/ViewModel code should only ever depend on this interface
 * (via AIOrchestrator below) — never directly on a concrete implementation —
 * so swapping in real AI later touches no screen code.
 */
interface AIService {
    suspend fun converse(request: NLRequest, tripContext: TripContext): AIResponse
    suspend fun personalizedRecommendations(tripContext: TripContext, userContext: UserContext): List<Recommendation>
    suspend fun suggestItineraryChange(tripContext: TripContext): ItineraryChangeSuggestion?
    fun isLive(): Boolean
}

/**
 * Always-available offline implementation. Delegates conversation to the
 * existing rule-based SathiEngine so behavior stays consistent whether or
 * not an online AI is ever configured. Never calls the network.
 */
class OfflineAIFallback(
    private val activeTripProvider: suspend () -> TripEntity?,
    private val spentProvider: suspend (Long) -> Int
) : AIService {

    override suspend fun converse(request: NLRequest, tripContext: TripContext): AIResponse {
        val trip = activeTripProvider()
        val spent = trip?.let { spentProvider(it.id) } ?: 0
        val reply = SathiEngine.reply(request.text, trip, spent, request.isHindi)
        return AIResponse(text = reply, fromOnlineAI = false)
    }

    override suspend fun personalizedRecommendations(tripContext: TripContext, userContext: UserContext): List<Recommendation> {
        // Simple offline heuristic: recommend based on stated trip type only — no fabricated personalization.
        val type = tripContext.tripType ?: return emptyList()
        return listOf(
            Recommendation(
                title = "More ideas for a $type trip",
                reason = "Based on the trip type you selected in the planner.",
                source = "offline-rules"
            )
        )
    }

    override suspend fun suggestItineraryChange(tripContext: TripContext): ItineraryChangeSuggestion? {
        val day = tripContext.dayNumber ?: return null
        val total = tripContext.totalDays ?: return null
        if (tripContext.spentInr != null && tripContext.budgetInr != null && tripContext.spentInr > tripContext.budgetInr) {
            return ItineraryChangeSuggestion(
                summary = "Consider trimming a lower-priority stop on day ${day + 1}.",
                reason = "You're over budget so far (day $day of $total)."
            )
        }
        return null
    }

    override fun isLive(): Boolean = false
}

/**
 * Integration point for a real online AI service (e.g. the Claude API or
 * another LLM provider) for natural conversation, personalized
 * recommendations, and intelligent itinerary changes. intentionally optional —
 * doing so would require a network call and an API key, which must stay
 * optional per the offline-first requirement.
 *
 * To go live later:
 *   1. Add the provider's SDK/HTTP client dependency.
 *   2. Read the API key from a non-committed local.properties entry via
 *      BuildConfig — never hard-code it in source.
 *   3. Implement this interface, calling the real API and mapping responses
 *      into AIResponse/Recommendation/ItineraryChangeSuggestion.
 *   4. isLive() must return false (not throw) whenever the key/network is
 *      unavailable, so AIOrchestrator falls back to OfflineAIFallback cleanly.
 *   5. Voice conversation: if the online provider supports streaming/voice
 *      responses, wire it through VoiceEngine.speak() the same way the
 *      offline path already does in SathiViewModel — no other change needed.
 */
interface OnlineAIProvider : AIService {
    override fun isLive(): Boolean
}

/**
 * Picks the online provider when available/live, otherwise the offline
 * fallback — and if an online call throws, callers should catch and retry
 * against [offline] rather than surface an error, per the "graceful offline
 * fallback" requirement.
 */
class AIOrchestrator(
    private val offline: OfflineAIFallback,
    private val online: OnlineAIProvider? = null
) : AIService {

    private fun active(): AIService = if (online != null && online.isLive()) online else offline

    override suspend fun converse(request: NLRequest, tripContext: TripContext): AIResponse =
        try {
            active().converse(request, tripContext)
        } catch (e: Exception) {
            offline.converse(request, tripContext)
        }

    override suspend fun personalizedRecommendations(tripContext: TripContext, userContext: UserContext): List<Recommendation> =
        try {
            active().personalizedRecommendations(tripContext, userContext)
        } catch (e: Exception) {
            offline.personalizedRecommendations(tripContext, userContext)
        }

    override suspend fun suggestItineraryChange(tripContext: TripContext): ItineraryChangeSuggestion? =
        try {
            active().suggestItineraryChange(tripContext)
        } catch (e: Exception) {
            offline.suggestItineraryChange(tripContext)
        }

    override fun isLive(): Boolean = online?.isLive() ?: false
}
