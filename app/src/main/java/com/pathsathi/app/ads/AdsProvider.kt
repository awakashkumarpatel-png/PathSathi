package com.pathsathi.app.ads

/** What an ad slot in the UI needs to render, or null when no ad should show. */
data class AdContent(
    val headline: String,
    val body: String,
    val providerName: String
)

enum class AdSurface { HOME, EXPLORE, STAY, FOOD, TRANSPORT }

/**
 * Screens where an ad must NEVER appear, regardless of provider/config —
 * Safety/Emergency, Sathi Robot guidance, Map/navigation, and Live Trip
 * Mode. Ad slot composables should check this before rendering, and
 * AdSurface intentionally has no entries for those screens at all.
 */
val AD_EXCLUDED_SURFACES_NOTE =
    "Ads are never placed on Safety/Emergency, Sathi Robot, Map/Navigation, or Live Trip screens."

interface AdsProvider {
    /** Returns ad content to show, or null if no ad is available right now. Never throws. */
    suspend fun loadAd(surface: AdSurface): AdContent?

    /** True only if this provider is actually configured and reachable. */
    fun isAvailable(): Boolean
}

/**
 * Default, always-safe provider: never shows an ad. This is what the app
 * uses offline, before any real ad SDK is configured, and whenever the
 * person has ads turned off in Settings.
 */
class NoOpAdsProvider : AdsProvider {
    override suspend fun loadAd(surface: AdSurface): AdContent? = null
    override fun isAvailable(): Boolean = false
}

/**
 * Integration point for a real ad network (AdMob, etc.). NOT implemented —
 * doing so needs a real ad-unit ID/API key, which must stay optional and is
 * never hard-coded here.
 *
 * To go live later:
 *   1. Add the ad SDK's Gradle dependency.
 *   2. Read the ad unit ID from a non-committed local.properties entry via
 *      BuildConfig — never hard-code it in source.
 *   3. Implement this interface, calling the real SDK's ad-load API and
 *      mapping a successful fill into AdContent.
 *   4. isAvailable() must return false (never throw) when the SDK/key isn't
 *      configured, so AdsOrchestrator falls back to no ads cleanly.
 *   5. Do not call loadAd() for any AdSurface tied to Safety, Sathi
 *      guidance, Map/navigation, or Live Trip — those are intentionally
 *      absent from the AdSurface enum.
 */
interface OnlineAdsProvider : AdsProvider {
    override fun isAvailable(): Boolean
}

/**
 * Picks the real ad provider only when: the person has ads enabled in
 * Settings AND the device is online AND a real provider is configured and
 * reachable. Otherwise — including on any failure — falls back to showing
 * nothing. No fake ads, no fake "ad loaded" claims.
 */
class AdsOrchestrator(
    private val isOnline: () -> Boolean,
    private val adsEnabledInSettings: () -> Boolean,
    private val online: OnlineAdsProvider? = null
) {
    private val noOp = NoOpAdsProvider()

    suspend fun loadAd(surface: AdSurface): AdContent? {
        if (!adsEnabledInSettings() || !isOnline()) return null
        val provider = online?.takeIf { it.isAvailable() } ?: return noOp.loadAd(surface)
        return try {
            provider.loadAd(surface)
        } catch (e: Exception) {
            null // fail closed — never show a broken/fake ad
        }
    }
}
