package com.piotrmarkowski.pmemories.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.piotrmarkowski.pmemories.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.piotrmarkowski.pmemories.ui.home.DashboardScreen
import com.piotrmarkowski.pmemories.ui.library.LibraryScreen
import com.piotrmarkowski.pmemories.ui.studio.StudioScreen
import com.piotrmarkowski.pmemories.travel.TravelViewModel
import com.piotrmarkowski.pmemories.ui.travel.LeaderboardScreen
import com.piotrmarkowski.pmemories.ui.travel.TravelPassportScreen
import com.piotrmarkowski.pmemories.ui.travel.TravelPosterScreen
import com.piotrmarkowski.pmemories.ui.travel.TravelScreen
import com.piotrmarkowski.pmemories.ui.travel.TripBuilderScreen
import com.piotrmarkowski.pmemories.ui.travel.TripDetailScreen

/**
 * Android analog of iOS `HomeView`'s `switch selectedTab` + `BottomTabBar`.
 * `Travel` (added 15.09.2026, Etap 7) has its own sub-navigation
 * (builder/detail/ranking) layered on the SAME `navController` rather than
 * a nested graph — simplest wiring for four flat-ish destinations, revisit
 * if Travel grows a deeper stack later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMemoriesNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination

            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute?.hierarchy?.any { it.route == MainTab.Home.route } == true,
                    onClick = { navigateToTab(navController, MainTab.Home) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) }
                )
                NavigationBarItem(
                    selected = currentRoute?.hierarchy?.any { it.route == MainTab.Studio.route } == true,
                    onClick = { navigateToTab(navController, MainTab.Studio) },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    label = { Text(stringResource(R.string.studio)) }
                )
                NavigationBarItem(
                    selected = currentRoute?.hierarchy?.any { it.route == MainTab.Travel.route } == true,
                    onClick = { navigateToTab(navController, MainTab.Travel) },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text("Travel") }
                )
                NavigationBarItem(
                    selected = currentRoute?.hierarchy?.any { it.route == MainTab.Library.route } == true,
                    onClick = { navigateToTab(navController, MainTab.Library) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.library)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainTab.Home.route) { DashboardScreen(onOpenTravel = { navigateToTab(navController, MainTab.Travel) }) }
            composable(MainTab.Studio.route) { StudioScreen() }
            composable(MainTab.Travel.route) {
                TravelScreen(
                    onOpenTrip = { tripId -> navController.navigate("travel/trip/$tripId") },
                    onNewTrip = { navController.navigate("travel/builder") },
                    onOpenRanking = { navController.navigate("travel/ranking") },
                    onOpenPoster = { navController.navigate("travel/poster") },
                    onOpenPassport = { navController.navigate("travel/passport") }
                )
            }
            composable("travel/poster") {
                val posterViewModel: TravelViewModel = viewModel()
                val posterState by posterViewModel.uiState.collectAsState()
                TravelPosterScreen(trips = posterState.trips, onBack = { navController.popBackStack() })
            }
            composable("travel/passport") {
                val passportViewModel: TravelViewModel = viewModel()
                val passportState by passportViewModel.uiState.collectAsState()
                TravelPassportScreen(trips = passportState.trips, onBack = { navController.popBackStack() })
            }
            composable("travel/builder") {
                TripBuilderScreen(onDone = { navController.popBackStack() })
            }
            composable(
                "travel/trip/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
                TripDetailScreen(tripId = tripId, onEdit = { navController.navigate("travel/builder") })
            }
            composable("travel/ranking") {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }
            composable(MainTab.Library.route) { LibraryScreen() }
        }
    }
}

/**
 * 15.09.2026 — deliberately WITHOUT `saveState`/`restoreState`. Those flags
 * are the standard bottom-nav recipe (remember each tab's own scroll
 * position/back stack), but Travel's sub-screens (`travel/builder`,
 * `travel/trip/{id}`) live in the SAME flat graph as `MainTab.Travel`
 * itself rather than a dedicated nested graph — found live on-device:
 * `restoreState` brought back "Build Route" instead of the trip list when
 * re-tapping the Travel tab after visiting the builder once. Dropping both
 * flags means every tab tap goes to a fresh top-level screen (Home/Studio/
 * Library lose cross-tab scroll memory, an acceptable trade at this stage)
 * — correct fix is per-tab nested graphs, left as a follow-up rather than
 * a bigger refactor right now.
 */
private fun navigateToTab(navController: androidx.navigation.NavController, tab: MainTab) {
    navController.navigate(tab.route) {
        popUpTo(navController.graph.findStartDestination().id)
        launchSingleTop = true
    }
}
