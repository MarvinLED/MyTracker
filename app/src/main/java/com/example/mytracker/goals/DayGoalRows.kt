package com.example.mytracker.goals

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.util.formatCompact
import com.example.mytracker.fitness.FitnessGoal
import com.example.mytracker.fitness.FitnessGoalMetric
import com.example.mytracker.fitness.FitnessGoalProgress
import com.example.mytracker.fitness.periodEndDay
import com.example.mytracker.fitness.unit
import com.example.mytracker.fitness.valueText
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.core.util.label
import com.example.mytracker.fitness.label
import com.example.mytracker.fitness.strength.label
import com.example.mytracker.fluid.FluidType
import com.example.mytracker.habit.Habit
import com.example.mytracker.habit.HabitGoal
import com.example.mytracker.habit.HabitType
import com.example.mytracker.nutrition.diary.goalTargetLabel
import com.example.mytracker.sleep.SleepEntry
import com.example.mytracker.sleep.sleepGoalStatuses
import com.example.mytracker.task.TaskStatus
import com.example.mytracker.task.dueLabel
import com.example.mytracker.task.dueToday

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

data class DayGoalsUiState(
    val sections: List<DayGoalSection> = emptyList(),
    /** "Gestern +34 Punkte · Figur Stufe 18" — the trailer for the Erfolge screen, null until a day
     * has been settled there. */
    val figureSummary: String? = null,
) {
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

/**
 * The tasks today has to answer for: everything still owed, plus what was ticked off today so the
 * row does not vanish the moment it is done (and take the day's count down with it).
 *
 * A task is not a matter of degree, so like a Ja/Nein-Habit it gets no bar — [DayGoalRow.fraction]
 * stays null and the screen draws a Haken or a Kreuz.
 */
fun taskRows(statuses: List<TaskStatus>): List<DayGoalRow> = statuses.dueToday().map { status ->
    DayGoalRow(
        id = "task-${status.task.id}",
        label = status.task.name,
        valueText = status.openDueDay?.let { dueLabel(it, status.today) } ?: "erledigt",
        isMet = !status.isOpen,
        fraction = null,
    )
}

/** The name a fitness goal goes by, including what it is scoped to when it is scoped to anything. */
fun FitnessGoal.dayGoalLabel(
    muscleGroupNames: Map<String, String>,
    exerciseNames: Map<String, String> = emptyMap(),
): String = when (metric) {
    FitnessGoalMetric.STRENGTH_SETS_MUSCLE_GROUP ->
        "Kraft-Sätze · ${muscleGroupId?.let { muscleGroupNames[it] } ?: "Muskelgruppe"}"
    FitnessGoalMetric.STRENGTH_SETS_MOVEMENT_DIRECTION ->
        "Kraft-Sätze · ${movementDirection?.label() ?: "Bewegungsrichtung"}"
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MUSCLE_GROUP ->
        "${metric.label()} · ${muscleGroupId?.let { muscleGroupNames[it] } ?: "Muskelgruppe"}"
    FitnessGoalMetric.STRENGTH_VOLUME_INCREASE_MOVEMENT_DIRECTION ->
        "${metric.label()} · ${movementDirection?.label() ?: "Bewegungsrichtung"}"
    FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE, FitnessGoalMetric.STRENGTH_VOLUME_INCREASE ->
        "${metric.label()} · ${exerciseId?.let { exerciseNames[it] } ?: "Übung"}"
    else -> metric.label()
}

/** "noch 3 Tage" — how long is left to do something about a goal that does not end today. */
fun remainingDaysLabel(daysLeft: Long): String? = when {
    daysLeft <= 0 -> "letzter Tag"
    daysLeft == 1L -> "noch 1 Tag"
    else -> "noch $daysLeft Tage"
}

/**
 * The fitness goals of the periods running *right now* — the week and the month included, not only
 * the day. A weekly goal is the one people actually set, and one that is only visible on the Fitness
 * screen is one nobody sees until the week is over; with the days left beside it, it is still
 * something today can be spent on.
 */
fun fitnessGoalRows(
    goals: List<FitnessGoal>,
    progressByGoalId: Map<String, FitnessGoalProgress>,
    muscleGroupNames: Map<String, String>,
    exerciseNames: Map<String, String> = emptyMap(),
    today: Long = DateUtils.todayEpochDay(),
): List<DayGoalRow> = goals
    .sortedWith(compareBy({ it.period.ordinal }, { it.metric.ordinal }))
    .map { goal ->
        val progress = progressByGoalId[goal.id]
            ?: FitnessGoalProgress(value = 0.0, target = goal.targetValue, isPercent = goal.isPercent)
        // Only for the periods that outlive today: a Tagesziel ends when the day does, and telling
        // someone it is the last day of today says nothing.
        val remaining = if (goal.period == GoalPeriod.DAILY) {
            null
        } else {
            remainingDaysLabel(periodEndDay(goal.period, today) - today)
        }
        DayGoalRow(
            id = "fitness-${goal.id}",
            label = "${goal.dayGoalLabel(muscleGroupNames, exerciseNames)} · ${goal.period.label()}",
            valueText = listOfNotNull(progress.valueText(goal.unit()), remaining).joinToString(" · "),
            isMet = progress.isMet,
            fraction = progress.fraction,
        )
    }

/**
 * Last night against the sleep goals. The rows are built in the sleep module — the bedtime's
 * across-midnight rule belongs next to the data, not here — and only reshaped for this screen.
 *
 * A night that isn't logged still shows: "0 h von mind. 7 h" is exactly the nag this screen is for.
 */
fun sleepGoalRows(
    entry: SleepEntry?,
    durationGoalMinutes: NutrientGoal?,
    bedtimeGoalMinuteOfDay: Int?,
): List<DayGoalRow> =
    sleepGoalStatuses(entry, durationGoalMinutes, bedtimeGoalMinuteOfDay).map { status ->
        DayGoalRow(
            id = "sleep-${status.label}",
            label = status.label,
            valueText = status.valueText,
            isMet = status.isMet,
            fraction = status.fraction,
        )
    }
