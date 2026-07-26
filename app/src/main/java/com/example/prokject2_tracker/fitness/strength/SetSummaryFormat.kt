package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.formatDecimal
import kotlin.math.roundToLong

/**
 * One set as the entry UI works with it, before it becomes a [StrengthSet] row. A null [weightKg]
 * means bodyweight — deliberately not 0 kg, since "no external weight" and "zero kilos" are
 * different facts and the max-weight series depends on telling them apart.
 */
data class SetDraft(val reps: Int, val weightKg: Double?)

fun StrengthSet.toDraft() = SetDraft(reps = reps, weightKg = weightKg)

/**
 * "50 kg × 5, 5 · 60 kg × 8, 8, 8" — **consecutive** sets of equal weight collapse into one group.
 *
 * Consecutive only, on purpose: 50/60/50 renders as three groups, because the order is the workout.
 * Merging them would claim the lifter did both 50 kg sets back to back.
 *
 * Weights use [formatDecimal] with two places rather than `formatCompact()`, which rounds to one
 * decimal and would turn a 62,25 kg set (reachable via the 0,25 kg steppers) into "62,3 kg".
 * Returns "" for an empty list so callers can supply their own placeholder.
 */
fun formatSetSummary(sets: List<SetDraft>): String {
    if (sets.isEmpty()) return ""
    // Group on the weight in whole grams: null groups with null, and two sets that stepped to the
    // same value can't miss each other over floating-point noise.
    return sets
        .groupConsecutiveBy { weightKey(it.weightKg) }
        .joinToString(" · ") { group ->
            "${weightLabel(group.first().weightKg)} × ${group.joinToString(", ") { it.reps.toString() }}"
        }
}

/** "60 kg", "62,25 kg", or "KG" for a bodyweight set — the label the training list has always used. */
fun weightLabel(weightKg: Double?): String =
    if (weightKg == null) "KG" else "${weightKg.formatDecimal(2)} kg"

/**
 * A session's headline for the exercise list: the heaviest weight and the reps done at it —
 * `70 kg × 5, 5, 5`. Unlike [formatSetSummary] this collects **every** set at that weight, not just
 * adjacent ones: it answers "what did I top out at last time", where dropping to a lighter weight
 * in between doesn't make the heavy sets two separate facts.
 *
 * A bodyweight-only session has no weight to lead with, so it falls back to the best rep count and
 * how often it was hit — `3× 12 Wiederholungen`. Returns null for an empty list.
 */
fun formatTopSets(sets: List<SetDraft>): String? {
    if (sets.isEmpty()) return null
    val maxWeight = maxWeightOf(sets)
    if (maxWeight == null) {
        val maxReps = sets.maxOf { it.reps }
        return "${sets.count { it.reps == maxReps }}× $maxReps Wiederholungen"
    }
    // Compared in whole grams, so a set stepped to 62,25 kg can't miss the max over float noise.
    val topKey = weightKey(maxWeight)
    val reps = sets.filter { weightKey(it.weightKg) == topKey }.joinToString(", ") { it.reps.toString() }
    return "${weightLabel(maxWeight)} × $reps"
}

/** Weight as whole grams, so nulls group with nulls and equal values always compare equal. */
private fun weightKey(weightKg: Double?): Long? = weightKg?.let { (it * 100).roundToLong() }

private fun <T, K> List<T>.groupConsecutiveBy(keyOf: (T) -> K): List<List<T>> {
    val groups = mutableListOf<MutableList<T>>()
    var previousKey: K? = null
    forEach { item ->
        val key = keyOf(item)
        if (groups.isEmpty() || key != previousKey) {
            groups += mutableListOf(item)
        } else {
            groups.last() += item
        }
        previousKey = key
    }
    return groups
}
