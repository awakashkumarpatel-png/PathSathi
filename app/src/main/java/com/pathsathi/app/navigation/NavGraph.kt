package com.pathsathi.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.ui.screens.*
import kotlinx.coroutines.flow.first

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TREK_DETAIL = "trek_detail/{trekId}"
    const val TRACKING = "tracking/{trekName}"
    const val TRACKING_HISTORY = "tracking_history"
    const val MAP = "map/{trekId}"
    const val FAVORITES = "favorites"
    const val TRIP_PLANNER = "trip_planner"
    const val COMPASS = "compass"
    const val SOS = "sos"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val JOURNAL = "journal"
    const val MY_TRIPS = "my_trips"
    const val CREATE_TRIP = "create_trip?tripId={tripId}"
    const val TRIP_VIEW = "trip_view/{tripId}"
    const val SETTINGS = "settings"
    const val OFFLINE_MAPS = "offline_maps"
    const val ASSISTANT = "assistant"
    const val ITINERARY_PREVIEW = "itinerary_preview"
    const val PROFILE = "profile"
    const val CHECKIN_SETTINGS = "checkin_settings"
    const val CHECKIN_PROMPT = "checkin_prompt"
    const val NEARBY_HELP = "nearby_help"
    const val TERMS = "terms"
    const val PRIVACY_POLICY = "privacy_policy"

    fun trekDetail(trekId: String) = "trek_detail/$trekId"
    fun tracking(trekName: String) = "tracking/${java.net.URLEncoder.encode(trekName, "UTF-8")}"
    fun map(trekId: String) = "map/$trekId"
    fun createTrip(tripId: Long? = null) = if (tripId != null) "create_trip?tripId=$tripId" else "create_trip"
    fun tripView(tripId: Long) = "trip_view/$tripId"
}

@Composable
fun PathSathiNavGraph(
    navController: NavHostController = rememberNavController(),
    startDeepLinkRoute: String? = null
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            val context = LocalContext.current
            var delayDone by remember { mutableStateOf(false) }
            var onboardingDone by remember { mutableStateOf<Boolean?>(null) }

            SplashScreen(onFinished = { delayDone = true })

            LaunchedEffect(Unit) {
                onboardingDone = AppPreferences.isOnboardingComplete(context).first()
            }

            LaunchedEffect(delayDone, onboardingDone) {
                val done = onboardingDone
                if (delayDone && done != null) {
                    val destination = if (!done) Routes.ONBOARDING else (startDeepLinkRoute ?: Routes.HOME)
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(startDeepLinkRoute ?: Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onOpenTerms = { navController.navigate(Routes.TERMS) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY_POLICY) }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onTrekClick = { navController.navigate(Routes.trekDetail(it)) },
                onCompassClick = { navController.navigate(Routes.COMPASS) },
                onSosClick = { navController.navigate(Routes.SOS) },
                onJournalClick = { navController.navigate(Routes.JOURNAL) },
                onHistoryClick = { navController.navigate(Routes.TRACKING_HISTORY) },
                onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
                onPlanTripClick = { navController.navigate(Routes.TRIP_PLANNER) },
                onEmergencyContactsClick = { navController.navigate(Routes.EMERGENCY_CONTACTS) },
                onMyTripsClick = { navController.navigate(Routes.MY_TRIPS) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onAssistantClick = { navController.navigate(Routes.ASSISTANT) },
                onNearbyHelpClick = { navController.navigate(Routes.NEARBY_HELP) }
            )
        }
        composable(
            Routes.TREK_DETAIL,
            arguments = listOf(navArgument("trekId") { type = NavType.StringType })
        ) { backStackEntry ->
            val trekId = backStackEntry.arguments?.getString("trekId") ?: ""
            TrekDetailScreen(
                trekId = trekId,
                onBack = { navController.popBackStack() },
                onStartTracking = { trekName -> navController.navigate(Routes.tracking(trekName)) },
                onViewMap = { id -> navController.navigate(Routes.map(id)) }
            )
        }
        composable(
            Routes.TRACKING,
            arguments = listOf(navArgument("trekName") { type = NavType.StringType })
        ) { backStackEntry ->
            val trekName = backStackEntry.arguments?.getString("trekName") ?: "Trek"
            TrackingScreen(
                trekName = trekName,
                onBack = { navController.popBackStack() },
                onFinished = { _, _ -> navController.popBackStack() }
            )
        }
        composable(Routes.TRACKING_HISTORY) {
            TrackingHistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.MAP,
            arguments = listOf(navArgument("trekId") { type = NavType.StringType })
        ) { backStackEntry ->
            val trekId = backStackEntry.arguments?.getString("trekId") ?: ""
            MapScreen(trekId = trekId, onBack = { navController.popBackStack() })
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onTrekClick = { navController.navigate(Routes.trekDetail(it)) }
            )
        }
        composable(Routes.TRIP_PLANNER) {
            TripPlannerScreen(
                onBack = { navController.popBackStack() },
                onViewSavedItineraries = { navController.navigate(Routes.ITINERARY_PREVIEW) }
            )
        }
        composable(Routes.COMPASS) {
            CompassScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SOS) {
            SosScreen(
                onBack = { navController.popBackStack() },
                onManageContacts = { navController.navigate(Routes.EMERGENCY_CONTACTS) }
            )
        }
        composable(Routes.EMERGENCY_CONTACTS) {
            EmergencyContactsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.JOURNAL) {
            JournalScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.MY_TRIPS) {
            MyTripsScreen(
                onBack = { navController.popBackStack() },
                onCreateTrip = { navController.navigate(Routes.createTrip()) },
                onViewTrip = { navController.navigate(Routes.tripView(it)) },
                onEditTrip = { navController.navigate(Routes.createTrip(it)) }
            )
        }
        composable(
            "create_trip?tripId={tripId}",
            arguments = listOf(navArgument("tripId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId")?.takeIf { it != -1L }
            CreateTripScreen(
                editTripId = tripId,
                onBack = { navController.popBackStack() },
                onSaved = { savedTripId, isNewTrip ->
                    if (isNewTrip) {
                        navController.navigate(Routes.tripView(savedTripId)) {
                            popUpTo("create_trip?tripId={tripId}") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(
            Routes.TRIP_VIEW,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getLong("tripId") ?: -1L
            TripViewScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.createTrip(it)) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOfflineMapsClick = { navController.navigate(Routes.OFFLINE_MAPS) },
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onCheckInClick = { navController.navigate(Routes.CHECKIN_SETTINGS) },
                onNearbyHelpClick = { navController.navigate(Routes.NEARBY_HELP) },
                onTermsClick = { navController.navigate(Routes.TERMS) },
                onPrivacyClick = { navController.navigate(Routes.PRIVACY_POLICY) }
            )
        }
        composable(Routes.OFFLINE_MAPS) {
            OfflineMapsScreen(
                onBack = { navController.popBackStack() },
                onOpenMap = { trekId -> navController.navigate(Routes.map(trekId)) }
            )
        }
        composable(Routes.ASSISTANT) {
            AssistantScreen(
                onBack = { navController.popBackStack() },
                onNavigateRoute = { route -> navController.navigate(route) }
            )
        }
        composable(Routes.ITINERARY_PREVIEW) {
            PreviewScreen(
                onBack = { navController.popBackStack() },
                onPlanTrip = { navController.navigate(Routes.TRIP_PLANNER) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onTermsClick = { navController.navigate(Routes.TERMS) },
                onPrivacyClick = { navController.navigate(Routes.PRIVACY_POLICY) }
            )
        }
        composable(Routes.CHECKIN_SETTINGS) {
            CheckInSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CHECKIN_PROMPT) {
            CheckInPromptScreen(onDone = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            })
        }
        composable(Routes.NEARBY_HELP) {
            NearbyHelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TERMS) {
            TermsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PRIVACY_POLICY) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
