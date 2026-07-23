package com.example.prokject2_tracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.prokject2_tracker.R
import com.example.prokject2_tracker.analyse.AnalyseRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryRoute
import com.example.prokject2_tracker.nutrition.library.LibraryRoute

/**
 * Bottom-nav destinations. Kept to a max of 4 (Material3 guidance); each future stage appends its
 * own entry/entries here without touching [com.example.prokject2_tracker.core.ui.AppScaffold].
 * 3 of 4 slots are used by this slice (Tagebuch/Bibliothek/Analyse) — the last slot is reserved
 * for a later, consolidated Fitness/Habits tab.
 */
enum class TopLevelDestination(
    val route: Any,
    val routeQualifiedName: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DIARY(
        route = DiaryRoute,
        routeQualifiedName = DiaryRoute::class.qualifiedName!!,
        labelRes = R.string.nav_diary,
        icon = Icons.Filled.Restaurant,
    ),
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
