package com.pathsathi.app.data.local

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the user's stored language preference by wrapping the base
 * Context with a Configuration for that Locale. Called from
 * MainActivity.attachBaseContext so newly-added screens built with
 * stringResource() (Onboarding, Profile, Settings) render in the chosen
 * language. Works on API 24+ without needing the AppCompat library.
 */
object LocaleHelper {

    fun wrap(context: Context): Context {
        val languageCode = AppPreferences.languageCodeBlocking(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
