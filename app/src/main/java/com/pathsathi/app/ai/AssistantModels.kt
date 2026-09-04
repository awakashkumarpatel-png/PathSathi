package com.pathsathi.app.ai

import com.pathsathi.app.data.model.Trip

enum class AssistantIntentType {
    CREATE_TRIP,
    VIEW_TRIP,
    EDIT_TRIP,
    DELETE_TRIP,
    ADD_STOP,
    SHOW_VIEWPOINTS,
    SHOW_MY_TRIPS,
    NAVIGATE,
    SOS,
    START_TRACKING,
    SHOW_WEATHER,
    NEARBY_HELP,
    MY_LOCATION,
    GREETING,
    HELP,
    UNKNOWN
}

/** Slots collected across a multi-turn conversation while building/updating a trip.
 *  Covers every field on the Create Trip screen so the assistant can fill the whole
 *  form through conversation, not just a subset of it. */
data class AssistantSlots(
    var tripName: String? = null,
    var destination: String? = null,
    var durationDays: Int? = null,
    var startDateText: String? = null,
    var travelWith: String? = null,
    var members: Int? = null,
    var budget: Double? = null,
    var stayDetails: String? = null,
    var notes: String? = null,
    var stops: MutableList<String> = mutableListOf(),
    var targetTripId: Long? = null,
    var targetTripName: String? = null,
    var stopToAdd: String? = null,
    // Tracks which optional Create Trip fields have already been asked about,
    // so a "skip" reply is remembered instead of re-prompting forever.
    var travelWithAsked: Boolean = false,
    var startDateAsked: Boolean = false,
    var budgetAsked: Boolean = false,
    var stayAsked: Boolean = false,
    var notesAsked: Boolean = false,
    var stopsAsked: Boolean = false,
    var tripNameAsked: Boolean = false
)

data class ViewpointSuggestion(
    val name: String,
    val info: String,
    val latitude: Double,
    val longitude: Double,
    val roleInTrip: String,
    /** Real photo URL from Wikipedia, only ever set when a genuine lookup succeeded - never fabricated. */
    val imageUrl: String? = null,
    val sourceUrl: String? = null
)

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val fromUser: Boolean,
    val text: String,
    val quickReplies: List<String> = emptyList(),
    val viewpoints: List<ViewpointSuggestion> = emptyList(),
    val tripPreview: Trip? = null,
    /** True while a network/online step (weather, place photo, online AI) is in flight. */
    val isLoading: Boolean = false,
    /** True when a step failed and the user can tap to retry the same action. */
    val canRetry: Boolean = false
)
