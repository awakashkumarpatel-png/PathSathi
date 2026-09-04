package com.pathsathi.app.data.network

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the app's manual Online/Offline mode.
 * This is a deliberate user-controlled switch (e.g. for saving data while
 * trekking) — it is independent of actual device connectivity and of GPS,
 * which keeps working in both modes since it doesn't need internet.
 */
object NetworkModeManager {
    private const val PREFS_NAME = "pathsathi_settings"
    private const val KEY_ONLINE_MODE = "online_mode"

    private var prefs: SharedPreferences? = null
    private val _isOnlineMode = MutableStateFlow(true)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        _isOnlineMode.value = p.getBoolean(KEY_ONLINE_MODE, true)
    }

    fun setOnlineMode(online: Boolean) {
        _isOnlineMode.value = online
        prefs?.edit()?.putBoolean(KEY_ONLINE_MODE, online)?.apply()
    }
}
