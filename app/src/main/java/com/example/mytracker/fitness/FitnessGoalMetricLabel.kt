package com.example.mytracker.fitness

fun FitnessGoalMetric.label(): String = when (this) {
    FitnessGoalMetric.CARDIO_SESSIONS -> "Cardio-Einheiten"
    FitnessGoalMetric.CARDIO_DURATION_MINUTES -> "Cardio-Dauer (Minuten)"
    FitnessGoalMetric.STRENGTH_SETS_TOTAL -> "Kraft-Sätze gesamt"
    FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP -> "Kraft-Sätze pro Muskelgruppe"
    FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION -> "Kraft-Sätze pro Bewegungsrichtung"
}
