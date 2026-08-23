package com.example.mytracker.fitness

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod

/**
 * The window a period-goal is measured over: from the start of the current week/month up to today.
 * Split out as a pure function of the day so the period arithmetic can be tested without a clock.
 */
fun currentPeriod(period: GoalPeriod, today: Long): LongRange {
    val start = when (period) {
        GoalPeriod.WEEKLY -> DateUtils.startOfWeekEpochDay(today)
        GoalPeriod.MONTHLY -> DateUtils.startOfMonthEpochDay(today)
        GoalPeriod.DAILY -> today
    }
    return start..today
}

/**
 * The window before [currentPeriod] — a whole one, not a part: the previous week runs Monday to
 * Sunday and the previous month first to last. That is what a Steigerung of volume is measured
 * against, and comparing three days of this week against a full week gone by would report a fall
 * every Wednesday.
 */
fun previousPeriod(period: GoalPeriod, today: Long): LongRange {
    val currentStart = currentPeriod(period, today).first
    val previousEnd = currentStart - 1
    val previousStart = when (period) {
        GoalPeriod.WEEKLY -> currentStart - 7
        GoalPeriod.MONTHLY -> DateUtils.startOfMonthEpochDay(previousEnd)
        GoalPeriod.DAILY -> previousEnd
    }
    return previousStart..previousEnd
}

/**
 * How far a long-term max-weight goal has come, and whether that is far enough by now.
 *
 * [expectedKg] is the straight line from where the goal started to where it is due, read off at
 * today — the only honest way to answer "am I on track?" months before the date. It is what makes a
 * goal actionable rather than a number to be surprised by at the deadline.
 */
data class MaxWeightGoalProgress(
    val currentKg: Double?,
    val expectedKg: Double,
    val remainingKg: Double,
    val daysRemaining: Long,
    val isReached: Boolean,
    val isOnTrack: Boolean,
    /** How much of the way from the starting weight to the target is done, for a bar. */
    val fraction: Float,
)

fun maxWeightGoalProgress(
    goal: StrengthMaxWeightGoal,
    currentMaxKg: Double?,
    today: Long = DateUtils.todayEpochDay(),
): MaxWeightGoalProgress {
    val current = currentMaxKg ?: goal.startWeightKg
    val totalDays = (goal.targetEpochDay - goal.startEpochDay).coerceAtLeast(1)
    val elapsed = (today - goal.startEpochDay).coerceIn(0, totalDays)
    val gain = goal.targetWeightKg - goal.startWeightKg
    val expected = goal.startWeightKg + gain * (elapsed.toDouble() / totalDays)
    val isReached = current >= goal.targetWeightKg
    return MaxWeightGoalProgress(
        currentKg = currentMaxKg,
        expectedKg = expected,
        remainingKg = (goal.targetWeightKg - current).coerceAtLeast(0.0),
        daysRemaining = goal.targetEpochDay - today,
        isReached = isReached,
        // Reached counts as on track even past the date: the goal was met, and a plan that keeps
        // scolding after the target was hit is measuring the calendar rather than the lifting.
        isOnTrack = isReached || current >= expected,
        fraction = if (gain <= 0.0) {
            if (isReached) 1f else 0f
        } else {
            ((current - goal.startWeightKg) / gain).toFloat().coerceIn(0f, 1f)
        },
    )
}
