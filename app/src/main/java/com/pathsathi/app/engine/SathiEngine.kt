package com.pathsathi.app.engine

import com.pathsathi.app.data.db.TripEntity

/** Offline travel assistant. Answers useful trip questions without inventing live data. */
object SathiEngine {

    data class ExpenseCommand(val amountInr: Int, val category: String, val note: String)

    /** Parses simple offline voice/text expense commands such as "add expense 500 food lunch" or "खर्च 500 food lunch". */
    fun parseExpenseCommand(userText: String): ExpenseCommand? {
        val raw = userText.trim()
        val lower = raw.lowercase()
        val trigger = listOf("add expense", "expense", "खर्च", "खर्चा", "खर्च जोड़", "खर्च जोड़")
        if (trigger.none { lower.contains(it) || raw.contains(it) }) return null
        val amount = Regex("(?<!\\d)(\\d{1,7})(?!\\d)").find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        val category = when {
            listOf("food", "खाना", "भोजन", "restaurant", "meal").any { lower.contains(it) || raw.contains(it) } -> "Food"
            listOf("stay", "hotel", "room", "होटल", "रहना").any { lower.contains(it) || raw.contains(it) } -> "Stay"
            listOf("travel", "transport", "taxi", "bus", "train", "यात्रा", "ट्रांसपोर्ट").any { lower.contains(it) || raw.contains(it) } -> "Travel"
            listOf("ticket", "tickets", "टिकट").any { lower.contains(it) || raw.contains(it) } -> "Tickets"
            listOf("shopping", "खरीदारी", "सामान").any { lower.contains(it) || raw.contains(it) } -> "Shopping"
            listOf("activity", "activities", "activity", "गतिविधि").any { lower.contains(it) || raw.contains(it) } -> "Activities"
            else -> "Other"
        }
        val note = raw
            .replace(Regex("(?i)add\\s+expense|expense|खर्चा?|खर्च\\s*(जोड़|जोड़)?"), " ")
            .replace(amount.toString(), " ")
            .replace(Regex("(?i)food|stay|hotel|room|travel|transport|taxi|bus|train|ticket|tickets|shopping|activity|activities|other"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', ':')
        return ExpenseCommand(amount, category, note)
    }
    fun reply(userText: String, activeTrip: TripEntity?, spentInr: Int, isHindi: Boolean): String {
        val text = userText.trim().lowercase()
        val next = has(text, "next", "kahan", "कहाँ", "कहां", "destination", "agla", "अगला", "पड़ाव", "stop")
        val budget = has(text, "budget", "बजट", "खर्च", "spent", "spend", "cost", "paisa", "पैसा", "remaining", "बाकी")
        val safety = has(text, "safety", "सुरक्षा", "emergency", "आपातकाल", "help", "sos", "मदद", "112")
        val itinerary = has(text, "itinerary", "plan", "योजना", "schedule", "day", "दिन")
        val tripStatus = has(text, "trip status", "status", "यात्रा कैसी", "यात्रा की स्थिति", "active trip", "मेरी यात्रा")
        val start = has(text, "start trip", "यात्रा शुरू", "शुरू करो", "start")
        val greeting = hasExact(text, "hi", "hello", "namaste", "नमस्ते", "hey", "हेलो")

        if (greeting) return if (isHindi) "नमस्ते! मैं Sathi हूँ। आपकी यात्रा, बजट, अगला पड़ाव, योजना और सुरक्षा में मदद कर सकता हूँ।" else "Hello! I'm Sathi. I can help with your trip, budget, next stop, itinerary and safety."
        if (activeTrip == null) {
            return if (safety) safetyReply(isHindi)
            else if (isHindi) "अभी कोई सक्रिय यात्रा नहीं है। Trip Planner से यात्रा बनाकर शुरू करें।" else "There is no active trip yet. Create a trip in Trip Planner to get started."
        }
        val day = (activeTrip.currentDayIndex + 1).coerceIn(1, activeTrip.days.coerceAtLeast(1))
        return when {
            safety -> safetyReply(isHindi)
            budget -> {
                val remaining = activeTrip.budgetInr - spentInr
                if (isHindi) "आपकी ${activeTrip.destination} यात्रा का कुल बजट ₹${activeTrip.budgetInr} है। अभी ₹$spentInr खर्च हुए हैं और ₹${kotlin.math.max(remaining, 0)} शेष हैं${if (remaining < 0) "। बजट ₹${-remaining} से अधिक हो गया है" else ""}."
                else "Your ${activeTrip.destination} trip budget is ₹${activeTrip.budgetInr}. You have spent ₹$spentInr and ₹${kotlin.math.max(remaining, 0)} remains${if (remaining < 0) "; you are ₹${-remaining} over budget" else ""}."
            }
            next -> if (isHindi) "आप अभी Day $day पर हैं। आपका अगला पड़ाव Live Trip में दिखाया जा रहा है। Map खोलकर उपलब्ध GPS distance और ETA भी देख सकते हैं।" else "You are on Day $day. Your next stop is shown in Live Trip. Open Map to see available GPS distance and ETA."
            itinerary -> if (isHindi) "आपकी ${activeTrip.destination} यात्रा में ${activeTrip.days} दिन हैं। अभी Day $day चल रहा है। पूरी day-wise योजना Trip Planner preview और Live Trip में देखें।" else "Your ${activeTrip.destination} trip has ${activeTrip.days} days. You are currently on Day $day. See the full day-by-day plan in Trip Planner preview and Live Trip."
            tripStatus -> if (isHindi) "${activeTrip.destination} यात्रा अभी ${activeTrip.status.lowercase()} है। Day $day/${activeTrip.days}, बजट ₹${activeTrip.budgetInr}, खर्च ₹$spentInr।" else "Your ${activeTrip.destination} trip is ${activeTrip.status.lowercase()}. Day $day/${activeTrip.days}, budget ₹${activeTrip.budgetInr}, spent ₹$spentInr."
            start -> if (activeTrip.status == "PLANNED") (if (isHindi) "Trip Planner से यात्रा confirm हो चुकी है। Trips में जाकर Start Trip दबाएँ; उसके बाद Sathi Auto Mode GPS guidance शुरू कर सकता है।" else "The trip is planned. Open Trips and tap Start Trip; Sathi Auto Mode can then start GPS guidance.") else (if (isHindi) "यात्रा पहले से शुरू है।" else "The trip is already started.")
            else -> if (isHindi) "मैं आपकी यात्रा के लिए बजट, अगला पड़ाव, itinerary, trip status और safety बता सकता हूँ। उदाहरण: 'मेरा बजट कितना बचा?', 'अगला पड़ाव?', 'आज की योजना?'" else "I can help with budget, next stop, itinerary, trip status and safety. Try: 'How much budget is left?', 'What's next?', or 'Today's plan?'."
        }
    }

    private fun has(text: String, vararg terms: String) = terms.any { text.contains(it) }
    private fun hasExact(text: String, vararg terms: String) = terms.any { text == it || text.startsWith("$it ") }
    private fun safetyReply(hindi: Boolean) = if (hindi) "आपातकाल में भारत का 112 नंबर इस्तेमाल करें। Safety स्क्रीन में trusted contacts भी सेव कर सकते हैं।" else "For an emergency in India, use 112. You can also save trusted contacts in the Safety screen."
}
