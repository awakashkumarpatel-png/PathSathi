package com.pathsathi.app.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The single source of truth for "are we online right now" across the whole
 * app. Every optional online feature (maps, weather, ads, online AI, cloud
 * sync) must check this — or better, go through an Orchestrator that already
 * checks this — before attempting a network call. Offline-core features
 * (saved trips, budget, Sathi's offline replies, saved places, emergency
 * info) never depend on this at all.
 */
object ConnectivityObserver {

    fun isOnline(context: Context): Flow<Boolean> = callbackFlow {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun currentlyOnline(): Boolean {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(currentlyOnline()) }
            override fun onLost(network: Network) { trySend(currentlyOnline()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(currentlyOnline())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        trySend(currentlyOnline())
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: SecurityException) {
            // ACCESS_NETWORK_STATE missing/denied — stay offline rather than crash.
            trySend(false)
        }

        awaitClose {
            try { cm.unregisterNetworkCallback(callback) } catch (e: Exception) { /* already unregistered */ }
        }
    }.distinctUntilChanged()
}
