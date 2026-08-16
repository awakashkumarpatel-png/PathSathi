# Path Sathi — Android Project

Offline-first tourist travel companion app with Sathi Robot, automatic trip
planning, and a 12-phase feature set, plus an optional Online + Offline,
Ads, and web-presence layer on top. This README reflects the project's
**current state** — earlier passes' turn-by-turn history has been folded
in here rather than kept as separate stacked sections, since some of that
narration (e.g. "the logo is unavailable") is no longer accurate now that
the real logo is integrated.

## ⚠️ Build verification status — read this first

This environment has **no Android SDK, no local Gradle installation, and
no network access**. No `./gradlew assembleDebug` or Android Studio
compile has ever been run on this project in any pass. What I *have* done,
every pass, is the strongest static validation available — see below —
which is real and catches real bugs (two were found and fixed in earlier
passes: a Java-version/AGP mismatch, and a missing theme dependency) but is
not a substitute for an actual compile. **Please open this in Android
Studio for its first real build.** If you hit compile errors, send me the
exact text and I'll fix it directly.

`gradle/wrapper/gradle-wrapper.jar` is not included — it's a compiled
binary Gradle generates itself, and this sandbox has no Gradle install and
no network to fetch or generate one (re-verified as recently as this
pass: no local `gradle` binary, a direct HTTPS request to
`services.gradle.org` returns 403, no existing copy anywhere on the
filesystem). `gradlew` and `gradlew.bat` (plain text scripts) **are**
present and valid — verified byte-for-byte in the actual delivered ZIP,
not just the working copy. The project also includes your own
`gradle/wrapper/WrapperDownloader.java` + `gradle/wrapper/README.md`
bootstrap approach, left exactly as you provided it; I haven't been able
to verify that script runs correctly or that its pinned checksum is
authentic, since testing it needs network access this sandbox doesn't
have — please verify it on your machine before relying on it.

## Logo

**The official Path Sathi logo is integrated**, using the exact PNG you
uploaded (`file_000000005ae082118475d0b3430344b3.png`, 1254×1254,
MD5 `99054496a4205a30a0e12c1fcd67365a`) — no redesign, recolor, or crop:

- The exact file is copied byte-for-byte to
  `app/src/main/res/drawable-nodpi/path_sathi_logo.png` (verified via MD5
  match). `drawable-nodpi` means Android displays it at whatever size the
  layout requests without reinterpreting the source pixels per density.
- **Home Screen**: the old `Icons.Filled.Explore` placeholder in the logo
  position is gone. It's now `Image(painter =
  painterResource(R.drawable.path_sathi_logo), contentScale =
  ContentScale.Fit)`, sized so the **entire logo is visible — nothing
  cropped**. (A separate, unrelated `Icons.Filled.Explore` still labels the
  "Explore" quick-access button elsewhere on Home — that's a functional
  navigation icon, not the logo, and wasn't part of what was asked to
  change.)
- **Launcher icon**: the same exact source image was resampled — resized
  only, identical pixel content, no crop/recolor/redesign — into the 5
  standard Android density buckets (48/72/96/144/192px) for
  `ic_launcher.png` and `ic_launcher_round.png`.
- The old adaptive-icon XML split (`mipmap-anydpi-v26/ic_launcher.xml` +
  placeholder foreground/background vector drawables) was removed rather
  than forcing this single flat logo into Android's foreground/background
  layering — that split requires guessing at padding/safe-zones, which
  risks the OS masking/clipping the wordmark on some launchers, an
  indirect crop that wasn't asked for. Removing it means every Android
  version shows the complete, unmodified logo bitmap. One caveat: some
  launchers apply their own automatic circular/squircle mask to *any* app
  icon, adaptive or not — that's OS/launcher behavior outside anyone's
  control, not something done to the artwork here.
- No dedicated splash-screen mechanism was added (that would mean a new
  dependency and a startup-behavior change beyond this task) — the
  launcher icon is what Android's own splash-screen system (API 31+)
  automatically shows at startup, so that's covered without new code.

## Phase status (all 12 — unchanged, nothing reordered)

All 12 phases from the original roadmap remain present with real
implementations (verified by file roll-call each pass): Foundation, Sathi
Robot, Trip Planner, Maps & Navigation, Explore, Stay+Food+Transport,
Smart Budget, Safety & Emergency, Language System, Complete Offline Mode,
Smart Automatic Travel Mode, and Advanced AI. No phase has been reordered,
removed, or had existing functionality taken away across any pass.

## Online + Offline, Ads, URL, and optional online AI architecture

Everything in this section is additive and off-by-default. The offline
core — saved trips, itineraries, budget, saved places, emergency info,
Sathi Robot's offline replies, and offline voice — is completely
unaffected and keeps working with no internet at all.

**Online + Offline architecture** (`com.pathsathi.app.core`,
`com.pathsathi.app.online`)
- `ConnectivityObserver` — the single real-time online/offline signal for
  the whole app, built on `ConnectivityManager`.
- `AppConfig` — one centralized, DataStore-backed settings object: a
  master "online features" switch, a separate "online AI" switch, an
  "ads" switch, and one configurable website URL. All default to
  off/placeholder.
- `com.pathsathi.app.online.OnlineServices` — the same offline-fallback /
  online-interface / orchestrator pattern used for Maps and AI, extended
  to Weather, updated Tourist Info, Transport updates, Cloud Sync, and
  Booking. Every `Online*Provider` interface is unimplemented on purpose
  (no live services, no keys); every offline fallback is real and returns
  an honest "needs internet" message rather than fake data; every
  Orchestrator fails closed to its offline fallback on any exception.
- All five are wired into real UI, not sitting as unused interfaces:
  Weather + updated Tourist Info cards on Explore; Transport updates on
  the Transport screen; a "Sync now" button on Settings (Cloud Sync); a
  "Check availability" button per stay option on Stay (Booking). Every one
  currently shows its offline-fallback message, since no online provider
  is configured anywhere.
- Settings screen has a live "Online & Offline" toggle section.

**Ads** (`com.pathsathi.app.ads`)
- `AdsProvider` interface, `NoOpAdsProvider` (the always-safe default —
  never shows an ad), `OnlineAdsProvider` interface (unimplemented, with a
  documented integration path for a real ad SDK later), and
  `AdsOrchestrator`, which only calls a real provider when ads are turned
  on AND the device is online AND a real provider is configured —
  otherwise, and on any failure, it shows nothing.
- `AdSurface` enum only has `HOME, EXPLORE, STAY, FOOD, TRANSPORT` — no
  entries for Safety, Sathi, Map, or Live Trip, so it's structurally not
  possible to wire an ad into those screens through this component.
- `AdSlot` composable renders nothing at all unless a real ad actually
  loaded — no placeholder box, no fake "ad loading" state.
- Settings screen has an "Ads" toggle with the same off-by-default,
  needs-a-real-provider behavior explained in-app.

**App URL / web presence** (`com.pathsathi.app.core.AppConfig`)
- One centralized, persisted `websiteUrl` setting, plus
  `PRIVACY_POLICY_URL_PLACEHOLDER` / `SUPPORT_URL_PLACEHOLDER` constants
  that derive from it once configured.
- Default value is a clearly-fake, clearly-marked placeholder
  (`https://CONFIGURE-ME.pathsathi.example/`) — no real domain was
  invented or guessed. `AppConfig.isWebsiteConfigured()` returns false for
  this placeholder so future UI can hide/disable web links until a real
  URL is entered.
- Settings screen lets you view/edit this URL now.

**Optional online AI** (`com.pathsathi.app.ai`)
- Sathi Robot's chat goes through `AIOrchestrator` → `OfflineAIFallback`.
  No `OnlineAIProvider` is implemented or passed in, so behavior is
  unchanged regardless of the "Online AI" toggle.
- The toggle is real and persisted (`AppConfig.onlineAiEnabled`); the
  Sathi screen shows an honest status line — "Online AI is turned on and
  you're online, but no provider is configured yet" — rather than
  silently doing nothing when enabled.
- No API key is hard-coded anywhere in this codebase.

## Verified this pass (logo integration)

- Both `gradlew`/`gradlew.bat` present, correct size, executable bit set —
  re-checked directly inside the packaged ZIP via Python's zipfile module.
- Real logo file confirmed present and MD5-identical to the upload at
  `drawable-nodpi/path_sathi_logo.png`; all 10 generated launcher PNGs
  (5 densities × 2 variants) confirmed valid PNGs at their exact expected
  dimensions.
- No dangling references anywhere to the removed adaptive-icon XML or
  placeholder vector drawables.
- `R.drawable.path_sathi_logo` reference in `HomeScreen.kt` matches an
  actual file in a valid resource directory.
- 55 Kotlin files: all balanced braces/parens, all with `package`
  declarations, zero duplicate top-level declarations.
- All XML files well-formed.
- All 14 screens still reachable from `NavGraph.kt`.
- No hard-coded API keys or secrets anywhere in source.
- No ad or online provider actually instantiated anywhere — every
  `Online*Provider` parameter is `null` at every call site, so Settings
  toggles change stored intent only, never live behavior.
- English/Hindi `strings.xml` key sets still match 1:1; every `R.string.*`
  reference still resolves.

No new features were added this pass, no API keys were added, and no ad or
online provider was activated.

## Project structure

```
PathSathi/
├── gradlew, gradlew.bat                 (wrapper scripts — jar itself not included, see above)
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/pathsathi/app/
│       │   ├── MainActivity.kt, PathSathiApp.kt
│       │   ├── data/          (Room entities incl. travelers, DAOs, database, repository, demo data)
│       │   ├── engine/        (Itinerary, AutoTravel, Sathi chat, serializer)
│       │   ├── map/           (provider abstraction, offline provider, Haversine math)
│       │   ├── ai/            (AI service interface, offline fallback, orchestrator)
│       │   ├── core/          (AppConfig settings, ConnectivityObserver, OnlineGate)
│       │   ├── online/        (Weather/TouristInfo/Transport/Sync/Booking architecture)
│       │   ├── ads/           (AdsProvider, AdSlot, orchestrator)
│       │   ├── voice/         (TTS/STT wrapper)
│       │   ├── alerts/        (WorkManager reminders)
│       │   └── ui/            (theme, navigation, one package per screen area)
│       └── res/
│           ├── drawable-nodpi/path_sathi_logo.png   (official logo, exact upload)
│           ├── mipmap-*dpi/                          (launcher icons generated from the logo)
│           └── values(-hi)/                          (strings, colors, themes)
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── gradle/wrapper/                       (gradle-wrapper.properties, your WrapperDownloader.java bootstrap)
```

## How to build (once you have Android Studio / network)

1. Open the project root in Android Studio.
2. Let Gradle sync (needs internet the first time, both for dependencies
   and to resolve the wrapper jar).
3. Run on a device/emulator with API 24+.
4. Grant location/microphone/notification permissions when prompted for
   Map, Sathi voice input, and Alerts — the app stays usable with reduced
   functionality if any are denied.

## Final core functionality additions in this source

- Smart Budget now includes today / this-week / this-month totals and keeps category totals.
- Shared group expenses can be recorded without assigning a payer; individual expenses can still be assigned to a traveler.
- Sathi Robot can record a simple offline expense command such as `add expense 500 food lunch` or `खर्च 500 food lunch`, then confirms it by text and natural TTS.
- Sathi can use the latest planned trip as context before a trip becomes ACTIVE, while ACTIVE trips still take priority.
- The existing offline itinerary, Preview, Live Trip GPS/ETA, Safety, Memory, Map, Explore, Stay, Food, Transport, language, alerts, ads-gating, online-gating, and optional-provider architecture remains in place.

Optional live services (real cloud sync, live weather, live transport feeds, real booking, real online AI, and a real ad network) still require their respective provider/API configuration; the offline core does not depend on them.
