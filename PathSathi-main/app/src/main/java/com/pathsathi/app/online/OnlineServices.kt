package com.pathsathi.app.online

/**
 * This file holds the "when internet is available" architecture for every
 * optional online capability the master spec calls for beyond maps and AI
 * (which already have their own dedicated packages — see
 * com.pathsathi.app.map and com.pathsathi.app.ai). Each capability follows
 * the same shape on purpose:
 *   - a small result/model type
 *   - an Offline*Fallback that's always safe to call and never touches the
 *     network
 *   - an Online*Provider interface that is intentionally optional (needs a
 *     real service + API key, which stays optional)
 *   - an Orchestrator that only calls the online path when connectivity AND
 *     app settings AND a configured provider all say yes, and falls back to
 *     offline on any failure
 *
 * These optional providers are kept separate from the offline core. Implementing any of the
 * Online*Provider interfaces later — and passing a real instance into the
 * matching Orchestrator — is the only change needed to go live; no UI or
 * ViewModel code elsewhere needs to change.
 */

// ---------- Weather ----------

data class WeatherInfo(val summary: String, val isLive: Boolean)

interface WeatherProvider {
    suspend fun current(destination: String): WeatherInfo
}

class OfflineWeatherFallback : WeatherProvider {
    override suspend fun current(destination: String): WeatherInfo =
        WeatherInfo("Weather needs an internet connection and isn't available offline.", isLive = false)
}

/** Implement with a real weather API + key later; must return gracefully (never throw) when unconfigured. */
interface OnlineWeatherProvider : WeatherProvider {
    fun isLive(): Boolean
}

class WeatherOrchestrator(
    private val isOnline: () -> Boolean,
    private val onlineFeaturesEnabled: () -> Boolean,
    private val offline: OfflineWeatherFallback = OfflineWeatherFallback(),
    private val online: OnlineWeatherProvider? = null
) : WeatherProvider {
    override suspend fun current(destination: String): WeatherInfo {
        if (!onlineFeaturesEnabled() || !isOnline() || online == null || !online.isLive()) return offline.current(destination)
        return try { online.current(destination) } catch (e: Exception) { offline.current(destination) }
    }
}

// ---------- Updated tourist information ----------

data class TouristInfoUpdate(val summary: String, val isLive: Boolean)

interface TouristInfoProvider {
    suspend fun updatesFor(destination: String): TouristInfoUpdate
}

class OfflineTouristInfoFallback : TouristInfoProvider {
    override suspend fun updatesFor(destination: String): TouristInfoUpdate =
        TouristInfoUpdate("Showing saved/offline place information. Connect to the internet for the latest updates.", isLive = false)
}

/** Implement with a real content API later. */
interface OnlineTouristInfoProvider : TouristInfoProvider {
    fun isLive(): Boolean
}

class TouristInfoOrchestrator(
    private val isOnline: () -> Boolean,
    private val onlineFeaturesEnabled: () -> Boolean,
    private val offline: OfflineTouristInfoFallback = OfflineTouristInfoFallback(),
    private val online: OnlineTouristInfoProvider? = null
) : TouristInfoProvider {
    override suspend fun updatesFor(destination: String): TouristInfoUpdate {
        if (!onlineFeaturesEnabled() || !isOnline() || online == null || !online.isLive()) return offline.updatesFor(destination)
        return try { online.updatesFor(destination) } catch (e: Exception) { offline.updatesFor(destination) }
    }
}

// ---------- Transport updates (live schedules/fares vs. the static demo data) ----------

data class TransportUpdate(val summary: String, val isLive: Boolean)

interface TransportUpdateProvider {
    suspend fun updatesFor(destination: String): TransportUpdate
}

class OfflineTransportUpdateFallback : TransportUpdateProvider {
    override suspend fun updatesFor(destination: String): TransportUpdate =
        TransportUpdate("Showing saved/sample transport info. Connect to the internet for live schedules and fares.", isLive = false)
}

/** Implement with a real transit/fare API later. */
interface OnlineTransportUpdateProvider : TransportUpdateProvider {
    fun isLive(): Boolean
}

class TransportUpdateOrchestrator(
    private val isOnline: () -> Boolean,
    private val onlineFeaturesEnabled: () -> Boolean,
    private val offline: OfflineTransportUpdateFallback = OfflineTransportUpdateFallback(),
    private val online: OnlineTransportUpdateProvider? = null
) : TransportUpdateProvider {
    override suspend fun updatesFor(destination: String): TransportUpdate {
        if (!onlineFeaturesEnabled() || !isOnline() || online == null || !online.isLive()) return offline.updatesFor(destination)
        return try { online.updatesFor(destination) } catch (e: Exception) { offline.updatesFor(destination) }
    }
}

// ---------- Cloud sync (backup of trips/budget/memory across devices) ----------

data class SyncResult(val success: Boolean, val message: String)

interface CloudSyncProvider {
    suspend fun syncNow(): SyncResult
    fun isConfigured(): Boolean
}

/** Default: sync is simply unavailable. Local Room data is always the source of truth regardless. */
class NoOpCloudSync : CloudSyncProvider {
    override suspend fun syncNow(): SyncResult = SyncResult(false, "Cloud sync isn't set up. Your data stays saved locally on this device.")
    override fun isConfigured(): Boolean = false
}

/** Implement with a real backend later. Local Room data must remain the source of truth even after this exists. */
interface OnlineCloudSyncProvider : CloudSyncProvider {
    override fun isConfigured(): Boolean
}

class CloudSyncOrchestrator(
    private val isOnline: () -> Boolean,
    private val onlineFeaturesEnabled: () -> Boolean,
    private val offline: NoOpCloudSync = NoOpCloudSync(),
    private val online: OnlineCloudSyncProvider? = null
) : CloudSyncProvider {
    override suspend fun syncNow(): SyncResult {
        if (!onlineFeaturesEnabled() || !isOnline() || online == null || !online.isConfigured()) return offline.syncNow()
        return try { online.syncNow() } catch (e: Exception) { SyncResult(false, "Sync failed; your local data is unaffected.") }
    }
    override fun isConfigured(): Boolean = online?.isConfigured() ?: false
}

// ---------- Online booking (stay/transport/tickets) ----------

data class BookingAvailability(val available: Boolean, val message: String)

interface BookingProvider {
    suspend fun checkAvailability(itemId: String): BookingAvailability
}

class OfflineBookingFallback : BookingProvider {
    override suspend fun checkAvailability(itemId: String): BookingAvailability =
        BookingAvailability(false, "Online booking isn't configured yet. Contact the stay/transport option directly for now.")
}

/** Implement with a real booking partner API + key later. */
interface OnlineBookingProvider : BookingProvider {
    fun isLive(): Boolean
}

class BookingOrchestrator(
    private val isOnline: () -> Boolean,
    private val onlineFeaturesEnabled: () -> Boolean,
    private val offline: OfflineBookingFallback = OfflineBookingFallback(),
    private val online: OnlineBookingProvider? = null
) : BookingProvider {
    override suspend fun checkAvailability(itemId: String): BookingAvailability {
        if (!onlineFeaturesEnabled() || !isOnline() || online == null || !online.isLive()) return offline.checkAvailability(itemId)
        return try { online.checkAvailability(itemId) } catch (e: Exception) { offline.checkAvailability(itemId) }
    }
}
