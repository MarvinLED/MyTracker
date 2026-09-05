package com.example.mytracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Task
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mytracker.R
import com.example.mytracker.achievements.AchievementsRoute
import com.example.mytracker.analyse.AnalyseRoute
import com.example.mytracker.bloodpressure.BloodPressureRoute
import com.example.mytracker.core.backup.BackupRoute
import com.example.mytracker.fluid.FluidRoute
import com.example.mytracker.goals.GoalsRoute
import com.example.mytracker.habit.HabitRoute
import com.example.mytracker.measurement.MeasurementRoute
import com.example.mytracker.nutrition.library.LibraryRoute
import com.example.mytracker.sleep.SleepRoute
import com.example.mytracker.task.TaskRoute
import com.example.mytracker.ui.theme.AppDomain

/**
 * Destinations reachable from the [com.example.mytracker.core.ui.AppScaffold] navigation
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
    ACHIEVEMENTS(
        route = AchievementsRoute,
        routeQualifiedName = AchievementsRoute::class.qualifiedName!!,
        labelRes = R.string.nav_achievements,
        icon = Icons.Filled.EmojiEvents,
        domain = AppDomain.ACHIEVEMENTS,
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
    FLUID(
        route = FluidRoute,
        routeQualifiedName = FluidRoute::class.qualifiedName!!,
        labelRes = R.string.nav_fluid,
        icon = Icons.Filled.LocalDrink,
        domain = AppDomain.FLUID,
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

    // Last on purpose: it is the only entry that is about the app rather than about a kind of data,
    // so it sits below the areas it backs up instead of among them.
    BACKUP(
        route = BackupRoute,
        routeQualifiedName = BackupRoute::class.qualifiedName!!,
        labelRes = R.string.nav_backup,
        icon = Icons.Filled.Backup,
        domain = AppDomain.BACKUP,
    ),
}
