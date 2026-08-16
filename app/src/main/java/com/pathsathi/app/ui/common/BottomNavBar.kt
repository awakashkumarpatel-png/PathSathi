package com.pathsathi.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pathsathi.app.R
import com.pathsathi.app.ui.navigation.NavRoutes

private data class BottomItem(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val items = listOf(
    BottomItem(NavRoutes.HOME, R.string.nav_home, Icons.Filled.Home),
    BottomItem(NavRoutes.EXPLORE, R.string.nav_explore, Icons.Filled.Explore),
    BottomItem(NavRoutes.TRIPS, R.string.nav_trips, Icons.Filled.Luggage),
    BottomItem(NavRoutes.MAP, R.string.nav_map, Icons.Filled.Map),
    BottomItem(NavRoutes.SATHI, R.string.nav_sathi, Icons.Filled.Chat),
)

@Composable
fun PathSathiBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = stringResourceCompat(item.labelRes)) },
                label = { Text(stringResourceCompat(item.labelRes)) }
            )
        }
    }
}

@Composable
private fun stringResourceCompat(resId: Int) = androidx.compose.ui.res.stringResource(id = resId)

private fun androidx.navigation.NavGraph.findStartDestination() =
    findNode(startDestinationId) ?: this
