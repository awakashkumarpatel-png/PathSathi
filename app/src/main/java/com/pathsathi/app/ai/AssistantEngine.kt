package com.pathsathi.app.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.PathSathiDatabase
import com.pathsathi.app.data.local.TripEntity
import com.pathsathi.app.data.local.toEntity
import com.pathsathi.app.data.local.toTrip
import com.pathsathi.app.data.model.Trip
import com.pathsathi.app.data.network.NetworkModeManager
import com.pathsathi.app.data.network.PlaceInfoRepository
import com.pathsathi.app.data.network.WeatherRepository
import com.pathsathi.app.data.repository.TrekRepository
import com.pathsathi.app.location.LocationHelper
import com.pathsathi.app.navigation.Routes
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Converts parsed user commands (typed or spoken) into real actions against
 * Path Sathi's existing Trip database, tracking service, and navigation
 * graph. Holds conversation state (pending intent, collected slots, pending
 * confirmation) across turns so it can carry on a multi-turn conversation
 * that fills every Create Trip field, not just reply with text.
 *
 * Every destructive or safety-relevant action (delete, SOS) always goes
 * through [pendingConfirmAction] and requires an explicit yes - this is
 * true regardless of whether the intent was recognized by the local
 * pattern-based parser or by the optional online AI classifier, since both
 * paths funnel into the same handling below.
 *
 * Not persisted across process death by design — conversation is a live
 * session, same as any chat assistant; all *results* (trips, edits,
 * deletes) go through the same TripDao used by Create Trip / My Trips,
 * so they persist exactly like manually-created trips.
 */
class AssistantEngine(
    private val context: Context,
    private val onNavigate: (String) -> Unit
) {
    private val tripDao = PathSathiDatabase.getInstance(context).tripDao()
    private val knownTreks = TrekRepository.treks

    private var pendingIntent: AssistantIntentType? = null
    private var slots = AssistantSlots()
    private var awaitingSlot: String? = null
    private var pendingConfirmAction: (suspend () -> List<ChatMessage>)? = null

    private fun reply(lang: AssistantLanguage, text: String, quickReplies: List<String> = emptyList()) =
        ChatMessage(fromUser = false, text = text, quickReplies = quickReplies)

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun handleUserMessage(text: String, lang: AssistantLanguage): List<ChatMessage> {
        return try {
            handleUserMessageInternal(text, lang)
        } catch (e: Exception) {
            // Last-resort safety net: a bug anywhere in intent handling should
            // never crash the assistant screen or leave the user stuck.
            listOf(reply(lang, AssistantStrings.genericError(lang)))
        }
    }

    private suspend fun handleUserMessageInternal(text: String, lang: AssistantLanguage): List<ChatMessage> {
        // 1) Waiting on a yes/no confirmation for a pending (possibly destructive) action
        pendingConfirmAction?.let { action ->
            return when {
                AssistantParser.isAffirmative(text) -> {
                    pendingConfirmAction = null
                    action()
                }
                AssistantParser.isNegative(text) -> {
                    pendingConfirmAction = null
                    pendingIntent = null
                    slots = AssistantSlots()
                    listOf(reply(lang, AssistantStrings.cancelled(lang)))
                }
                else -> listOf(reply(lang, AssistantStrings.pleaseConfirm(lang), AssistantStrings.yesNoReplies(lang)))
            }
        }

        // 2) Waiting on an answer to fill a specific slot
        awaitingSlot?.let { slot ->
            if (slot == "stops") return handleStopsSlotInput(text, lang)
            fillSlot(slot, text)
            awaitingSlot = null
            return continueFlow(lang)
        }

        // 3) Fresh command — parse intent locally first (works fully offline, zero network cost)
        var intent = AssistantParser.parseIntent(text)

        // 4) If local parsing couldn't classify it, and the user has opted into Online AI
        // with their own API key, ask it to help understand the free-form text. Its output
        // only feeds slot values into the SAME deterministic flow below - it never executes
        // an action directly, so confirmation gating for delete/SOS is unaffected either way.
        var aiGeneralReply: String? = null
        if (intent == AssistantIntentType.UNKNOWN) {
            val aiSettings = AppPreferences.onlineAiSettings(context).first()
            if (aiSettings.enabled && aiSettings.apiKey.isNotBlank() && NetworkModeManager.isOnlineMode.value) {
                val classification = OnlineAiClient(aiSettings.apiKey, aiSettings.model)
                    .classifyIntent(text, conversationHint = pendingIntent?.name ?: "none")
                if (classification != null) {
                    aiGeneralReply = classification.generalReply
                    intent = mapAiIntent(classification.intent)
                    if (intent == AssistantIntentType.CREATE_TRIP) {
                        return startCreateTripFlow(
                            lang,
                            destination = classification.destination,
                            duration = classification.durationDays,
                            members = classification.members,
                            budget = classification.budget?.toDouble(),
                            tripName = classification.tripName,
                            travelWith = classification.travelWith
                        )
                    }
                } else {
                    // Online AI was enabled+configured but the call itself failed
                    // (network/auth/malformed response) - tell the user plainly, don't
                    // silently swallow it, and don't pretend the message was understood.
                    return listOf(reply(lang, AssistantStrings.onlineAiFailed(lang), canRetryMarker = true))
                }
                if (!aiGeneralReply.isNullOrBlank()) {
                    return listOf(reply(lang, aiGeneralReply))
                }
            }
        }

        return when (intent) {
            AssistantIntentType.GREETING -> listOf(reply(lang, AssistantStrings.greeting(lang)))

            AssistantIntentType.HELP -> listOf(reply(lang, AssistantStrings.helpText(lang)))

            AssistantIntentType.CREATE_TRIP -> startCreateTripFlow(
                lang,
                destination = AssistantParser.extractDestination(text, knownTreks.map { it.name }),
                duration = AssistantParser.extractDurationDays(text),
                members = AssistantParser.extractMembers(text),
                budget = AssistantParser.extractBudget(text),
                tripName = AssistantParser.extractTripName(text),
                travelWith = AssistantParser.extractTravelWith(text),
                stayDetails = AssistantParser.extractStayDetails(text),
                notes = AssistantParser.extractNotes(text),
                startDateText = AssistantParser.extractStartDateText(text)
            )

            AssistantIntentType.SHOW_MY_TRIPS -> {
                onNavigate(Routes.MY_TRIPS)
                listOf(reply(lang, AssistantStrings.openingMyTrips(lang)))
            }

            AssistantIntentType.VIEW_TRIP -> {
                val all = tripDao.getAll().first()
                val match = findTripMatch(text, all)
                if (match != null) {
                    onNavigate(Routes.tripView(match.id))
                    listOf(reply(lang, AssistantStrings.openingTrip(lang, match.tripName)))
                } else {
                    pendingIntent = AssistantIntentType.VIEW_TRIP
                    awaitingSlot = "target_trip_view"
                    listOf(reply(lang, AssistantStrings.askWhichTrip(lang)))
                }
            }

            AssistantIntentType.EDIT_TRIP -> {
                val all = tripDao.getAll().first()
                val match = findTripMatch(text, all)
                if (match != null) {
                    onNavigate(Routes.createTrip(match.id))
                    listOf(reply(lang, AssistantStrings.openingTrip(lang, match.tripName)))
                } else {
                    pendingIntent = AssistantIntentType.EDIT_TRIP
                    awaitingSlot = "target_trip_edit"
                    listOf(reply(lang, AssistantStrings.askWhichTrip(lang)))
                }
            }

            AssistantIntentType.DELETE_TRIP -> {
                val all = tripDao.getAll().first()
                val match = findTripMatch(text, all)
                if (match != null) {
                    setupDeleteConfirmation(match, lang)
                } else {
                    pendingIntent = AssistantIntentType.DELETE_TRIP
                    awaitingSlot = "target_trip_delete"
                    listOf(reply(lang, AssistantStrings.askWhichTrip(lang)))
                }
            }

            AssistantIntentType.ADD_STOP -> {
                val all = tripDao.getAll().first()
                val match = findTripMatch(text, all)
                val stopName = AssistantParser.extractStopName(text)
                if (match == null) {
                    pendingIntent = AssistantIntentType.ADD_STOP
                    slots = AssistantSlots(stopToAdd = stopName)
                    awaitingSlot = "target_trip_addstop"
                    listOf(reply(lang, AssistantStrings.askWhichTrip(lang)))
                } else if (stopName.isNullOrBlank()) {
                    pendingIntent = AssistantIntentType.ADD_STOP
                    slots = AssistantSlots(targetTripId = match.id, targetTripName = match.tripName)
                    awaitingSlot = "stop_name"
                    listOf(reply(lang, AssistantStrings.askStopName(lang)))
                } else {
                    setupAddStopConfirmation(match, stopName, lang)
                }
            }

            AssistantIntentType.SHOW_VIEWPOINTS -> {
                val destination = AssistantParser.extractDestination(text, knownTreks.map { it.name })
                    ?: knownTreks.firstOrNull()?.name
                showViewpoints(destination, lang)
            }

            AssistantIntentType.NAVIGATE -> {
                val destination = AssistantParser.extractDestination(text, knownTreks.map { it.name })
                val trek = destination?.let { d -> knownTreks.find { it.name.equals(d, true) || it.location.contains(d, true) } }
                if (trek != null) {
                    onNavigate(Routes.map(trek.id))
                    listOf(reply(lang, AssistantStrings.openingMap(lang, trek.name)))
                } else {
                    listOf(reply(lang, AssistantStrings.noMapData(lang, destination ?: "")))
                }
            }

            AssistantIntentType.START_TRACKING -> {
                val destination = AssistantParser.extractDestination(text, knownTreks.map { it.name })
                val trek = destination?.let { d -> knownTreks.find { it.name.equals(d, true) || it.location.contains(d, true) } }
                    ?: knownTreks.find { text.contains(it.name, ignoreCase = true) }
                if (trek != null) {
                    onNavigate(Routes.tracking(trek.name))
                    listOf(reply(lang, AssistantStrings.trackingStarted(lang, trek.name)))
                } else {
                    listOf(reply(lang, AssistantStrings.trackingNoMatch(lang)))
                }
            }

            AssistantIntentType.SHOW_WEATHER -> handleWeatherQuery(text, lang)

            AssistantIntentType.NEARBY_HELP -> {
                onNavigate(Routes.NEARBY_HELP)
                listOf(reply(lang, AssistantStrings.nearbyOpening(lang)))
            }

            AssistantIntentType.MY_LOCATION -> handleMyLocation(lang)

            AssistantIntentType.SOS -> {
                pendingConfirmAction = {
                    onNavigate(Routes.SOS)
                    listOf(reply(lang, AssistantStrings.openingSos(lang)))
                }
                listOf(reply(lang, AssistantStrings.confirmSos(lang), AssistantStrings.yesNoReplies(lang)))
            }

            AssistantIntentType.UNKNOWN -> {
                val aiSettings = AppPreferences.onlineAiSettings(context).first()
                if (!aiSettings.enabled) {
                    listOf(reply(lang, AssistantStrings.onlineAiNotConfigured(lang)))
                } else {
                    listOf(reply(lang, AssistantStrings.unknown(lang)))
                }
            }
        }
    }

    private fun reply(lang: AssistantLanguage, text: String, canRetryMarker: Boolean): ChatMessage =
        ChatMessage(fromUser = false, text = text, canRetry = canRetryMarker)

    private fun mapAiIntent(raw: String): AssistantIntentType = when (raw) {
        "create_trip" -> AssistantIntentType.CREATE_TRIP
        "view_trips" -> AssistantIntentType.SHOW_MY_TRIPS
        "delete_trip" -> AssistantIntentType.DELETE_TRIP
        "start_tracking" -> AssistantIntentType.START_TRACKING
        "sos" -> AssistantIntentType.SOS
        "show_weather" -> AssistantIntentType.SHOW_WEATHER
        "nearby_help" -> AssistantIntentType.NEARBY_HELP
        "my_location" -> AssistantIntentType.MY_LOCATION
        "viewpoint_info" -> AssistantIntentType.SHOW_VIEWPOINTS
        else -> AssistantIntentType.UNKNOWN
    }

    private suspend fun startCreateTripFlow(
        lang: AssistantLanguage,
        destination: String?,
        duration: Int?,
        members: Int?,
        budget: Double?,
        tripName: String?,
        travelWith: String?,
        stayDetails: String? = null,
        notes: String? = null,
        startDateText: String? = null
    ): List<ChatMessage> {
        pendingIntent = AssistantIntentType.CREATE_TRIP
        slots = AssistantSlots(
            destination = destination,
            durationDays = duration,
            members = members,
            budget = budget,
            budgetAsked = budget != null,
            tripName = tripName,
            tripNameAsked = tripName != null,
            travelWith = travelWith,
            travelWithAsked = travelWith != null,
            stayDetails = stayDetails,
            stayAsked = stayDetails != null,
            notes = notes,
            notesAsked = notes != null,
            startDateText = startDateText,
            startDateAsked = startDateText != null
        )
        return continueCreateTripFlow(lang)
    }

    private suspend fun handleWeatherQuery(text: String, lang: AssistantLanguage): List<ChatMessage> {
        if (!NetworkModeManager.isOnlineMode.value) {
            return listOf(reply(lang, AssistantStrings.offlineNotice(lang)))
        }
        val place = AssistantParser.extractPlaceQuery(text, knownTreks.map { it.name })
        val trek = place?.let { p -> knownTreks.find { it.name.equals(p, true) } }

        val (lat, lon) = when {
            trek != null -> Pair(trek.latitude, trek.longitude)
            hasLocationPermission() -> {
                val loc = LocationHelper(context).getCurrentLocation()
                    ?: return listOf(reply(lang, AssistantStrings.weatherUnavailable(lang), canRetryMarker = true))
                loc
            }
            else -> return listOf(reply(lang, AssistantStrings.locationPermissionNeeded(lang)))
        }

        val result = WeatherRepository().getCurrentWeather(lat, lon)
        return result.fold(
            onSuccess = { weather ->
                listOf(reply(lang, AssistantStrings.weatherReply(lang, place ?: trek?.name ?: "your area", weather.temperatureC, weather.condition)))
            },
            onFailure = { listOf(reply(lang, AssistantStrings.weatherUnavailable(lang), canRetryMarker = true)) }
        )
    }

    private suspend fun handleMyLocation(lang: AssistantLanguage): List<ChatMessage> {
        if (!hasLocationPermission()) {
            return listOf(reply(lang, AssistantStrings.locationPermissionNeeded(lang)))
        }
        val loc = LocationHelper(context).getCurrentLocation()
        return if (loc != null) {
            listOf(reply(lang, AssistantStrings.myLocationReply(lang, loc.first, loc.second)))
        } else {
            listOf(reply(lang, AssistantStrings.locationUnavailable(lang), canRetryMarker = true))
        }
    }

    private suspend fun handleStopsSlotInput(text: String, lang: AssistantLanguage): List<ChatMessage> {
        return if (AssistantParser.isSkip(text)) {
            awaitingSlot = null
            continueCreateTripFlow(lang)
        } else {
            val stop = text.trim()
            if (stop.isNotBlank()) slots.stops.add(stop)
            awaitingSlot = "stops"
            listOf(reply(lang, AssistantStrings.stopAddedToPlan(lang, stop), listOf(AssistantStrings.doneLabel(lang))))
        }
    }

    private fun fillSlot(slot: String, text: String) {
        val skip = AssistantParser.isSkip(text)
        when (slot) {
            "destination" -> slots.destination = text.trim()
            "duration" -> slots.durationDays = AssistantParser.extractDurationDays(text) ?: text.trim().toIntOrNull()
            "members" -> slots.members = AssistantParser.extractMembers(text) ?: text.trim().toIntOrNull()
            "travel_with" -> if (!skip) slots.travelWith = AssistantParser.extractTravelWith(text) ?: text.trim()
            "start_date" -> if (!skip) slots.startDateText = text.trim()
            "budget" -> if (!skip) slots.budget = AssistantParser.extractBudget(text) ?: text.trim().toDoubleOrNull()
            "stay" -> if (!skip) slots.stayDetails = text.trim()
            "notes" -> if (!skip) slots.notes = text.trim()
            "trip_name" -> if (!skip) slots.tripName = text.trim()
            "stop_name" -> slots.stopToAdd = text.trim()
            "target_trip_view", "target_trip_edit", "target_trip_delete", "target_trip_addstop" ->
                slots.targetTripName = text.trim()
        }
    }

    private suspend fun continueFlow(lang: AssistantLanguage): List<ChatMessage> {
        return when (pendingIntent) {
            AssistantIntentType.CREATE_TRIP -> continueCreateTripFlow(lang)

            AssistantIntentType.VIEW_TRIP -> {
                val all = tripDao.getAll().first()
                val match = findByName(slots.targetTripName, all)
                if (match != null) {
                    onNavigate(Routes.tripView(match.id))
                    listOf(reply(lang, AssistantStrings.openingTrip(lang, match.tripName)))
                } else {
                    listOf(reply(lang, AssistantStrings.noTripFound(lang, slots.targetTripName ?: "")))
                }
            }

            AssistantIntentType.EDIT_TRIP -> {
                val all = tripDao.getAll().first()
                val match = findByName(slots.targetTripName, all)
                if (match != null) {
                    onNavigate(Routes.createTrip(match.id))
                    listOf(reply(lang, AssistantStrings.openingTrip(lang, match.tripName)))
                } else {
                    listOf(reply(lang, AssistantStrings.noTripFound(lang, slots.targetTripName ?: "")))
                }
            }

            AssistantIntentType.DELETE_TRIP -> {
                val all = tripDao.getAll().first()
                val match = findByName(slots.targetTripName, all)
                if (match != null) {
                    setupDeleteConfirmation(match, lang)
                } else {
                    listOf(reply(lang, AssistantStrings.noTripFound(lang, slots.targetTripName ?: "")))
                }
            }

            AssistantIntentType.ADD_STOP -> {
                val all = tripDao.getAll().first()
                val match = findByName(slots.targetTripName, all)
                when {
                    match == null -> listOf(reply(lang, AssistantStrings.noTripFound(lang, slots.targetTripName ?: "")))
                    slots.stopToAdd.isNullOrBlank() -> {
                        awaitingSlot = "stop_name"
                        listOf(reply(lang, AssistantStrings.askStopName(lang)))
                    }
                    else -> setupAddStopConfirmation(match, slots.stopToAdd!!, lang)
                }
            }

            else -> listOf(reply(lang, AssistantStrings.unknown(lang)))
        }
    }

    private suspend fun continueCreateTripFlow(lang: AssistantLanguage): List<ChatMessage> {
        return when {
            slots.destination.isNullOrBlank() -> {
                awaitingSlot = "destination"
                listOf(reply(lang, AssistantStrings.askDestination(lang)))
            }
            slots.durationDays == null -> {
                awaitingSlot = "duration"
                listOf(reply(lang, AssistantStrings.askDuration(lang)))
            }
            slots.members == null -> {
                awaitingSlot = "members"
                listOf(reply(lang, AssistantStrings.askMembers(lang)))
            }
            !slots.travelWithAsked -> {
                slots.travelWithAsked = true
                awaitingSlot = "travel_with"
                listOf(reply(lang, AssistantStrings.askTravelWith(lang), listOf("Solo", "Family", "Friends", "Partner", "Group", AssistantStrings.skipLabel(lang))))
            }
            !slots.startDateAsked -> {
                slots.startDateAsked = true
                awaitingSlot = "start_date"
                listOf(reply(lang, AssistantStrings.askStartDate(lang), listOf(AssistantStrings.skipLabel(lang))))
            }
            !slots.budgetAsked -> {
                slots.budgetAsked = true
                awaitingSlot = "budget"
                listOf(reply(lang, AssistantStrings.askBudget(lang), listOf(AssistantStrings.skipLabel(lang))))
            }
            !slots.stayAsked -> {
                slots.stayAsked = true
                awaitingSlot = "stay"
                listOf(reply(lang, AssistantStrings.askStay(lang), listOf(AssistantStrings.skipLabel(lang))))
            }
            !slots.notesAsked -> {
                slots.notesAsked = true
                awaitingSlot = "notes"
                listOf(reply(lang, AssistantStrings.askNotes(lang), listOf(AssistantStrings.skipLabel(lang))))
            }
            !slots.stopsAsked -> {
                slots.stopsAsked = true
                awaitingSlot = "stops"
                listOf(reply(lang, AssistantStrings.askStops(lang), listOf(AssistantStrings.doneLabel(lang))))
            }
            !slots.tripNameAsked -> {
                slots.tripNameAsked = true
                awaitingSlot = "trip_name"
                listOf(reply(lang, AssistantStrings.askTripName(lang), listOf(AssistantStrings.skipLabel(lang))))
            }
            else -> buildTripPreview(lang)
        }
    }

    private suspend fun buildTripPreview(lang: AssistantLanguage): List<ChatMessage> {
        val destination = slots.destination!!
        val start = parseStartDate(slots.startDateText) ?: System.currentTimeMillis()
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, slots.durationDays ?: 1)
        }.timeInMillis
        val memberCount = (slots.members ?: 1).coerceAtLeast(1)
        val travelWith = slots.travelWith?.takeIf { it.isNotBlank() } ?: if (memberCount > 1) "Group" else "Solo"

        val previewTrip = Trip(
            tripName = slots.tripName?.takeIf { it.isNotBlank() } ?: "$destination Trip",
            destination = destination,
            startDateTime = start,
            endDateTime = end,
            travelWith = travelWith,
            members = if (memberCount > 1) List(memberCount - 1) { "Member ${it + 1}" } else emptyList(),
            stops = slots.stops.toList(),
            stay = slots.stayDetails ?: "",
            budget = slots.budget,
            notes = slots.notes ?: ""
        )

        val viewpoints = matchViewpoints(destination)

        pendingConfirmAction = {
            val newId = tripDao.insert(previewTrip.toEntity())
            onNavigate(Routes.tripView(newId))
            val saved = listOf(reply(lang, AssistantStrings.tripSaved(lang, previewTrip.tripName)))
            pendingIntent = null
            slots = AssistantSlots()
            saved
        }

        val messages = mutableListOf(
            ChatMessage(
                fromUser = false,
                text = AssistantStrings.tripPreviewText(lang, previewTrip),
                tripPreview = previewTrip,
                quickReplies = AssistantStrings.yesNoReplies(lang)
            )
        )
        if (viewpoints.isNotEmpty()) {
            messages.add(0, ChatMessage(fromUser = false, text = AssistantStrings.viewpointsIntro(lang, destination), viewpoints = viewpoints))
        }
        return messages
    }

    /** Best-effort parse of the free-text start date the user gave (e.g. "15 October").
     *  Falls back to null (meaning "today") if it can't be parsed - never guesses a
     *  specific date the user didn't imply. The user can always fine-tune the exact
     *  date/time afterwards in Create Trip. */
    private fun parseStartDate(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val formats = listOf("d MMMM", "d MMM", "d MMMM yyyy", "d MMM yyyy")
        for (pattern in formats) {
            try {
                val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.ENGLISH)
                fmt.isLenient = false
                val parsed = fmt.parse(text.trim()) ?: continue
                val cal = Calendar.getInstance()
                val now = Calendar.getInstance()
                cal.time = parsed
                cal.set(Calendar.YEAR, now.get(Calendar.YEAR))
                if (cal.timeInMillis < now.timeInMillis) cal.add(Calendar.YEAR, 1)
                return cal.timeInMillis
            } catch (e: Exception) {
                // try next pattern
            }
        }
        return null
    }

    private fun setupDeleteConfirmation(trip: TripEntity, lang: AssistantLanguage): List<ChatMessage> {
        pendingConfirmAction = {
            tripDao.delete(trip)
            pendingIntent = null
            slots = AssistantSlots()
            listOf(reply(lang, AssistantStrings.tripDeleted(lang, trip.tripName)))
        }
        return listOf(reply(lang, AssistantStrings.confirmDelete(lang, trip.tripName), AssistantStrings.yesNoReplies(lang)))
    }

    private fun setupAddStopConfirmation(trip: TripEntity, stopName: String, lang: AssistantLanguage): List<ChatMessage> {
        pendingConfirmAction = {
            val updated = trip.toTrip().let { it.copy(stops = it.stops + stopName) }
            tripDao.update(updated.toEntity())
            pendingIntent = null
            slots = AssistantSlots()
            listOf(reply(lang, AssistantStrings.stopAdded(lang, stopName, trip.tripName)))
        }
        return listOf(
            reply(
                lang,
                "\"$stopName\" \u2192 \"${trip.tripName}\" \u2014 ${AssistantStrings.pleaseConfirm(lang)}",
                AssistantStrings.yesNoReplies(lang)
            )
        )
    }

    private suspend fun showViewpoints(destination: String?, lang: AssistantLanguage): List<ChatMessage> {
        if (destination.isNullOrBlank()) {
            return listOf(reply(lang, AssistantStrings.askDestination(lang)))
        }
        val viewpoints = matchViewpoints(destination)
        return if (viewpoints.isEmpty()) {
            listOf(reply(lang, AssistantStrings.noViewpoints(lang, destination)))
        } else {
            listOf(ChatMessage(fromUser = false, text = AssistantStrings.viewpointsIntro(lang, destination), viewpoints = viewpoints))
        }
    }

    /**
     * Builds the viewpoint suggestion for a destination. When online, tries to
     * enrich it with a real photo + summary from Wikipedia (PlaceInfoRepository) -
     * never fabricated; if the lookup fails or the app is offline, falls back to
     * the app's own trek description (also real data, never invented) with no image.
     */
    private suspend fun matchViewpoints(destination: String): List<ViewpointSuggestion> {
        val trek = knownTreks.find {
            it.name.contains(destination, ignoreCase = true) || it.location.contains(destination, ignoreCase = true)
        } ?: return emptyList()

        var info = trek.description
        var imageUrl: String? = null
        var sourceUrl: String? = null

        if (NetworkModeManager.isOnlineMode.value) {
            val placeInfo = PlaceInfoRepository().lookup(trek.name).getOrNull()
            if (placeInfo != null) {
                if (placeInfo.extract.isNotBlank()) info = placeInfo.extract
                imageUrl = placeInfo.imageUrl
                sourceUrl = placeInfo.pageUrl
            }
        }

        return listOf(
            ViewpointSuggestion(
                name = trek.name,
                info = info,
                latitude = trek.latitude,
                longitude = trek.longitude,
                roleInTrip = "Main destination",
                imageUrl = imageUrl,
                sourceUrl = sourceUrl
            )
        )
    }

    private fun findTripMatch(text: String, all: List<TripEntity>): TripEntity? {
        val keyword = AssistantParser.extractMentionedTripKeyword(text, all.map { it.tripName })
        return findByName(keyword, all)
    }

    private fun findByName(name: String?, all: List<TripEntity>): TripEntity? {
        if (name.isNullOrBlank()) return null
        return all.find { it.tripName.contains(name, ignoreCase = true) || it.destination.contains(name, ignoreCase = true) }
    }
}
