package com.example.prokject2_tracker.fitness.strength

import kotlinx.serialization.Serializable

@Serializable
data object StrengthExerciseLibraryRoute

@Serializable
data class StrengthExerciseEditRoute(val exerciseId: String? = null)

@Serializable
data object MuscleGroupManageRoute
