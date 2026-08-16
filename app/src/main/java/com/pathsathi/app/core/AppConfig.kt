package com.pathsathi.app.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.configDataStore by preferencesDataStore(name = "pathsathi_config")

/**
 * Single centralized place for every online/offline, ads, and web-presence
 * setting in the app. Nothing here requires a server or domain — every
 * default is offline-safe, and every online capability stays off until the
 * person explicitly enables it (and a real provider/key is wired in).
 *
 * ⚠️ WEBSITE_URL_PLACEHOLDER is intentionally NOT a real domain. Path Sathi
 * does not have an official domain yet. Replace this one constant with the
 * real URL once it exists — nothing else in the app needs to change, since
 * every screen that wants to show a website/support/privacy-policy link
 * reads it from here.
 */
object AppConfig {

    /**
     * Clearly-marked placeholder. Intentionally invalid as a real URL (the
     * "CONFIGURE-ME" segment) so it can never be mistaken for a live link if
     * this constant is ever surfaced before someone fills in the real one.
     */
    const val WEBSITE_URL_PLACEHOLDER = "https://CONFIGURE-ME.pathsathi.example/"
    const val PRIVACY_POLICY_URL_PLACEHOLDER = "https://CONFIGURE-ME.pathsathi.example/privacy"
    const val SUPPORT_URL_PLACEHOLDER = "https://CONFIGURE-ME.pathsathi.example/support"

    /** True only when WEBSITE_URL_PLACEHOLDER has been replaced with a real URL. Used to hide/disable web-link UI until then. */
    fun isWebsiteConfigured(url: String): Boolean = url.isNotBlank() && !url.contains("CONFIGURE-ME")

    private val KEY_ONLINE_FEATURES_ENABLED = booleanPreferencesKey("online_features_enabled")
    private val KEY_ONLINE_AI_ENABLED = booleanPreferencesKey("online_ai_enabled")
    private val KEY_ADS_ENABLED = booleanPreferencesKey("ads_enabled")
    private val KEY_WEBSITE_URL = stringPreferencesKey("website_url")

    /** Master switch. When false, the app never attempts any network call, regardless of connectivity. */
    fun onlineFeaturesEnabled(context: Context): Flow<Boolean> =
        context.configDataStore.data.map { it[KEY_ONLINE_FEATURES_ENABLED] ?: false }

    suspend fun setOnlineFeaturesEnabled(context: Context, enabled: Boolean) {
        context.configDataStore.edit { it[KEY_ONLINE_FEATURES_ENABLED] = enabled }
    }

    /** Sub-switch for Advanced AI specifically (Phase 12) — still requires onlineFeaturesEnabled AND a configured OnlineAIProvider. */
    fun onlineAiEnabled(context: Context): Flow<Boolean> =
        context.configDataStore.data.map { it[KEY_ONLINE_AI_ENABLED] ?: false }

    suspend fun setOnlineAiEnabled(context: Context, enabled: Boolean) {
        context.configDataStore.edit { it[KEY_ONLINE_AI_ENABLED] = enabled }
    }

    /** Ads only ever load when this is true AND the device is online AND a real ad provider is configured. */
    fun adsEnabled(context: Context): Flow<Boolean> =
        context.configDataStore.data.map { it[KEY_ADS_ENABLED] ?: false }

    suspend fun setAdsEnabled(context: Context, enabled: Boolean) {
        context.configDataStore.edit { it[KEY_ADS_ENABLED] = enabled }
    }

    /** Falls back to the clearly-marked placeholder until a real URL is stored. */
    fun websiteUrl(context: Context): Flow<String> =
        context.configDataStore.data.map { it[KEY_WEBSITE_URL] ?: WEBSITE_URL_PLACEHOLDER }

    suspend fun setWebsiteUrl(context: Context, url: String) {
        context.configDataStore.edit { it[KEY_WEBSITE_URL] = url }
    }
}
