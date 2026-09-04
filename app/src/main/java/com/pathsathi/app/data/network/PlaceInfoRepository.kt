package com.pathsathi.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PlaceInfo(
    val title: String,
    val extract: String,
    val imageUrl: String?,
    val pageUrl: String?
)

/**
 * Looks up a real, freely-licensed photo and a short factual summary for a
 * place name using Wikipedia's public REST summary API (no API key). This
 * exists specifically so the AI Assistant's viewpoint/place answers show
 * genuine images and information instead of ever generating or guessing
 * either - if Wikipedia has nothing for the query, this returns a failure
 * and the assistant falls back to the app's own trek data (also real, never
 * invented) or says it couldn't find a photo.
 */
class PlaceInfoRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(placeName: String): Result<PlaceInfo> = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(placeName, "UTF-8")
            val request = Request.Builder()
                .url("https://en.wikipedia.org/api/rest_v1/page/summary/$encoded")
                .header("User-Agent", "PathSathi-Android-App/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("No page found for \"$placeName\""))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(body)
                if (json.has("type") && json.optString("type") == "disambiguation") {
                    return@withContext Result.failure(Exception("\"$placeName\" is ambiguous"))
                }
                val extract = json.optString("extract", "")
                val thumbnail = json.optJSONObject("thumbnail")?.optString("source")
                val original = json.optJSONObject("originalimage")?.optString("source")
                val pageUrl = json.optJSONObject("content_urls")
                    ?.optJSONObject("desktop")
                    ?.optString("page")

                Result.success(
                    PlaceInfo(
                        title = json.optString("title", placeName),
                        extract = extract,
                        imageUrl = original ?: thumbnail,
                        pageUrl = pageUrl
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
