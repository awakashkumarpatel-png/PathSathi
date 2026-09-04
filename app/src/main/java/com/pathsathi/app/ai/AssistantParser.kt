package com.pathsathi.app.ai

object AssistantParser {

    fun parseIntent(text: String): AssistantIntentType {
        val t = text.lowercase().trim()
        val mentionsTrip = Regex("trip|\u092F\u093E\u0924\u094D\u0930\u093E").containsMatchIn(t)

        return when {
            Regex("\\bsos\\b|emergency|bachao|jaan khatre|life threat").containsMatchIn(t) ->
                AssistantIntentType.SOS

            Regex("delete|hatao|hata do|remove kar|mita").containsMatchIn(t) && mentionsTrip ->
                AssistantIntentType.DELETE_TRIP

            (Regex("add.*(viewpoint|stop|place|jagah)").containsMatchIn(t) ||
                Regex("(viewpoint|stop|place|jagah).*add").containsMatchIn(t)) ->
                AssistantIntentType.ADD_STOP

            Regex("edit|change kar|update kar|badal").containsMatchIn(t) && mentionsTrip ->
                AssistantIntentType.EDIT_TRIP

            Regex("nearby|paas mein|najdeek|hospital|police station|pharmacy|rescue point").containsMatchIn(t) ->
                AssistantIntentType.NEARBY_HELP

            Regex("weather|mausam|barish|temperature|forecast").containsMatchIn(t) ->
                AssistantIntentType.SHOW_WEATHER

            Regex("mera location|current location|kaha[an]? hoon|where am i|my location").containsMatchIn(t) ->
                AssistantIntentType.MY_LOCATION

            Regex("tracking (start|shuru)|start track|track karo|trek shuru").containsMatchIn(t) ->
                AssistantIntentType.START_TRACKING

            Regex("viewpoint|attraction|places to visit|dekhne (ki|layak)|ghumne").containsMatchIn(t) ->
                AssistantIntentType.SHOW_VIEWPOINTS

            Regex("my trips|mere trips?|trips? dikhao|saved trips?").containsMatchIn(t) ->
                AssistantIntentType.SHOW_MY_TRIPS

            Regex("view|dikhao|show|dekh").containsMatchIn(t) && mentionsTrip ->
                AssistantIntentType.VIEW_TRIP

            Regex("navigate|route|way to|kaise jau|kaise jaayein|map khol").containsMatchIn(t) ->
                AssistantIntentType.NAVIGATE

            Regex("trip bana|trip plan|banao|create trip|plan kar|trip chahiye").containsMatchIn(t) ->
                AssistantIntentType.CREATE_TRIP

            Regex("what can you do|kya kar sakte|kaise kaam karta|how does this work|commands|^help$").containsMatchIn(t) ->
                AssistantIntentType.HELP

            Regex("^(hi|hello|hey|namaste|namaskar|salaam)\\b").containsMatchIn(t) && t.split(" ").size <= 3 ->
                AssistantIntentType.GREETING

            else -> AssistantIntentType.UNKNOWN
        }
    }

    fun extractDurationDays(text: String): Int? {
        val m = Regex("(\\d+)\\s*(din|days?|d\\b)").find(text.lowercase())
        return m?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractMembers(text: String): Int? {
        val m = Regex("(\\d+)\\s*(log|logo|logon|members?|people|travell?ers?)").find(text.lowercase())
        return m?.groupValues?.get(1)?.toIntOrNull()
    }

    fun extractBudget(text: String): Double? {
        val m = Regex("(?:\u20b9|rs\\.?|rupees?)\\s*([\\d,]+)|budget\\s*(?:is|hai|:)?\\s*([\\d,]+)")
            .find(text.lowercase())
        val raw = m?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: m?.groupValues?.getOrNull(2)?.takeIf { it.isNotBlank() }
        return raw?.replace(",", "")?.toDoubleOrNull()
    }

    /** Best-effort trip-name extraction, e.g. "trip ka naam Summer Escape rakho" / "call it Summer Escape". */
    fun extractTripName(text: String): String? {
        val m = Regex("(?:trip ka naam|naam|name it|call it)\\s+([A-Za-z\\u0900-\\u097F0-9 ]+?)(?:\\s+rakho|$)", RegexOption.IGNORE_CASE)
            .find(text)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** Maps free text to one of the Create Trip screen's "Travel With" options
     *  (kept in sync with CreateTripScreen's travelWithOptions: Solo, Family, Friends, Partner, Group). */
    fun extractTravelWith(text: String): String? {
        val t = text.lowercase()
        return when {
            Regex("\\bsolo\\b|akela|akele|अकेला").containsMatchIn(t) -> "Solo"
            Regex("partner|spouse|girlfriend|boyfriend|husband|wife|patni|pati").containsMatchIn(t) -> "Partner"
            Regex("family|parivar|परिवार").containsMatchIn(t) -> "Family"
            Regex("friends?|dost|दोस्त").containsMatchIn(t) -> "Friends"
            Regex("group|टीम|team").containsMatchIn(t) -> "Group"
            else -> null
        }
    }

    /** Best-effort stay-details extraction, e.g. "stay hotel Mountain View mein" / "homestay mein rukna hai". */
    fun extractStayDetails(text: String): String? {
        val m = Regex("(?:stay(?:ing)? (?:at|in)|rukna hai|thehrenge)\\s+([A-Za-z\\u0900-\\u097F0-9 ]+)", RegexOption.IGNORE_CASE)
            .find(text)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** Best-effort free-text note extraction, e.g. "note likho carry extra water" / "notes: bring rain gear". */
    fun extractNotes(text: String): String? {
        val m = Regex("(?:notes?|yaad rakhna)\\s*[:\\-]?\\s+(.+)", RegexOption.IGNORE_CASE).find(text)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** Best-effort raw start-date text, e.g. "15 October se" / "starting from 15 Oct" - stored as free text,
     *  the user can fine-tune the exact date/time later in Create Trip if this doesn't parse into one. */
    fun extractStartDateText(text: String): String? {
        val m = Regex("(?:starting from|start date|shuru|se shuru|se)\\s+([0-9]{1,2}\\s*[A-Za-z\\u0900-\\u097F]*\\s*[0-9]{0,4})", RegexOption.IGNORE_CASE)
            .find(text)
        return m?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    /** Extracts the place name for a weather/nearby-help/viewpoint-info query about a specific place,
     *  e.g. "Manali ka mausam batao" or "Rishikesh ke baare mein batao". */
    fun extractPlaceQuery(text: String, knownPlaces: List<String>): String? {
        knownPlaces.forEach { place ->
            if (place.isNotBlank() && Regex("\\b${Regex.escape(place)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return place
            }
        }
        Regex("([A-Za-z\\u0900-\\u097F]+)\\s+(?:ka mausam|ke baare|mausam|weather|about)", RegexOption.IGNORE_CASE)
            .find(text)?.let { return it.groupValues[1] }
        return null
    }

    /**
     * Best-effort destination extraction. Tries known trek names/locations first
     * (so it lines up with existing Path Sathi trek data when possible), then
     * falls back to the word immediately before "trip"/"jaana" — enough to
     * handle inputs like "3 din ka Rajgir trip banao".
     */
    fun extractDestination(text: String, knownPlaces: List<String>): String? {
        knownPlaces.forEach { place ->
            if (place.isNotBlank() && Regex("\\b${Regex.escape(place)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                return place
            }
        }
        Regex("([A-Za-z\\u0900-\\u097F]+)\\s+(trip|jaana|jana|ghumne)", RegexOption.IGNORE_CASE)
            .find(text)?.let { return it.groupValues[1] }
        Regex("(?:trip to|trip for)\\s+([A-Za-z\\u0900-\\u097F]+)", RegexOption.IGNORE_CASE)
            .find(text)?.let { return it.groupValues[1] }
        return null
    }

    /** Extracts a trip name mentioned in a command like "Goa trip delete karo" or "Manali trip mein ...". */
    fun extractMentionedTripKeyword(text: String, knownTripNames: List<String>): String? {
        knownTripNames.forEach { name ->
            if (name.isNotBlank() && text.contains(name, ignoreCase = true)) return name
        }
        return extractDestination(text, emptyList())
    }

    /** Extracts the place name for an "add stop/viewpoint" command, e.g. "Rajgir trip mein Ghora Katora add karo". */
    fun extractStopName(text: String): String? {
        val m = Regex("(?:mein|me|to)\\s+([A-Za-z\\u0900-\\u097F ]+?)\\s+add", RegexOption.IGNORE_CASE).find(text)
        if (m != null) return m.groupValues[1].trim()
        val m2 = Regex("add\\s+([A-Za-z\\u0900-\\u097F ]+?)(?:\\s+(?:in|to|mein|me))", RegexOption.IGNORE_CASE).find(text)
        return m2?.groupValues?.get(1)?.trim()
    }

    /** True if [text] reads as a plain affirmative reply ("yes", "haan", "ok", "confirm", ...). */
    fun isAffirmative(text: String): Boolean {
        val t = text.trim().lowercase()
        return Regex("^(yes|yeah|yep|ok(ay)?|sure|haan|ha|bilkul|confirm|kar do|theek hai|thik hai)\\.?$").matches(t)
    }

    /** True if [text] reads as a plain negative reply ("no", "nahi", "cancel", ...). */
    fun isNegative(text: String): Boolean {
        val t = text.trim().lowercase()
        return Regex("^(no|nope|nah|nahi|nahin|cancel|mat karo|rehne do)\\.?$").matches(t)
    }

    /** True if [text] reads as the user opting to skip an optional slot ("skip", "chhodo", "bas", "next"). */
    fun isSkip(text: String): Boolean {
        val t = text.trim().lowercase()
        return Regex("^(skip|chhodo|chodo|bas|next|nothing|kuch nahi|no thanks|done|khatam)\\.?$").matches(t)
    }
}
