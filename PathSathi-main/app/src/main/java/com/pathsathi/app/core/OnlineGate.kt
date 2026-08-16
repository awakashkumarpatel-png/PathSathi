package com.pathsathi.app.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

/** Live snapshot of the two flags every Online*Orchestrator needs to decide offline-vs-online. */
data class OnlineGate(val isOnline: Boolean, val onlineFeaturesEnabled: Boolean)

@Composable
fun rememberOnlineGate(): OnlineGate {
    val context = LocalContext.current
    val isOnline by ConnectivityObserver.isOnline(context).collectAsState(initial = false)
    val onlineFeaturesEnabled by AppConfig.onlineFeaturesEnabled(context).collectAsState(initial = false)
    return OnlineGate(isOnline = isOnline, onlineFeaturesEnabled = onlineFeaturesEnabled)
}
