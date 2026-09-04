package com.pathsathi.app.ai

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class AssistantLanguage(val code: String, val displayName: String, val locale: Locale) {
    HINGLISH("hg", "Hinglish", Locale.ENGLISH),
    ENGLISH("en", "English", Locale.ENGLISH),
    HINDI("hi", "\u0939\u093F\u0928\u094D\u0926\u0940", Locale("hi"))
}

/**
 * Persisted language choice for the AI Assistant's own responses (text + voice).
 * Independent of the rest of the app's UI, which stays in its existing language.
 */
object AssistantLanguageManager {
    private const val PREFS_NAME = "pathsathi_settings"
    private const val KEY_LANGUAGE = "assistant_language"

    private var prefs: SharedPreferences? = null
    private val _language = MutableStateFlow(AssistantLanguage.HINGLISH)
    val language: StateFlow<AssistantLanguage> = _language

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        val savedCode = p.getString(KEY_LANGUAGE, AssistantLanguage.HINGLISH.code)
        _language.value = AssistantLanguage.values().find { it.code == savedCode } ?: AssistantLanguage.HINGLISH
    }

    fun setLanguage(lang: AssistantLanguage) {
        _language.value = lang
        prefs?.edit()?.putString(KEY_LANGUAGE, lang.code)?.apply()
    }
}
