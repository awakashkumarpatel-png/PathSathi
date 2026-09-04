package com.pathsathi.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "pathsathi_settings")

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class UnitSystem { METRIC, IMPERIAL }
enum class GpsAccuracyMode { HIGH, BALANCED, BATTERY_SAVER }
enum class AppLanguage(val code: String) { ENGLISH("en"), HINDI("hi") }

/**
 * Single source of truth for every user-configurable setting in the app.
 * Backed by Jetpack DataStore so values survive process death and are
 * observed reactively wherever they're needed (Settings, Home, Theme, etc).
 */
object AppPreferences {

    // Onboarding
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    private val KEY_LEGAL_ACCEPTED = booleanPreferencesKey("legal_accepted")

    // Profile
    private val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")
    private val KEY_PROFILE_PHONE = stringPreferencesKey("profile_phone")
    private val KEY_PROFILE_BLOOD_GROUP = stringPreferencesKey("profile_blood_group")
    private val KEY_PROFILE_NOTE = stringPreferencesKey("profile_emergency_note")

    // App-wide
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_UNITS = stringPreferencesKey("units")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

    // GPS / battery
    private val KEY_GPS_MODE = stringPreferencesKey("gps_mode")
    private val KEY_BATTERY_SAVER = booleanPreferencesKey("battery_saver")

    // Emergency
    private val KEY_SOS_COUNTDOWN_SEC = intPreferencesKey("sos_countdown_sec")

    // Safety check-in
    private val KEY_CHECKIN_ENABLED = booleanPreferencesKey("checkin_enabled")
    private val KEY_CHECKIN_INTERVAL_MIN = intPreferencesKey("checkin_interval_min")
    private val KEY_CHECKIN_GRACE_MIN = intPreferencesKey("checkin_grace_min")

    // AI Assistant - optional online mode (bring-your-own API key, never hardcoded)
    private val KEY_AI_ONLINE_ENABLED = booleanPreferencesKey("ai_online_enabled")
    private val KEY_AI_API_KEY = stringPreferencesKey("ai_api_key")
    private val KEY_AI_MODEL = stringPreferencesKey("ai_model")

    fun isOnboardingComplete(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingComplete(context: Context, done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    fun isLegalAccepted(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_LEGAL_ACCEPTED] ?: false }

    suspend fun setLegalAccepted(context: Context, accepted: Boolean) {
        context.dataStore.edit { it[KEY_LEGAL_ACCEPTED] = accepted }
    }

    /** One-shot blocking read, safe only for very small startup checks (e.g. attachBaseContext). */
    fun languageCodeBlocking(context: Context): String = runBlocking {
        context.dataStore.data.map { it[KEY_LANGUAGE] ?: AppLanguage.ENGLISH.code }.first()
    }

    /** One-shot blocking read used when starting a GPS tracking session (TrackingService). */
    fun gpsModeBlocking(context: Context): GpsAccuracyMode = runBlocking { gpsMode(context).first() }

    /** One-shot blocking read used when starting a GPS tracking session (TrackingService). */
    fun batterySaverBlocking(context: Context): Boolean = runBlocking { batterySaver(context).first() }

    data class ProfileData(
        val name: String = "",
        val phone: String = "",
        val bloodGroup: String = "",
        val emergencyNote: String = ""
    )

    fun profile(context: Context): Flow<ProfileData> = context.dataStore.data.map {
        ProfileData(
            name = it[KEY_PROFILE_NAME] ?: "",
            phone = it[KEY_PROFILE_PHONE] ?: "",
            bloodGroup = it[KEY_PROFILE_BLOOD_GROUP] ?: "",
            emergencyNote = it[KEY_PROFILE_NOTE] ?: ""
        )
    }

    suspend fun saveProfile(context: Context, profile: ProfileData) {
        context.dataStore.edit {
            it[KEY_PROFILE_NAME] = profile.name
            it[KEY_PROFILE_PHONE] = profile.phone
            it[KEY_PROFILE_BLOOD_GROUP] = profile.bloodGroup
            it[KEY_PROFILE_NOTE] = profile.emergencyNote
        }
    }

    fun language(context: Context): Flow<AppLanguage> = context.dataStore.data.map {
        if (it[KEY_LANGUAGE] == AppLanguage.HINDI.code) AppLanguage.HINDI else AppLanguage.ENGLISH
    }

    suspend fun setLanguage(context: Context, language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.code }
    }

    fun units(context: Context): Flow<UnitSystem> = context.dataStore.data.map {
        if (it[KEY_UNITS] == "imperial") UnitSystem.IMPERIAL else UnitSystem.METRIC
    }

    suspend fun setUnits(context: Context, units: UnitSystem) {
        context.dataStore.edit { it[KEY_UNITS] = if (units == UnitSystem.IMPERIAL) "imperial" else "metric" }
    }

    fun themeMode(context: Context): Flow<ThemeMode> = context.dataStore.data.map {
        when (it[KEY_THEME_MODE]) {
            "dark" -> ThemeMode.DARK
            "light" -> ThemeMode.LIGHT
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit {
            it[KEY_THEME_MODE] = when (mode) {
                ThemeMode.DARK -> "dark"
                ThemeMode.LIGHT -> "light"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }

    fun notificationsEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    fun gpsMode(context: Context): Flow<GpsAccuracyMode> = context.dataStore.data.map {
        when (it[KEY_GPS_MODE]) {
            "battery_saver" -> GpsAccuracyMode.BATTERY_SAVER
            "balanced" -> GpsAccuracyMode.BALANCED
            else -> GpsAccuracyMode.HIGH
        }
    }

    suspend fun setGpsMode(context: Context, mode: GpsAccuracyMode) {
        context.dataStore.edit {
            it[KEY_GPS_MODE] = when (mode) {
                GpsAccuracyMode.HIGH -> "high"
                GpsAccuracyMode.BALANCED -> "balanced"
                GpsAccuracyMode.BATTERY_SAVER -> "battery_saver"
            }
        }
    }

    fun batterySaver(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_BATTERY_SAVER] ?: false }

    suspend fun setBatterySaver(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[KEY_BATTERY_SAVER] = enabled }
    }

    fun sosCountdownSeconds(context: Context): Flow<Int> =
        context.dataStore.data.map { it[KEY_SOS_COUNTDOWN_SEC] ?: 5 }

    suspend fun setSosCountdownSeconds(context: Context, seconds: Int) {
        context.dataStore.edit { it[KEY_SOS_COUNTDOWN_SEC] = seconds }
    }

    data class CheckInSettings(
        val enabled: Boolean = false,
        val intervalMinutes: Int = 60,
        val graceMinutes: Int = 15
    )

    fun checkInSettings(context: Context): Flow<CheckInSettings> = context.dataStore.data.map {
        CheckInSettings(
            enabled = it[KEY_CHECKIN_ENABLED] ?: false,
            intervalMinutes = it[KEY_CHECKIN_INTERVAL_MIN] ?: 60,
            graceMinutes = it[KEY_CHECKIN_GRACE_MIN] ?: 15
        )
    }

    suspend fun saveCheckInSettings(context: Context, settings: CheckInSettings) {
        context.dataStore.edit {
            it[KEY_CHECKIN_ENABLED] = settings.enabled
            it[KEY_CHECKIN_INTERVAL_MIN] = settings.intervalMinutes
            it[KEY_CHECKIN_GRACE_MIN] = settings.graceMinutes
        }
    }

    data class OnlineAiSettings(
        val enabled: Boolean = false,
        val apiKey: String = "",
        val model: String = "claude-3-5-haiku-latest"
    )

    fun onlineAiSettings(context: Context): Flow<OnlineAiSettings> = context.dataStore.data.map {
        OnlineAiSettings(
            enabled = it[KEY_AI_ONLINE_ENABLED] ?: false,
            apiKey = it[KEY_AI_API_KEY] ?: "",
            model = it[KEY_AI_MODEL]?.takeIf { m -> m.isNotBlank() } ?: "claude-3-5-haiku-latest"
        )
    }

    suspend fun saveOnlineAiSettings(context: Context, settings: OnlineAiSettings) {
        context.dataStore.edit {
            it[KEY_AI_ONLINE_ENABLED] = settings.enabled
            it[KEY_AI_API_KEY] = settings.apiKey
            it[KEY_AI_MODEL] = settings.model
        }
    }
}
