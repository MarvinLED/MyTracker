package com.example.prokject2_tracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.prokject2_tracker.R
import com.example.prokject2_tracker.analyse.AnalyseRoute
import com.example.prokject2_tracker.bloodpressure.BloodPressureRoute
import com.example.prokject2_tracker.goals.GoalsRoute
import com.example.prokject2_tracker.habit.HabitRoute
import com.example.prokject2_tracker.measurement.MeasurementRoute
import com.example.prokject2_tracker.nutrition.library.LibraryRoute
import com.example.prokject2_tracker.sleep.SleepRoute
import com.example.prokject2_tracker.task.TaskRoute
import com.example.prokject2_tracker.ui.theme.AppDomain
import com.example.prokject2_tracker.weight.WeightRoute

/**
 * Destinations reachable from the [com.example.prokject2_tracker.core.ui.AppScaffold] navigation
 * drawer rather than the bottom-nav bar. Peers of [TopLevelDestination], just surfaced elsewhere.
 */
enum class DrawerDestination(
    val route: Any,
    val routeQualifiedName: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val domain: AppDomain,
) {
    LIBRARY(
        route = LibraryRoute,
        routeQualifiedName = LibraryRoute::class.qualifiedName!!,
        labelRes = R.string.nav_library,
        icon = Icons.Filled.Kitchen,
        domain = AppDomain.LIBRARY,
    ),
    ANALYSE(
        route = AnalyseRoute,
        routeQualifiedName = AnalyseRoute::class.qualifiedName!!,
        labelRes = R.string.nav_analyse,
        icon = Icons.Filled.Insights,
        domain = AppDomain.ANALYSE,
    ),
    GOALS(
        route = GoalsRoute,
        routeQualifiedName = GoalsRoute::class.qualifiedName!!,
        labelRes = R.string.nav_goals,
        icon = Icons.Filled.Flag,
        domain = AppDomain.GOALS,
    ),
    HABIT(
        route = HabitRoute,
        routeQualifiedName = HabitRoute::class.qualifiedName!!,
        labelRes = R.string.nav_habit,
        icon = Icons.Filled.Checklist,
        domain = AppDomain.HABIT,
    ),
    TASK(
        route = TaskRoute,
        routeQualifiedName = TaskRoute::class.qualifiedName!!,
        labelRes = R.string.nav_tasks,
        icon = Icons.Filled.Task,
        domain = AppDomain.TASK,
    ),
    WEIGHT(
        route = WeightRoute,
        routeQualifiedName = WeightRoute::class.qualifiedName!!,
        labelRes = R.string.nav_weight,
        icon = Icons.Filled.MonitorWeight,
        domain = AppDomain.WEIGHT,
    ),
    MEASUREMENT(
        route = MeasurementRoute,
        routeQualifiedName = MeasurementRoute::class.qualifiedName!!,
        labelRes = R.string.nav_measurement,
        icon = Icons.Filled.Straighten,
        domain = AppDomain.MEASUREMENT,
    ),
    SLEEP(
        route = SleepRoute,
        routeQualifiedName = SleepRoute::class.qualifiedName!!,
        labelRes = R.string.nav_sleep,
        icon = Icons.Filled.Bedtime,
        domain = AppDomain.SLEEP,
    ),
    BLOOD_PRESSURE(
        route = BloodPressureRoute,
        routeQualifiedName = BloodPressureRoute::class.qualifiedName!!,
        labelRes = R.string.nav_blood_pressure,
        icon = Icons.Filled.MonitorHeart,
        domain = AppDomain.BLOOD_PRESSURE,
    ),
}
