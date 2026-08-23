package com.example.mytracker.goals

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.fitness.FitnessGoalChange
import java.time.format.DateTimeFormatter
import java.util.Locale

/** How many entries the Zieländerungs-Historie shows — enough to see a trend, not a logbook. */
const val GOAL_CHANGE_HISTORY_LIMIT = 30

/**
 * The log turned into steps. Rows are grouped per goal to find each one's predecessor, then put
 * back in newest-first order across all goals — which is how anyone reads a history.
 */
fun goalChangeRows(changes: List<FitnessGoalChange>): List<GoalChangeRow> {
    val dateFormatter = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN)
    return changes
        .groupBy { it.goalKey }
        .flatMap { (_, perGoal) ->
            val ordered = perGoal.sortedWith(compareBy({ it.effectiveFromEpochDay }, { it.changedAt }))
            ordered.mapIndexed { index, change ->
                val previous = ordered.getOrNull(index - 1)
                change to previous
            }
        }
        .sortedWith(compareByDescending<Pair<FitnessGoalChange, FitnessGoalChange?>> {
            it.first.effectiveFromEpochDay
        }.thenByDescending { it.first.changedAt })
        .take(GOAL_CHANGE_HISTORY_LIMIT)
        .map { (change, previous) ->
            GoalChangeRow(
                id = change.id,
                label = change.label,
                dateText = DateUtils.localDateOfEpochDay(change.effectiveFromEpochDay).format(dateFormatter),
                changeText = changeText(change, previous),
            )
        }
}

private fun changeText(change: FitnessGoalChange, previous: FitnessGoalChange?): String {
    fun format(value: Double?, isPercent: Boolean): String =
        value?.let { "${it.formatCompact()}${if (isPercent) " %" else ""}" } ?: "kein Ziel"
    val from = previous?.let { format(it.targetValue, it.isPercent) }
    val to = format(change.targetValue, change.isPercent)
    val target = change.targetEpochDay?.let { day ->
        " (bis ${DateUtils.localDateOfEpochDay(day).format(DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.GERMAN))})"
    }.orEmpty()
    // The first entry of a goal has nothing to come from, and "kein Ziel → 300" would be a step
    // that never happened: before it, the goal simply did not exist.
    return if (from == null) "$to gesetzt$target" else "$from → $to$target"
}
