package com.example.prokject2_tracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.prokject2_tracker.R
import com.example.prokject2_tracker.analyse.AnalyseRoute
import com.example.prokject2_tracker.nutrition.library.LibraryRoute

/**
 * Destinations reachable from the [com.example.prokject2_tracker.core.ui.AppScaffold] navigation
 * drawer rather than the bottom-nav bar. Peers of [TopLevelDestination], just surfaced elsewhere.
 */
enum class DrawerDestination(
    val route: Any,
    val routeQualifiedName: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LIBRARY(
        route = LibraryRoute,
        routeQualifiedName = LibraryRoute::class.qualifiedName!!,
        labelRes = R.string.nav_library,
        icon = Icons.Filled.Kitchen,
    ),
    ANALYSE(
        route = AnalyseRoute,
        routeQualifiedName = AnalyseRoute::class.qualifiedName!!,
        labelRes = R.string.nav_analyse,
        icon = Icons.Filled.Insights,
    ),
}
