package com.example.prokject2_tracker.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.prokject2_tracker.R
import com.example.prokject2_tracker.fitness.FitnessRoute
import com.example.prokject2_tracker.fluid.FluidRoute
import com.example.prokject2_tracker.habit.HabitRoute
import com.example.prokject2_tracker.nutrition.diary.DiaryRoute

/**
 * Bottom-nav destinations. Kept to a max of 4 (Material3 guidance). Bibliothek/Analyse moved to
 * the [DrawerDestination] navigation drawer instead, freeing slots for Flüssigkeiten and Habits.
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
    FLUID(
        route = FluidRoute,
        routeQualifiedName = FluidRoute::class.qualifiedName!!,
        labelRes = R.string.nav_fluid,
        icon = Icons.Filled.LocalDrink,
    ),
    FITNESS(
        route = FitnessRoute,
        routeQualifiedName = FitnessRoute::class.qualifiedName!!,
        labelRes = R.string.nav_fitness,
        icon = Icons.Filled.FitnessCenter,
    ),
    HABIT(
        route = HabitRoute,
        routeQualifiedName = HabitRoute::class.qualifiedName!!,
        labelRes = R.string.nav_habit,
        icon = Icons.Filled.Checklist,
    ),
}
