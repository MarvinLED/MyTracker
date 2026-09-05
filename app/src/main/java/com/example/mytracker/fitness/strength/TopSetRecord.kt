package com.example.mytracker.fitness.strength

/**
 * A top set that beat everything this exercise had ever done before it. [previousKg] is null for the
 * very first weighted session — that is a first mark rather than a record broken, and saying "davor
 * 0 kg" would invent a session that never happened.
 */
data class TopSetRecord(val weightKg: Double, val previousKg: Double?)

/**
 * Whether [day] set a new best for this exercise, given every set it has.
 *
 * Strictly greater, like the volume target: repeating an old best is the same lift, not a better
 * one, and a banner that fired on every repeat would stop meaning anything within a week.
 *
 * Measured against **all** earlier days rather than the session before, because that is what a
 * record is — a week off must not lower the bar to whatever came last.
 */
fun topSetRecord(current: SessionStats?, allSets: List<StrengthSet>, day: Long): TopSetRecord? {
    // Bodyweight-only days carry no external weight and therefore no top set to compare.
    val today = current?.maxWeightKg ?: return null
    val before = allSets.filter { it.epochDay < day }.mapNotNull { it.weightKg }.maxOrNull()
    if (before != null && today <= before) return null
    return TopSetRecord(weightKg = today, previousKg = before)
}
