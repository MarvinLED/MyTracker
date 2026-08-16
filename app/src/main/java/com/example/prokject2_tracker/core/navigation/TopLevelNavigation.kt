package com.example.prokject2_tracker.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Routes a tab or drawer entry is allowed to land on. Everything else is a detail screen reached
 * from one of them (Getränkearten verwalten, Eintrag hinzufügen, Rezept bearbeiten, ...).
 */
val topLevelRouteNames: Set<String> =
    (TopLevelDestination.entries.map { it.routeQualifiedName } +
        DrawerDestination.entries.map { it.routeQualifiedName }).toSet()

/**
 * Switches to a tab/drawer destination, as the bottom bar and the drawer do it.
 *
 * **Every** way into a [topLevelRouteNames] destination has to go through here, including in-screen
 * shortcuts like the Tagebuch's "Bibliothek" button. Pushing one with a plain `navigate` puts it on
 * the stack in a shape this function's `popUpTo(start) { saveState }` then saves and `restoreState`
 * puts straight back: the tab you tap pops it and immediately restores it, so the bar looks dead and
 * only the system back button gets you out. Measured, not theorised — see `TopLevelNavigationTest`.
 *
 * Extracted from `AppScaffold` so that behaviour is testable at all.
 */
fun NavController.navigateToTopLevel(route: Any, topLevelRoutes: Set<String> = topLevelRouteNames) {
    // Drop any detail screen the user had open *before* popUpTo(saveState = true) snapshots this
    // tab's stack — otherwise coming back to the tab restores that detail screen instead of the
    // tab's own overview.
    //
    // `previousBackStackEntry != null` is what keeps this from emptying the graph, and the
    // `!popBackStack()` break cannot stand in for it: popBackStack() carries the pop out even when
    // it returns false, so by the time it says "nothing left" the last entry is already gone and
    // the NavHost has nothing to draw. Today the loop always stops on the start destination, which
    // is top-level itself — the guard is what keeps that from being a coincidence.
    while (currentDestination?.route !in topLevelRoutes && previousBackStackEntry != null) {
        if (!popBackStack()) break
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
