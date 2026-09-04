package com.pathsathi.app.ai

import com.pathsathi.app.data.model.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AssistantStrings {

    fun greeting(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH ->
            "Hi! I'm Sathi, your Path Sathi assistant. Tell me what you'd like to do — e.g. \"Plan a 3 day trip to Manali for 4 people\"."
        AssistantLanguage.HINDI ->
            "\u0928\u092E\u0938\u094D\u0924\u0947! \u092E\u0948\u0902 \u0938\u093E\u0925\u0940 \u0939\u0942\u0902, \u0906\u092A\u0915\u093E Path Sathi \u0905\u0938\u093F\u0938\u094D\u091F\u0947\u0902\u091F\u0964 \u092C\u0924\u093E\u0907\u090F \u0915\u094D\u092F\u093E \u0915\u0930\u0928\u093E \u0939\u0948 \u2014 \u091C\u0948\u0938\u0947 \"3 \u0926\u093F\u0928 \u0915\u0940 \u092E\u0928\u093E\u0932\u0940 \u091F\u094D\u0930\u093F\u092A \u092C\u0928\u093E\u0913, 4 \u0932\u094B\u0917 \u0939\u0948\u0902\"\u0964"
        AssistantLanguage.HINGLISH ->
            "Namaste! Main Sathi hoon, aapka Path Sathi assistant. Batao kya karna hai — jaise \"3 din ka Manali trip banao, 4 log hain\"."
    }

    fun offlineNotice(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "You're offline, so I can't reach online AI or fetch live info right now — but I can still create/view/edit/delete trips, add stops, and open SOS using what's already on your device."
        AssistantLanguage.HINDI -> "Aap offline hain, isliye online AI ya live jaankari abhi available nahi. Trip banana/dekhna/edit/delete, stop add karna, aur SOS abhi bhi kaam karega."
        AssistantLanguage.HINGLISH -> "Aap offline hain, isliye online AI ya live info abhi nahi milegi. Lekin trip create/view/edit/delete, stop add karna, aur SOS abhi bhi normally kaam karenge."
    }

    fun askDestination(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Sure — where would you like to go?"
        AssistantLanguage.HINDI -> "\u0920\u0940\u0915 \u0939\u0948 \u2014 \u0906\u092A \u0915\u0939\u093E\u0902 \u091C\u093E\u0928\u093E \u091A\u093E\u0939\u0924\u0947 \u0939\u0948\u0902?"
        AssistantLanguage.HINGLISH -> "Theek hai — aap kahan jaana chahte hain?"
    }

    fun askDuration(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "How many days is the trip?"
        AssistantLanguage.HINDI -> "\u091F\u094D\u0930\u093F\u092A \u0915\u093F\u0924\u0928\u0947 \u0926\u093F\u0928 \u0915\u0940 \u0939\u0948?"
        AssistantLanguage.HINGLISH -> "Trip kitne din ki hai?"
    }

    fun askMembers(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "How many people are travelling?"
        AssistantLanguage.HINDI -> "\u0915\u093F\u0924\u0928\u0947 \u0932\u094B\u0917 \u091C\u093E \u0930\u0939\u0947 \u0939\u0948\u0902?"
        AssistantLanguage.HINGLISH -> "Kitne log ja rahe hain?"
    }

    fun askBudget(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "What's the budget for this trip? (or say skip)"
        AssistantLanguage.HINDI -> "\u0907\u0938 trip \u0915\u093E budget \u0915\u094D\u092F\u093E \u0939\u0948? (\u092F\u093E skip \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "Is trip ka budget kitna hai? (ya skip bolo)"
    }

    fun askTravelWith(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Who are you travelling with? (Solo, Family, Friends, Partner, or Group — or say skip)"
        AssistantLanguage.HINDI -> "\u0906\u092A \u0915\u093F\u0938\u0915\u0947 \u0938\u093E\u0925 \u091C\u093E \u0930\u0939\u0947 \u0939\u0948\u0902? (Solo, Family, Friends, Partner, Group — \u092F\u093E skip \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "Kiske saath ja rahe ho? (Solo, Family, Friends, Partner, Group — ya skip bolo)"
    }

    fun askStartDate(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "When does the trip start? (e.g. \"15 October\" — or say skip to use today)"
        AssistantLanguage.HINDI -> "\u091F\u094D\u0930\u093F\u092A \u0915\u092C \u0936\u0941\u0930\u0942 \u0939\u094B\u0917\u0940? (\u091C\u0948\u0938\u0947 \"15 October\" — \u092F\u093E skip \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "Trip kab se shuru hogi? (jaise \"15 October\" — ya aaj ke liye skip bolo)"
    }

    fun askStay(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Any stay details — hotel, homestay, camping? (or say skip)"
        AssistantLanguage.HINDI -> "Stay \u0915\u0940 \u0915\u094B\u0908 details \u2014 hotel, homestay, camping? (\u092F\u093E skip \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "Stay ki koi details — hotel, homestay, camping? (ya skip bolo)"
    }

    fun askNotes(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Any notes to add for this trip? (or say skip)"
        AssistantLanguage.HINDI -> "\u0907\u0938 trip \u0915\u0947 \u0932\u093F\u090F \u0915\u094B\u0908 note? (\u092F\u093E skip \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "Is trip ke liye koi note add karna hai? (ya skip bolo)"
    }

    fun askStops(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Any stops to add along the way? Tell me one at a time, or say done when finished."
        AssistantLanguage.HINDI -> "\u0930\u093E\u0938\u094D\u0924\u0947 \u092E\u0947\u0902 \u0915\u094B\u0908 stops \u091C\u094B\u0921\u093C\u0928\u0947 \u0939\u0948\u0902? \u090F\u0915-\u090F\u0915 \u0915\u0930\u0915\u0947 \u092C\u0924\u093E\u0913, \u0916\u0924\u094D\u092E \u0939\u094B\u0928\u0947 \u092A\u0930 done \u092C\u094B\u0932\u0947\u0902\u0964"
        AssistantLanguage.HINGLISH -> "Raaste mein koi stops add karne hain? Ek-ek karke batao, khatam hone par \"done\" bolo."
    }

    fun stopAddedToPlan(lang: AssistantLanguage, stop: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Added \"$stop\". Any more stops? (say done when finished)"
        AssistantLanguage.HINDI -> "\"$stop\" \u091C\u094B\u0921\u093C \u0926\u093F\u092F\u093E\u0964 \u0914\u0930 \u0915\u094B\u0908 stop? (\u0916\u0924\u094D\u092E \u0939\u094B\u0928\u0947 \u092A\u0930 done \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "\"$stop\" add kar diya. Aur koi stop? (khatam ho to \"done\" bolo)"
    }

    fun askTripName(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "What would you like to name this trip? (or say skip to use a default name)"
        AssistantLanguage.HINDI -> "\u0907\u0938 trip \u0915\u093E \u0928\u093E\u092E \u0915\u094D\u092F\u093E \u0930\u0916\u0947\u0902? (\u092F\u093E skip \u092C\u094B\u0932\u0947\u0902)"
        AssistantLanguage.HINGLISH -> "Is trip ka naam kya rakhein? (ya default naam ke liye skip bolo)"
    }

    fun skipLabel(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.HINDI -> "Skip"
        else -> "Skip"
    }

    fun doneLabel(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.HINDI -> "Done"
        else -> "Done"
    }

    fun askWhichTrip(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Which trip? Please tell me the trip name or destination."
        AssistantLanguage.HINDI -> "\u0915\u094C\u0928 \u0938\u0940 \u091F\u094D\u0930\u093F\u092A? \u0915\u0943\u092A\u092F\u093E \u091F\u094D\u0930\u093F\u092A \u0915\u093E \u0928\u093E\u092E \u092F\u093E destination \u092C\u0924\u093E\u090F\u0902\u0964"
        AssistantLanguage.HINGLISH -> "Kaunsi trip? Trip ka naam ya destination bata dijiye."
    }

    fun askStopName(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Which place would you like to add as a stop?"
        AssistantLanguage.HINDI -> "\u0915\u094C\u0928 \u0938\u0940 \u091C\u0917\u0939 stop \u0915\u0947 \u0930\u0942\u092A \u092E\u0947\u0902 \u091C\u094B\u0921\u093C\u0928\u0940 \u0939\u0948?"
        AssistantLanguage.HINGLISH -> "Kaunsi jagah stop ke roop mein add karni hai?"
    }

    fun noTripFound(lang: AssistantLanguage, query: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I couldn't find a saved trip matching \"$query\". Check My Trips for the exact name?"
        AssistantLanguage.HINDI -> "\"$query\" \u0938\u0947 \u092E\u0947\u0932 \u0916\u093E\u0924\u0940 \u0915\u094B\u0908 saved trip \u0928\u0939\u0940\u0902 \u092E\u093F\u0932\u0940\u0964"
        AssistantLanguage.HINGLISH -> "\"$query\" se milti koi saved trip nahi mili. My Trips mein exact naam check kar lijiye."
    }

    fun tripDeleted(lang: AssistantLanguage, name: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Deleted \"$name\"."
        AssistantLanguage.HINDI -> "\"$name\" delete \u0915\u0930 \u0926\u0940\u0964"
        AssistantLanguage.HINGLISH -> "\"$name\" delete kar diya."
    }

    fun confirmDelete(lang: AssistantLanguage, name: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Are you sure you want to delete \"$name\"? This can't be undone."
        AssistantLanguage.HINDI -> "\u0915\u094D\u092F\u093E \u0906\u092A \u0935\u093E\u0915\u0908 \"$name\" delete \u0915\u0930\u0928\u093E \u091A\u093E\u0939\u0924\u0947 \u0939\u0948\u0902?"
        AssistantLanguage.HINGLISH -> "Pakka \"$name\" delete karni hai? Ye wapas nahi hogi."
    }

    fun confirmSos(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Open the SOS emergency screen now?"
        AssistantLanguage.HINDI -> "\u0915\u094D\u092F\u093E \u0905\u092D\u0940 SOS emergency screen \u0916\u094B\u0932\u0942\u0902?"
        AssistantLanguage.HINGLISH -> "Abhi SOS emergency screen kholu?"
    }

    fun openingSos(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Opening SOS..."
        AssistantLanguage.HINDI -> "SOS \u0916\u094B\u0932 \u0930\u0939\u093E \u0939\u0942\u0902..."
        AssistantLanguage.HINGLISH -> "SOS khol raha hoon..."
    }

    fun yesNoReplies(lang: AssistantLanguage): List<String> = when (lang) {
        AssistantLanguage.HINDI -> listOf("\u0939\u093E\u0902", "\u0928\u0939\u0940\u0902")
        else -> listOf("Yes", "No")
    }

    fun yesLabel(lang: AssistantLanguage): String = yesNoReplies(lang)[0]
    fun noLabel(lang: AssistantLanguage): String = yesNoReplies(lang)[1]

    fun cancelled(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Okay, cancelled."
        AssistantLanguage.HINDI -> "\u0920\u0940\u0915 \u0939\u0948, \u0930\u0926\u094D\u0926 \u0915\u0930 \u0926\u093F\u092F\u093E\u0964"
        AssistantLanguage.HINGLISH -> "Theek hai, cancel kar diya."
    }

    fun pleaseConfirm(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Please reply Yes or No."
        AssistantLanguage.HINDI -> "\u0915\u0943\u092A\u092F\u093E \u0939\u093E\u0902 \u092F\u093E \u0928\u0939\u0940\u0902 \u092E\u0947\u0902 \u091C\u0935\u093E\u092C \u0926\u0947\u0902\u0964"
        AssistantLanguage.HINGLISH -> "Please Yes ya No mein jawab dijiye."
    }

    fun unknown(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I didn't quite get that. Try something like \"Plan a 3 day trip to Manali for 4 people\", \"Show my trips\", \"Weather in Manali\", or \"Delete my Manali trip\"."
        AssistantLanguage.HINDI -> "\u092E\u0941\u091D\u0947 \u0938\u092E\u091D \u0928\u0939\u0940\u0902 \u0906\u092F\u093E\u0964 \"3 \u0926\u093F\u0928 \u0915\u0940 \u092E\u0928\u093E\u0932\u0940 \u091F\u094D\u0930\u093F\u092A \u092C\u0928\u093E\u0913\" \u091C\u0948\u0938\u093E \u0915\u0941\u091B \u0915\u0939\u0947\u0902\u0964"
        AssistantLanguage.HINGLISH -> "Samajh nahi aaya. \"3 din ka Manali trip banao\", \"My trips dikhao\", \"Manali ka weather batao\", ya \"Manali trip delete karo\" jaisa try karo."
    }

    fun helpText(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH ->
            "I can: plan/save a trip, show or edit or delete a trip, add stops, show My Trips, tell you the weather, find nearby help, share your location, start tracking, or open SOS. Just tell me in your own words, or tap the mic."
        AssistantLanguage.HINDI ->
            "\u092E\u0948\u0902 \u092F\u0947 \u0915\u0930 \u0938\u0915\u0924\u093E \u0939\u0942\u0902: trip \u092C\u0928\u093E\u0928\u093E/save \u0915\u0930\u0928\u093E, trip \u0926\u093F\u0916\u093E\u0928\u093E/edit/delete \u0915\u0930\u0928\u093E, stops add \u0915\u0930\u0928\u093E, My Trips, weather, nearby help, location, tracking, ya SOS \u0916\u094B\u0932\u0928\u093E\u0964"
        AssistantLanguage.HINGLISH ->
            "Main ye kar sakta hoon: trip plan/save karna, trip dikhana/edit/delete karna, stops add karna, My Trips dikhana, weather batana, nearby help dhoondna, location share karna, tracking start karna, ya SOS kholna. Bas apne shabdon mein bolo ya mic dabao."
    }

    fun openingMyTrips(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Opening My Trips..."
        AssistantLanguage.HINDI -> "My Trips \u0916\u094B\u0932 \u0930\u0939\u093E \u0939\u0942\u0902..."
        AssistantLanguage.HINGLISH -> "My Trips khol raha hoon..."
    }

    fun openingTrip(lang: AssistantLanguage, name: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Opening \"$name\"..."
        AssistantLanguage.HINDI -> "\"$name\" \u0916\u094B\u0932 \u0930\u0939\u093E \u0939\u0942\u0902..."
        AssistantLanguage.HINGLISH -> "\"$name\" khol raha hoon..."
    }

    fun openingMap(lang: AssistantLanguage, name: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Opening the map for $name..."
        AssistantLanguage.HINDI -> "$name \u0915\u093E map \u0916\u094B\u0932 \u0930\u0939\u093E \u0939\u0942\u0902..."
        AssistantLanguage.HINGLISH -> "$name ka map khol raha hoon..."
    }

    fun noMapData(lang: AssistantLanguage, name: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I don't have trek map data for \"$name\" yet, but your trip and its stops are saved — you can still navigate using your phone's GPS."
        AssistantLanguage.HINDI -> "\"$name\" \u0915\u093E map data \u0905\u092D\u0940 \u0928\u0939\u0940\u0902 \u0939\u0948, \u092A\u0930 \u0906\u092A\u0915\u0940 trip save \u0939\u0948\u0964"
        AssistantLanguage.HINGLISH -> "\"$name\" ka map data abhi available nahi hai, lekin trip save ho chuki hai — GPS se navigate kar sakte ho."
    }

    fun stopAdded(lang: AssistantLanguage, stop: String, tripName: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Added \"$stop\" to \"$tripName\" and saved."
        AssistantLanguage.HINDI -> "\"$stop\" \"$tripName\" \u092E\u0947\u0902 \u091C\u094B\u0921\u093C \u0915\u0930 save \u0915\u0930 \u0926\u093F\u092F\u093E\u0964"
        AssistantLanguage.HINGLISH -> "\"$stop\" \"$tripName\" mein add karke save kar diya."
    }

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun tripPreviewText(lang: AssistantLanguage, trip: Trip): String {
        val dates = "${dateFormat.format(Date(trip.startDateTime))} - ${dateFormat.format(Date(trip.endDateTime))}"
        return when (lang) {
            AssistantLanguage.ENGLISH ->
                "Here's the preview for \"${trip.tripName}\" to ${trip.destination} ($dates, ${trip.members.size.coerceAtLeast(1)} people). Save this trip?"
            AssistantLanguage.HINDI ->
                "\"${trip.tripName}\" (${trip.destination}, $dates) \u0915\u093E preview \u0924\u0948\u092F\u093E\u0930 \u0939\u0948\u0964 \u0938\u0947\u0935 \u0915\u0930\u0942\u0902?"
            AssistantLanguage.HINGLISH ->
                "\"${trip.tripName}\" (${trip.destination}, $dates) ka preview ready hai. Save karu?"
        }
    }

    fun tripSaved(lang: AssistantLanguage, name: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Saved! \"$name\" is now in My Trips."
        AssistantLanguage.HINDI -> "Save \u0939\u094B \u0917\u092F\u093E! \"$name\" My Trips \u092E\u0947\u0902 \u0939\u0948\u0964"
        AssistantLanguage.HINGLISH -> "Save ho gaya! \"$name\" ab My Trips mein hai."
    }

    fun viewpointsIntro(lang: AssistantLanguage, destination: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Here are some places worth visiting around $destination:"
        AssistantLanguage.HINDI -> "$destination \u0915\u0947 \u0906\u0938-\u092A\u093E\u0938 \u0915\u0941\u091B \u0918\u0942\u092E\u0928\u0947 \u0932\u093E\u092F\u0915 \u091C\u0917\u0939\u0947\u0902:"
        AssistantLanguage.HINGLISH -> "$destination ke aas-paas ye jagah dekhne layak hain:"
    }

    fun noViewpoints(lang: AssistantLanguage, destination: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I don't have detailed viewpoint data for $destination yet — but you can add your own stops when creating the trip."
        AssistantLanguage.HINDI -> "$destination \u0915\u0947 \u0932\u093F\u090F \u0905\u092D\u0940 viewpoint data \u0928\u0939\u0940\u0902 \u0939\u0948\u0964"
        AssistantLanguage.HINGLISH -> "$destination ke liye abhi viewpoint data available nahi hai — trip banate waqt aap khud stops add kar sakte hain."
    }

    fun weatherReply(lang: AssistantLanguage, place: String, tempC: Double, condition: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Weather near $place: ${tempC}°C, $condition."
        AssistantLanguage.HINDI -> "$place \u0915\u0947 \u092A\u093E\u0938 \u092E\u094C\u0938\u092E: ${tempC}\u00b0C, $condition\u0964"
        AssistantLanguage.HINGLISH -> "$place ke paas mausam: ${tempC}°C, $condition."
    }

    fun weatherUnavailable(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Couldn't fetch live weather right now — check your internet connection and try again."
        AssistantLanguage.HINDI -> "Abhi live mausam nahi mil saka — internet connection check karke phir try karein."
        AssistantLanguage.HINGLISH -> "Abhi live weather nahi mil saka — internet connection check karke phir try karo."
    }

    fun nearbyOpening(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Opening nearby help — hospitals, police, pharmacy, and rescue points near you..."
        AssistantLanguage.HINDI -> "Nearby Help khol raha hoon — aapke paas ke hospital, police, pharmacy, aur rescue points..."
        AssistantLanguage.HINGLISH -> "Nearby Help khol raha hoon — aapke aas-paas ke hospital, police, pharmacy, rescue points..."
    }

    fun myLocationReply(lang: AssistantLanguage, lat: Double, lon: Double): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Your current location is approximately %.4f, %.4f.".format(lat, lon)
        AssistantLanguage.HINDI -> "\u0906\u092A\u0915\u0940 \u0935\u0930\u094D\u0924\u092E\u093E\u0928 location \u0932\u0917\u092D\u0917 %.4f, %.4f \u0939\u0948\u0964".format(lat, lon)
        AssistantLanguage.HINGLISH -> "Aapki current location lagbhag %.4f, %.4f hai.".format(lat, lon)
    }

    fun locationUnavailable(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I couldn't get your location. Make sure location permission is granted and GPS is on."
        AssistantLanguage.HINDI -> "Location \u0928\u0939\u0940\u0902 \u092E\u093F\u0932 \u0938\u0915\u0940\u0964 Location permission aur GPS on check karein."
        AssistantLanguage.HINGLISH -> "Location nahi mil saki. Location permission diya hai aur GPS on hai, ye check kar lo."
    }

    fun locationPermissionNeeded(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I need location permission for this. Please allow it from the prompt, or from Settings."
        AssistantLanguage.HINDI -> "Iske liye location permission chahiye. Prompt se ya Settings se allow karein."
        AssistantLanguage.HINGLISH -> "Iske liye location permission chahiye. Prompt se ya Settings se allow kar do."
    }

    fun micPermissionNeeded(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I need microphone permission for voice input. Please allow it."
        AssistantLanguage.HINDI -> "Voice input ke liye microphone permission chahiye. Kripya allow karein."
        AssistantLanguage.HINGLISH -> "Voice input ke liye mic permission chahiye. Allow kar do."
    }

    fun voiceUnavailable(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Voice input isn't available on this device. You can type instead."
        AssistantLanguage.HINDI -> "Is device par voice input available nahi hai. Type karke bhi bata sakte hain."
        AssistantLanguage.HINGLISH -> "Is device par voice input available nahi hai. Type karke bata sakte ho."
    }

    fun trackingStarted(lang: AssistantLanguage, trekName: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Opening tracking for $trekName — tap Start when you're ready."
        AssistantLanguage.HINDI -> "$trekName \u0915\u0940 tracking \u0916\u094B\u0932 \u0930\u0939\u093E \u0939\u0942\u0902 \u2014 \u0924\u0948\u092F\u093E\u0930 \u0939\u094B\u0928\u0947 \u092A\u0930 Start \u0926\u092C\u093E\u090F\u0902\u0964"
        AssistantLanguage.HINGLISH -> "$trekName ki tracking khol raha hoon — tayaar hote hi Start dabana."
    }

    fun trackingNoMatch(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Which trek would you like to start tracking? Please name a saved trek."
        AssistantLanguage.HINDI -> "\u0915\u094C\u0928 \u0938\u0940 trek \u0915\u0947 \u0932\u093F\u090F tracking \u0936\u0941\u0930\u0942 \u0915\u0930\u0947\u0902? \u0915\u094B\u0908 saved trek \u0915\u093E \u0928\u093E\u092E \u092C\u0924\u093E\u090F\u0902\u0964"
        AssistantLanguage.HINGLISH -> "Kis trek ke liye tracking start karein? Ek saved trek ka naam bata dijiye."
    }

    fun onlineAiNotConfigured(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "I didn't understand that with my built-in commands. You can enable Online AI in Settings for more open-ended help, or try rephrasing."
        AssistantLanguage.HINDI -> "Built-in commands se ye samajh nahi aaya. Settings mein Online AI on karke aur madad le sakte hain, ya dobara phrase karke try karein."
        AssistantLanguage.HINGLISH -> "Built-in commands se samajh nahi aaya. Settings mein Online AI on karke aur khula-khula pooch sakte ho, ya alag tarike se try karo."
    }

    fun onlineAiFailed(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Online AI didn't respond — check your API key in Settings and your internet, then try again."
        AssistantLanguage.HINDI -> "Online AI se jawab nahi mila — Settings mein API key aur internet check karke phir try karein."
        AssistantLanguage.HINGLISH -> "Online AI se response nahi mila — Settings mein API key aur internet check karke phir try karo."
    }

    fun genericError(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Something went wrong on my end. Please try again."
        AssistantLanguage.HINDI -> "Kuch gadbad ho gayi. Kripya dobara try karein."
        AssistantLanguage.HINGLISH -> "Kuch gadbad ho gayi. Dobara try karo."
    }

    fun retryLabel(lang: AssistantLanguage): String = when (lang) {
        AssistantLanguage.HINDI -> "Dobara try karein"
        else -> "Retry"
    }

    fun placeInfoIntro(lang: AssistantLanguage, place: String): String = when (lang) {
        AssistantLanguage.ENGLISH -> "Here's what I found about $place:"
        AssistantLanguage.HINDI -> "$place \u0915\u0947 \u092C\u093E\u0930\u0947 \u092E\u0947\u0902 \u092F\u0947 \u092E\u093F\u0932\u093E:"
        AssistantLanguage.HINGLISH -> "$place ke baare mein ye mila:"
    }
}
