package com.pathsathi.app.engine

import com.pathsathi.app.data.db.TripEntity

/**
 * Offline, rule/pattern-based reply engine for Sathi Robot.
 * This intentionally does NOT call any network/AI service — it recognizes
 * simple intents (budget, next destination, safety, greeting) in Hindi/English
 * and answers from local trip data. Phase 12 ("Advanced AI") is meant to sit
 * BEHIND this same interface later as an optional online upgrade; if that
 * service is unavailable, replies must keep falling back to this engine
 * rather than the app breaking or inventing information.
 */
object SathiEngine {

    fun reply(userText: String, activeTrip: TripEntity?, spentInr: Int, isHindi: Boolean): String {
        val text = userText.trim().lowercase()

        val wantsNextDestination = listOf("next", "kahan", "कहाँ", "कहां", "destination", "agla", "अगला").any { text.contains(it) }
        val wantsBudget = listOf("budget", "बजट", "खर्च", "spend", "cost", "paisa", "पैसा").any { text.contains(it) }
        val wantsSafety = listOf("safety", "सुरक्षा", "emergency", "आपातकाल", "help", "sos", "मदद").any { text.contains(it) }
        val greeting = listOf("hi", "hello", "namaste", "नमस्ते", "hey").any { text == it || text.startsWith(it) }

        return when {
            activeTrip == null && (wantsNextDestination || wantsBudget) ->
                if (isHindi) "अभी कोई सक्रिय यात्रा नहीं है। पहले Trip Planner से एक यात्रा बनाएं।"
                else "There's no active trip right now. Start one from the Trip Planner first."

            greeting ->
                if (isHindi) "नमस्ते! मैं यहाँ आपकी यात्रा में मदद के लिए हूं।"
                else "Hello! I'm here to help with your trip."

            wantsNextDestination && activeTrip != null -> {
                val dayIdx = activeTrip.currentDayIndex.coerceAtLeast(0)
                if (isHindi) "आपका अगला पड़ाव Day ${dayIdx + 1} की योजना के अनुसार तय है। Trips टैब में Live Trip खोलकर पूरी जानकारी देखें।"
                else "Your next stop follows the Day ${dayIdx + 1} plan. Open Live Trip under Trips to see full details."
            }

            wantsBudget && activeTrip != null -> {
                val remaining = activeTrip.budgetInr - spentInr
                if (isHindi) "अब तक ₹$spentInr खर्च हुए हैं, कुल बजट ₹${activeTrip.budgetInr} में से ₹$remaining शेष है।"
                else "You've spent ₹$spentInr so far, out of a total budget of ₹${activeTrip.budgetInr}. ₹$remaining remaining."
            }

            wantsSafety ->
                if (isHindi) "सुरक्षा जानकारी के लिए नीचे दिए गए Safety टैब में जाएं। आपातकाल में राष्ट्रीय हेल्पलाइन 112 पर कॉल करें।"
                else "Open the Safety tab below for emergency info. In an emergency, call the national helpline 112."

            else ->
                if (isHindi) "मैं अभी ऑफ़लाइन मोड में सीमित जवाब दे सकता हूं। कृपया अपनी यात्रा, बजट या सुरक्षा से जुड़ा सवाल पूछें।"
                else "I can give limited answers while offline. Try asking about your trip, budget, or safety."
        }
    }
}
