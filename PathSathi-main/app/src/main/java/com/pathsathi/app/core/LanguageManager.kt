package com.pathsathi.app.core
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
object LanguageManager {
 private const val P="pathsathi_language"; private const val K="language"
 fun getLanguage(c:Context)=c.getSharedPreferences(P,Context.MODE_PRIVATE).getString(K,"en")?:"en"
 fun setLanguage(c:Context,v:String){c.getSharedPreferences(P,Context.MODE_PRIVATE).edit().putString(K,v).apply()}
 fun wrap(c:Context):Context{val l=Locale(getLanguage(c));Locale.setDefault(l);val cfg=Configuration(c.resources.configuration);if(Build.VERSION.SDK_INT>=24)cfg.setLocale(l)else @Suppress("DEPRECATION") run{cfg.locale=l};return c.createConfigurationContext(cfg)}
}
