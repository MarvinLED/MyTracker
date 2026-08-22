package com.example.mytracker.fitness.strength

/**
 * The movement pattern a [StrengthExercise] trains, tagged alongside its muscle groups. A fixed
 * set rather than a user-managed library like [MuscleGroup] — the taxonomy is exhaustive by
 * definition, so there is nothing to add or rename.
 */
enum class MovementDirection { PUSH, PULL, ISOMETRIC }

fun MovementDirection.label(): String = when (this) {
    MovementDirection.PUSH -> "Push"
    MovementDirection.PULL -> "Pull"
    MovementDirection.ISOMETRIC -> "Isometrisch"
}
