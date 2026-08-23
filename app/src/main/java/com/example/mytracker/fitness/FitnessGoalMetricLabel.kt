package com.example.mytracker.fitness

fun FitnessGoalMetric.label(): String = when (this) {
    FitnessGoalMetric.CARDIO_SESSIONS -> "Cardio-Einheiten"
    FitnessGoalMetric.CARDIO_DURATION_MINUTES -> "Cardio-Dauer (Minuten)"
    FitnessGoalMetric.STRENGTH_SETS_TOTAL -> "Kraft-Sätze gesamt"
    FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP -> "Kraft-Sätze pro Muskelgruppe"
    FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION -> "Kraft-Sätze pro Bewegungsrichtung"
    FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE -> "Steigerung Maximalgewicht"
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE -> "Steigerung Gesamtvolumen"
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP -> "Steigerung Volumen (Muskelgruppe)"
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MOVEMENT_DIRECTION -> "Steigerung Volumen (Bewegungsrichtung)"
}

/** The unit [FitnessGoal.targetValue] is read in. Empty for the metrics that simply count. */
fun FitnessGoalMetric.unit(): String = when (this) {
    FitnessGoalMetric.CARDIO_DURATION_MINUTES -> "min"
    FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE,
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP,
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MOVEMENT_DIRECTION,
    -> "kg"
    else -> ""
}

/** The unit one goal is actually read in — percent once it is set that way. */
fun FitnessGoal.unit(): String = if (isPercent) "%" else metric.unit()

/**
 * True for the metrics that are measured against the period *before* rather than against zero. A
 * progress of 0 means "no better than last time" for these, not "nothing done yet" — which is why
 * they are labelled with a sign wherever they are shown.
 */
val FitnessGoalMetric.isIncrease: Boolean
    get() = when (this) {
        FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE,
        FitnessGoalMetric.STRENGTH_VOLUME_INCREASE,
        FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP,
        FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MOVEMENT_DIRECTION,
        -> true
        else -> false
    }

/** True for the metrics that need an exercise to be about anything. */
val FitnessGoalMetric.isExerciseScoped: Boolean
    get() = this == FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE ||
        this == FitnessGoalMetric.STRENGTH_VOLUME_INCREASE
