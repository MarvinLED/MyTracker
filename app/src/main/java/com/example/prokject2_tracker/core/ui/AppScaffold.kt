package com.example.prokject2_tracker.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.prokject2_tracker.core.navigation.AppNavHost
import com.example.prokject2_tracker.core.navigation.DrawerDestination
import com.example.prokject2_tracker.core.navigation.TopLevelDestination
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val destinations = TopLevelDestination.entries
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Routes that a tab/drawer entry is allowed to land on. Everything else is a detail screen
    // reached from one of them (Getränkearten verwalten, Eintrag hinzufügen, Rezept bearbeiten, ...).
    val topLevelRoutes = remember {
        (TopLevelDestination.entries.map { it.routeQualifiedName } +
            DrawerDestination.entries.map { it.routeQualifiedName }).toSet()
    }

    fun navigateToTopLevel(route: Any) {
        // Drop any detail screen the user had open *before* popUpTo(saveState = true) snapshots this
        // tab's stack — otherwise coming back to the tab restores that detail screen instead of the
        // tab's own overview.
        while (navController.currentDestination?.route !in topLevelRoutes) {
            if (!navController.popBackStack()) break
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.routeQualifiedName
                    } == true

                    NavigationDrawerItem(
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateToTopLevel(destination.route)
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Each screen's own TopAppBar already handles the status bar inset; without this,
            // Scaffold's default insets reserve that space a second time, leaving an empty strip
            // above every screen's top bar.
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.routeQualifiedName
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
