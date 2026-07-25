package com.example.prokject2_tracker.core.datastore

/**
 * What a nutrition goal's number means. The distinction is the point: "120 g Protein" and "50 g
 * Zucker" are both goals, but hitting the first is success and hitting the second is failure, so a
 * progress bar has to know which way it is being read.
 */
enum class NutrientGoalType {
    /** Aim for the value; over and under are both off-target. */
    EXACT,

    /** Reach at least the value; more is fine. */
    MIN,

    /** Stay at or below the value; going over is the failure case. */
    MAX,
}

/** A configured nutrition goal. Absence of one (a null [NutrientGoal]) means "not tracked". */
data class NutrientGoal(
    val value: Double,
    val type: NutrientGoalType = NutrientGoalType.EXACT,
) {
    /**
     * Whether [consumed] has met this goal. A [NutrientGoalType.MAX] goal counts as met while you're
     * still under it; [NutrientGoalType.EXACT] allows a 5 % band either side, since hitting a
     * nutrient target to the gram is not a thing that happens.
     */
    fun isMetBy(consumed: Double): Boolean = when (type) {
        NutrientGoalType.MIN -> consumed >= value
        NutrientGoalType.MAX -> consumed <= value
        NutrientGoalType.EXACT -> value > 0.0 && consumed >= value * 0.95 && consumed <= value * 1.05
    }

    /** True only for a [NutrientGoalType.MAX] goal that has been blown — the one "bad" state. */
    fun isExceededBy(consumed: Double): Boolean = type == NutrientGoalType.MAX && consumed > value

    /** Progress toward the value, clamped to 0..1 for a bar's fill. */
    fun fractionOf(consumed: Double): Float =
        if (value <= 0.0) 0f else (consumed / value).toFloat().coerceIn(0f, 1f)
}

/** The nutrients a daily goal can be set for. Order is the order they're shown in. */
enum class Nutrient(val label: String, val unit: String) {
    KCAL("Kalorien", "kcal"),
    PROTEIN("Protein", "g"),
    CARBS("Kohlenhydrate", "g"),
    FAT("Fett", "g"),
    SATURATED_FAT("Gesättigte Fettsäuren", "g"),
    SUGAR("Zucker", "g"),
    FIBER("Ballaststoffe", "g"),
    SALT("Salz", "g"),
}

/** Label for a goal type as shown in the Ziele screen's selector. */
fun NutrientGoalType.label(): String = when (this) {
    NutrientGoalType.EXACT -> "genau"
    NutrientGoalType.MIN -> "mindestens"
    NutrientGoalType.MAX -> "höchstens"
}
