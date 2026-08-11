package com.example.prokject2_tracker.goals

import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricPoint

/** The bounds a recorded change put in force, as the same shape the rest of the app reasons about. */
fun NutrientGoalChange.toGoal(): NutrientGoal = NutrientGoal(min = minValue, max = maxValue)

/**
 * What a nutrient's goal was on each day of [range], as one point per day — the "Soll" line.
 *
 * A point per day rather than one per change so the series buckets weekly/monthly exactly like the
 * Ist series it is read against; both then land on the same x positions and the shared crosshair
 * can answer for both at once.
 *
 * Three cases, in the order they are tried:
 * - a recorded change on or before the day wins — this is the actual history;
 * - before the oldest recorded change, that oldest value is extended backwards. With the seed row
 *   [NutrientGoalHistoryRepository] writes this only bites for days before the log began;
 * - with no history at all, [currentGoal] runs flat across the whole range. That is the honest
 *   shape for a goal that has never been changed since recording started, and the fallback for one
 *   whose earlier changes predate the feature.
 *
 * Days on which the goal was cleared produce **no point**, since there was nothing to aim at.
 *
 * [changes] must be this nutrient's rows only, oldest first.
 */
fun nutrientGoalTimeline(
    range: EpochDayRange,
    changes: List<NutrientGoalChange>,
    currentGoal: NutrientGoal?,
): List<MetricPoint> {
    if (range.endInclusive < range.startInclusive) return emptyList()
    val oldest = changes.firstOrNull()?.toGoal()

    return (range.startInclusive..range.endInclusive).mapNotNull { day ->
        val goal = changes.lastOrNull { it.effectiveFromEpochDay <= day }?.toGoal()
            ?: oldest
            ?: currentGoal
        goal?.lineTarget?.let { MetricPoint(day, it) }
    }
}
