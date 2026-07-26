package com.example.prokject2_tracker.fitness.strength

import kotlinx.serialization.Serializable

@Serializable
data object StrengthExerciseLibraryRoute

@Serializable
data class StrengthExerciseEditRoute(val exerciseId: String? = null)

@Serializable
data object MuscleGroupManageRoute

/**
 * The per-exercise page: last/current session on top, fast set entry in the middle, weekly chart at
 * the bottom. [epochDay] is non-null because Navigation has no nullable NavType for primitives —
 * callers pass `DateUtils.todayEpochDay()` to open on today, or an entry's day to edit history.
 */
@Serializable
data class StrengthExerciseDetailRoute(val exerciseId: String, val epochDay: Long)
