package com.pathsathi.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pathsathi.app.ui.budget.BudgetScreen
import com.pathsathi.app.ui.common.PathSathiBottomBar
import com.pathsathi.app.ui.explore.ExploreScreen
import com.pathsathi.app.ui.food.FoodScreen
import com.pathsathi.app.ui.home.HomeScreen
import com.pathsathi.app.ui.map.MapScreen
import com.pathsathi.app.ui.memory.MemoryScreen
import com.pathsathi.app.ui.planner.TripPlannerScreen
import com.pathsathi.app.ui.safety.SafetyScreen
import com.pathsathi.app.ui.sathi.SathiScreen
import com.pathsathi.app.ui.settings.SettingsScreen
import com.pathsathi.app.ui.stay.StayScreen
import com.pathsathi.app.ui.transport.TransportScreen
import com.pathsathi.app.ui.trips.LiveTripScreen
import com.pathsathi.app.ui.trips.TripsScreen

@Composable
fun PathSathiNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { PathSathiBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onPlanTrip = { dest ->
                        navController.navigate("${NavRoutes.PLANNER}?destination=${Uri.encode(dest)}")
                    },
                    onOpenBudget = { navController.navigate(NavRoutes.BUDGET_HOME) },
                    onOpenSafety = { navController.navigate(NavRoutes.SAFETY) },
                    onOpenExplore = { navController.navigate(NavRoutes.EXPLORE) },
                    onOpenMemory = { navController.navigate(NavRoutes.MEMORY) },
                    onOpenSathi = { navController.navigate(NavRoutes.SATHI) },
                    onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                )
            }
            composable(NavRoutes.EXPLORE) {
                ExploreScreen(
                    onOpenStay = { navController.navigate(NavRoutes.STAY) },
                    onOpenFood = { navController.navigate(NavRoutes.FOOD) },
                    onOpenTransport = { navController.navigate(NavRoutes.TRANSPORT) },
                )
            }
            composable(NavRoutes.TRIPS) {
                TripsScreen(onOpenTrip = { id -> navController.navigate(NavRoutes.liveTrip(id)) })
            }
            composable(NavRoutes.MAP) {
                MapScreen()
            }
            composable(NavRoutes.SATHI) {
                SathiScreen()
            }

            composable(
                route = "${NavRoutes.PLANNER}?destination={destination}",
                arguments = listOf(navArgument("destination") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val dest = backStackEntry.arguments?.getString("destination") ?: ""
                TripPlannerScreen(
                    initialDestination = dest,
                    onTripCreated = { tripId ->
                        navController.navigate(NavRoutes.liveTrip(tripId)) {
                            popUpTo(NavRoutes.HOME)
                        }
                    }
                )
            }

            composable(
                route = NavRoutes.LIVE_TRIP,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                LiveTripScreen(
                    tripId = tripId,
                    onOpenBudget = { navController.navigate(NavRoutes.budget(tripId)) },
                    onOpenMemory = { navController.navigate(NavRoutes.MEMORY) },
                )
            }

            composable(NavRoutes.BUDGET_HOME) { BudgetScreen(tripId = null) }
            composable(
                route = NavRoutes.BUDGET,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getLong("tripId") ?: 0L
                BudgetScreen(tripId = tripId)
            }

            composable(NavRoutes.STAY) { StayScreen() }
            composable(NavRoutes.FOOD) { FoodScreen() }
            composable(NavRoutes.TRANSPORT) { TransportScreen() }
            composable(NavRoutes.SAFETY) { SafetyScreen() }
            composable(NavRoutes.SETTINGS) { SettingsScreen() }
            composable(NavRoutes.MEMORY) { MemoryScreen(activeTripId = null) }
        }
    }
}
