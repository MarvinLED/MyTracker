package com.example.mytracker.achievements

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.fitness.FitnessGoalMetric

/**
 * The five things the figure is made of. Each one is fed by the goals that actually belong to it, so
 * the figure stays an honest picture of what was done rather than a score that could be pointed
 * anywhere.
 */
enum class AvatarAttribute(val label: String, val shows: String) {
    KRAFT("Kraft", "Schultern, Arme und Brust"),
    FORM("Form", "die Taille"),
    AUSDAUER("Ausdauer", "Beine und Haltung"),
    KLARHEIT("Klarheit", "Kopf und Blick"),
    VITALITAET("Vitalität", "Hautton und Ausstrahlung"),
}

/**
 * Which attribute a day-goal feeds, keyed on the row ids the goal builders in
 * [com.example.mytracker.goals.DayGoalRows] already hand out. Matching on those ids rather than
 * adding a field to `DayGoalRow` keeps the whole mapping in this package: the goals know nothing
 * about the game, and changing what feeds what touches one file.
 *
 * Null means the row earns nothing. Nothing is currently mapped to null, but a new kind of goal will
 * be — and silently routing it somewhere arbitrary would be worse than it earning nothing until
 * someone decides where it belongs.
 */
fun attributeForGoalRow(rowId: String): AvatarAttribute? = when {
    rowId.startsWith("nutrient-") -> when (rowId.removePrefix("nutrient-")) {
        // Protein builds; the rest is what the waistline is made of.
        Nutrient.PROTEIN.name -> AvatarAttribute.KRAFT
        else -> AvatarAttribute.FORM
    }
    rowId.startsWith("sleep-") -> AvatarAttribute.KLARHEIT
    // Water, chores and habits are the day-to-day upkeep rather than any one kind of training.
    rowId.startsWith("fluid-") || rowId.startsWith("task-") || rowId.startsWith("habit-") ->
        AvatarAttribute.VITALITAET
    else -> null
}

/** Which attribute a Fitness-Ziel feeds. Cardio is stamina, everything with a barbell is strength. */
fun attributeForFitnessMetric(metric: FitnessGoalMetric): AvatarAttribute = when (metric) {
    FitnessGoalMetric.CARDIO_SESSIONS,
    FitnessGoalMetric.CARDIO_DURATION_MINUTES,
    -> AvatarAttribute.AUSDAUER

    else -> AvatarAttribute.KRAFT
}
