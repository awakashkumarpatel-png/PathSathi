package com.pathsathi.app.ai

/** A single natural-language turn from the user, with the language it was spoken/typed in. */
data class NLRequest(val text: String, val isHindi: Boolean)

/** Everything about the current trip an AI (online or offline) would need to answer well. */
data class TripContext(
    val tripId: Long?,
    val destination: String?,
    val dayNumber: Int?,
    val totalDays: Int?,
    val budgetInr: Int?,
    val spentInr: Int?,
    val tripType: String?,
    val nextDestinationName: String?
)

/** Minimal user-level context — kept small and local; never sent anywhere by the offline fallback. */
data class UserContext(
    val preferredLanguageIsHindi: Boolean,
    val travelerCount: Int
)

data class AIResponse(
    val text: String,
    /** True only if this answer actually came from a live online AI call. */
    val fromOnlineAI: Boolean
)

data class Recommendation(
    val title: String,
    val reason: String,
    val source: String // e.g. "offline-rules" or "online-ai"
)

data class ItineraryChangeSuggestion(
    val summary: String,
    val reason: String
)
