package com.example.prokject2_tracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.prokject2_tracker.R
import com.example.prokject2_tracker.fitness.FitnessRoute
import com.example.prokject2_tracker.goals.DayGoalsRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryRoute
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.weight.WeightRoute

/**
 * Bottom-nav destinations. Kept to a max of 4 (Material3 guidance): Tagebuch · Tagesziele ·
 * Gewicht · Fitness. Everything else — Bibliothek, Analyse, Habits, Flüssigkeiten and the other
 * kinds of data — lives in the [DrawerDestination] navigation drawer. Tagebuch is the app's
 * home/start destination, so it goes first.
 */
enum class TopLevelDestination(
    val route: Any,
    val routeQualifiedName: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val domain: AppDomain,
) {
    DIARY(
        route = DiaryRoute,
        routeQualifiedName = DiaryRoute::class.qualifiedName!!,
        labelRes = R.string.nav_diary,
        icon = Icons.Filled.Restaurant,
        domain = AppDomain.DIARY,
    ),
    // Right next to Tagebuch: it is the same day seen from the other side — what was logged there
    // is what these goals are measured against.
    DAY_GOALS(
        route = DayGoalsRoute,
        routeQualifiedName = DayGoalsRoute::class.qualifiedName!!,
        labelRes = R.string.nav_day_goals,
        icon = Icons.Filled.TaskAlt,
        domain = AppDomain.GOALS,
    ),
    WEIGHT(
        route = WeightRoute,
        routeQualifiedName = WeightRoute::class.qualifiedName!!,
        labelRes = R.string.nav_weight,
        icon = Icons.Filled.MonitorWeight,
        domain = AppDomain.WEIGHT,
    ),
    FITNESS(
        route = FitnessRoute,
        routeQualifiedName = FitnessRoute::class.qualifiedName!!,
        labelRes = R.string.nav_fitness,
        icon = Icons.Filled.FitnessCenter,
        domain = AppDomain.FITNESS,
    ),
}
