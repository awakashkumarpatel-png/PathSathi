package com.pathsathi.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to a real hosted LLM (Anthropic Messages API) using an API key the
 * user enters themselves in Settings -> AI Assistant -> Online AI. This is
 * always off by default and never ships with, or falls back to, any
 * embedded key - if the user hasn't configured one, this class is simply
 * never called (AssistantEngine checks `enabled && apiKey.isNotBlank()`
 * before every use).
 *
 * Role in the app: understanding open-ended free text and answering
 * general questions only. It never directly executes an app action - its
 * structured output is fed back into AssistantEngine's own deterministic
 * intent handling, so destructive/state-changing actions (delete, SOS,
 * save trip) still always go through the app's existing confirmation flow
 * regardless of whether the intent came from local parsing or this client.
 */
class OnlineAiClient(private val apiKey: String, private val model: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Asks the model to classify [userText] into one of PathSathi's known
     * intents and extract any slot values it can find, as JSON. Returns
     * null on any failure (network, auth, malformed response) - callers
     * must treat null as "couldn't classify" and fall back gracefully,
     * never crash or block on it.
     */
    suspend fun classifyIntent(userText: String, conversationHint: String): AiClassification? =
        withContext(Dispatchers.IO) {
            val systemPrompt = """
                You are the intent-understanding layer for PathSathi, an offline-first trekking
                safety Android app. Given the user's message, output ONLY a compact JSON object
                (no prose, no markdown fences) with this exact shape:
                {"intent": "<one of: create_trip, view_trips, delete_trip, start_tracking, sos,
                 show_weather, nearby_help, my_location, viewpoint_info, unknown>",
                 "destination": "<place name or empty string>",
                 "duration_days": <integer or null>,
                 "members": <integer or null>,
                 "budget": <integer or null>,
                 "trip_name": "<string or empty>",
                 "travel_with": "<solo/friends/family/group or empty>",
                 "reply": "<a short, friendly, direct answer to the user IF their message was a
                 general question unrelated to any action - otherwise empty string>"}
                Current context: $conversationHint
                Never invent a destination, place fact, or number the user didn't imply.
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", model)
                put("max_tokens", 400)
                put("system", systemPrompt)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userText)
                    })
                })
            }

            try {
                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val responseBody = response.body?.string() ?: return@withContext null
                    val json = JSONObject(responseBody)
                    val contentArray = json.optJSONArray("content") ?: return@withContext null
                    if (contentArray.length() == 0) return@withContext null
                    val text = contentArray.getJSONObject(0).optString("text").trim()

                    // Model may still wrap JSON in fences despite instructions - strip defensively.
                    val cleaned = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val parsed = JSONObject(cleaned)

                    AiClassification(
                        intent = parsed.optString("intent", "unknown"),
                        destination = parsed.optString("destination", "").takeIf { it.isNotBlank() },
                        durationDays = parsed.optInt("duration_days", -1).takeIf { it > 0 },
                        members = parsed.optInt("members", -1).takeIf { it > 0 },
                        budget = parsed.optInt("budget", -1).takeIf { it > 0 },
                        tripName = parsed.optString("trip_name", "").takeIf { it.isNotBlank() },
                        travelWith = parsed.optString("travel_with", "").takeIf { it.isNotBlank() },
                        generalReply = parsed.optString("reply", "").takeIf { it.isNotBlank() }
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
}

data class AiClassification(
    val intent: String,
    val destination: String?,
    val durationDays: Int?,
    val members: Int?,
    val budget: Int?,
    val tripName: String?,
    val travelWith: String?,
    val generalReply: String?
)
