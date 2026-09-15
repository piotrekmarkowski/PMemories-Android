package com.piotrmarkowski.pmemories.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.piotrmarkowski.pmemories.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piotrmarkowski.pmemories.ui.home.DashboardScreen
import com.piotrmarkowski.pmemories.ui.library.LibraryScreen
import com.piotrmarkowski.pmemories.ui.studio.StudioScreen

/**
 * Android analog of iOS `HomeView`'s `switch selectedTab` + `BottomTabBar`.
 * Only the three tabs that don't depend on travel data — see `MainTab`.
 */
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
            composable(MainTab.Home.route) { DashboardScreen() }
            composable(MainTab.Studio.route) { StudioScreen() }
            composable(MainTab.Library.route) { LibraryScreen() }
        }
    }
}

private fun navigateToTab(navController: androidx.navigation.NavController, tab: MainTab) {
    navController.navigate(tab.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
