package com.example.mytracker.core.datastore

/**
 * A configured nutrition goal: a lower bound, an upper bound, or both at once — the same shape the
 * Getränkearten already use. Absence of one (a null [NutrientGoal]) means "not tracked", and so does
 * a goal with neither bound set.
 *
 * Both bounds together is the case worth having: "100 bis 150 g Protein" is a real target, and
 * expressing it as a single number plus a direction never could.
 */
data class NutrientGoal(
    val min: Double? = null,
    val max: Double? = null,
) {
    /** True once neither bound is set — such a goal is indistinguishable from having none. */
    val isEmpty: Boolean get() = min == null && max == null

    /**
     * The number a bar runs to. The upper bound when there is one, because that is where the bar
     * ends; with only a lower bound, reaching it is what fills the bar.
     */
    val barTarget: Double? get() = max ?: min

    /**
     * The number the Verlauf's "Soll" line follows: the **lower** bound first. Deliberately the
     * mirror image of [barTarget] — a bar runs to the upper bound because that is where it ends,
     * while a trend line is read as "what am I working towards", and with "100 bis 150 g Protein"
     * that is the 100.
     */
    val lineTarget: Double? get() = min ?: max

    /**
     * Where the lower bound sits on a bar that runs to [barTarget], as a fraction of the bar's
     * width. Null when there is nothing to mark — no lower bound, or no upper bound for it to sit
     * inside of.
     */
    val minMarkerFraction: Float?
        get() {
            val lower = min ?: return null
            val upper = max ?: return null
            if (upper <= 0.0) return null
            return (lower / upper).toFloat().coerceIn(0f, 1f)
        }

    /** Whether [consumed] is within both bounds. An unset bound is one that cannot be missed. */
    fun isMetBy(consumed: Double): Boolean =
        !isEmpty && (min == null || consumed >= min) && (max == null || consumed <= max)

    /** True only when the upper bound has been blown — the one "bad" state. */
    fun isExceededBy(consumed: Double): Boolean = max != null && consumed > max

    /** Progress toward [barTarget], clamped to 0..1 for a bar's fill. */
    fun fractionOf(consumed: Double): Float {
        val target = barTarget ?: return 0f
        if (target <= 0.0) return 0f
        return (consumed / target).toFloat().coerceIn(0f, 1f)
    }
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
