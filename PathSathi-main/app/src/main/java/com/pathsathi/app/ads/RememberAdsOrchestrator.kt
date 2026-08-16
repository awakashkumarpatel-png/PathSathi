package com.pathsathi.app.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.pathsathi.app.core.AppConfig
import com.pathsathi.app.core.ConnectivityObserver

/**
 * Builds an [AdsOrchestrator] bound to live connectivity and the person's
 * ads setting. Safe to call from any screen in [AdSurface] — it always
 * resolves to "no ad" offline, when ads are off, or when no real provider
 * is configured, since no [OnlineAdsProvider] is passed in here.
 */
@Composable
fun rememberAdsOrchestrator(): AdsOrchestrator {
    val context = LocalContext.current
    val isOnline by ConnectivityObserver.isOnline(context).collectAsState(initial = false)
    val adsEnabled by AppConfig.adsEnabled(context).collectAsState(initial = false)
    return remember(isOnline, adsEnabled) {
        AdsOrchestrator(isOnline = { isOnline }, adsEnabledInSettings = { adsEnabled })
    }
}
