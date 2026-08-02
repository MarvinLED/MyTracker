package com.example.prokject2_tracker.goals

import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.util.formatCompact
import com.example.prokject2_tracker.fitness.FitnessGoal
import com.example.prokject2_tracker.fitness.FitnessGoalMetric
import com.example.prokject2_tracker.fitness.label
import com.example.prokject2_tracker.fitness.strength.label
import com.example.prokject2_tracker.fluid.FluidType
import com.example.prokject2_tracker.habit.Habit
import com.example.prokject2_tracker.habit.HabitGoal
import com.example.prokject2_tracker.habit.HabitType
import com.example.prokject2_tracker.nutrition.diary.goalTargetLabel

/**
 * One goal of today, reduced to what the screen draws: how far along it is and whether it is met.
 *
 * [fraction] is null for a goal that is not a matter of degree — a Ja/Nein-Habit is done or it is
 * not, and a bar that can only ever be empty or full says less than a Haken does.
 */
data class DayGoalRow(
    val id: String,
    val label: String,
    val valueText: String,
    val isMet: Boolean,
    val fraction: Float?,
)

data class DayGoalSection(val title: String, val rows: List<DayGoalRow>)

data class DayGoalsUiState(val sections: List<DayGoalSection> = emptyList()) {
    val total: Int get() = sections.sumOf { it.rows.size }
    val metCount: Int get() = sections.sumOf { section -> section.rows.count { it.isMet } }
    val isEmpty: Boolean get() = total == 0
}

/**
 * A goal measured in an amount. The bounds do the deciding, so a lower bound ("≥ 100 g Protein")
 * turns green on reaching it and an upper bound ("≤ 50 g Zucker") is green until it is blown.
 */
private fun amountRow(
    id: String,
    label: String,
    consumed: Double,
    goal: NutrientGoal,
    unit: String,
): DayGoalRow {
    val target = goalTargetLabel(goal.min, goal.max)
    val unitSuffix = if (unit.isBlank()) "" else " $unit"
    return DayGoalRow(
        id = id,
        label = label,
        valueText = "${consumed.formatCompact()} / $target$unitSuffix",
        isMet = goal.isMetBy(consumed),
        fraction = goal.fractionOf(consumed),
    )
}

/** Only the nutrients a goal is actually set for, in [Nutrient] order. */
fun nutrientGoalRows(
    goals: Map<Nutrient, NutrientGoal>,
    consumed: Map<Nutrient, Double>,
): List<DayGoalRow> = Nutrient.entries.mapNotNull { nutrient ->
    val goal = goals[nutrient]?.takeUnless { it.isEmpty } ?: return@mapNotNull null
    amountRow(
        id = "nutrient-${nutrient.name}",
        label = nutrient.label,
        consumed = consumed[nutrient] ?: 0.0,
        goal = goal,
        unit = nutrient.unit,
    )
}

/**
 * The daily drinking goal first, then the per-drink goals. The overall one is a lower bound: it is
 * an amount to reach, not a ceiling to stay under.
 */
fun fluidGoalRows(
    dailyGoalMl: Double,
    totalMl: Double,
    types: List<FluidType>,
    totalsByTypeId: Map<String, Double>,
): List<DayGoalRow> {
    val overall = if (dailyGoalMl > 0.0) {
        listOf(
            amountRow(
                id = "fluid-total",
                label = "Flüssigkeit gesamt",
                consumed = totalMl,
                goal = NutrientGoal(min = dailyGoalMl),
                unit = "ml",
            ),
        )
    } else {
        emptyList()
    }
    val perType = types.mapNotNull { type ->
        val goal = NutrientGoal(min = type.dailyGoalMinMl, max = type.dailyGoalMaxMl)
        if (goal.isEmpty) return@mapNotNull null
        amountRow(
            id = "fluid-${type.id}",
            label = type.name,
            consumed = totalsByTypeId[type.id] ?: 0.0,
            goal = goal,
            unit = "ml",
        )
    }
    return overall + perType
}

/**
 * Habits with a goal for today. A Ja/Nein-Habit is the one goal on this screen that gets no bar —
 * [DayGoalRow.fraction] stays null and the screen shows a Haken or a Kreuz instead.
 */
fun habitGoalRows(
    habits: List<Habit>,
    dailyGoalsByHabitId: Map<String, HabitGoal>,
    checkedInHabitIds: Set<String>,
    valuesByHabitId: Map<String, Double>,
): List<DayGoalRow> = habits.mapNotNull { habit ->
    val goal = dailyGoalsByHabitId[habit.id] ?: return@mapNotNull null
    when (habit.type) {
        HabitType.YES_NO -> DayGoalRow(
            id = "habit-${habit.id}",
            label = habit.name,
            valueText = if (habit.id in checkedInHabitIds) "erledigt" else "offen",
            isMet = habit.id in checkedInHabitIds,
            fraction = null,
        )
        HabitType.COUNT, HabitType.DURATION -> amountRow(
            id = "habit-${habit.id}",
            label = habit.name,
            consumed = valuesByHabitId[habit.id] ?: 0.0,
            goal = NutrientGoal(min = goal.targetValue),
            unit = if (habit.type == HabitType.DURATION) "min" else "",
        )
    }
}

/** The name a fitness goal goes by, including what it is scoped to when it is scoped to anything. */
fun FitnessGoal.dayGoalLabel(muscleGroupNames: Map<String, String>): String = when (metric) {
    FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP ->
        "Kraft-Sätze · ${muscleGroupId?.let { muscleGroupNames[it] } ?: "Muskelgruppe"}"
    FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION ->
        "Kraft-Sätze · ${movementDirection?.label() ?: "Bewegungsrichtung"}"
    else -> metric.label()
}

/** Callers pass only the DAILY goals; weekly and monthly ones are not this screen's business. */
fun fitnessGoalRows(
    goals: List<FitnessGoal>,
    progressByGoalId: Map<String, Double>,
    muscleGroupNames: Map<String, String>,
): List<DayGoalRow> = goals.map { goal ->
    amountRow(
        id = "fitness-${goal.id}",
        label = goal.dayGoalLabel(muscleGroupNames),
        consumed = progressByGoalId[goal.id] ?: 0.0,
        goal = NutrientGoal(min = goal.targetValue),
        unit = if (goal.metric == FitnessGoalMetric.CARDIO_DURATION_MINUTES) "min" else "",
    )
}
