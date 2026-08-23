package com.example.mytracker.fitness

import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.GoalPeriod

/**
 * How far back a Steigerung looks for something to be measured against. Eight weeks or eight months
 * is long enough to carry a holiday, an injury or a deload, and short enough that what it finds is
 * still the same training: comparing October against last February would not be a Steigerung, it
 * would be a different phase of life.
 */
const val MAX_REFERENCE_PERIODS_BACK = 8

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
 * The whole period [periodsBack] before the current one — a full week Monday to Sunday, a full
 * calendar month. Never a part of one: comparing three days of this week against a full week gone
 * by would report a fall every Wednesday.
 */
fun periodBefore(period: GoalPeriod, today: Long, periodsBack: Int = 1): LongRange {
    require(periodsBack >= 1) { "periodsBack must be at least 1" }
    var end = currentPeriod(period, today).first - 1
    repeat(periodsBack - 1) { end = periodStart(period, end) - 1 }
    return periodStart(period, end)..end
}

private fun periodStart(period: GoalPeriod, day: Long): Long = when (period) {
    GoalPeriod.WEEKLY -> DateUtils.startOfWeekEpochDay(day)
    GoalPeriod.MONTHLY -> DateUtils.startOfMonthEpochDay(day)
    GoalPeriod.DAILY -> day
}

/**
 * The last day of the period [today] falls in — what "noch 3 Tage" is counted to. The current window
 * ends at today because nothing later has happened yet; the *deadline* is this.
 */
fun periodEndDay(period: GoalPeriod, today: Long): Long = when (period) {
    GoalPeriod.WEEKLY -> DateUtils.startOfWeekEpochDay(today) + 6
    GoalPeriod.MONTHLY -> DateUtils.localDateOfEpochDay(today)
        .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
        .toEpochDay()
    GoalPeriod.DAILY -> today
}

/** Kept for the callers that only want the period right before this one. */
fun previousPeriod(period: GoalPeriod, today: Long): LongRange = periodBefore(period, today, 1)

/**
 * One goal's standing in its current period.
 *
 * A Steigerung has two ways of being neither met nor missed, and both have to be sayable:
 * [isPaused] — nothing was trained in this scope at all, so there is nothing to judge and a deload
 * week does not count as a failure — and [hasReference] false, when there is no earlier period to
 * measure against. Reporting either as "0 von 5 kg" would be a red bar for having rested.
 */
data class FitnessGoalProgress(
    val value: Double,
    val target: Double,
    val isPercent: Boolean = false,
    val isPaused: Boolean = false,
    val hasReference: Boolean = true,
    /** 1 = the period right before; more when empty ones were skipped. 0 for a goal that has no reference. */
    val referencePeriodsBack: Int = 0,
) {
    val isMet: Boolean get() = !isPaused && hasReference && target > 0 && value >= target

    val fraction: Float
        get() = if (isPaused || !hasReference || target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
}

/**
 * The gain and the reference behind it, resolved over possibly several periods back — the one rule
 * every Steigerung follows, in one place, whatever the scope's queries happen to be.
 *
 * [trainedIn] decides whether a period counts, not the value: a bodyweight week carries no volume at
 * all, and reading that zero as "not trained" would put Klimmzüge on the same footing as a week on
 * the sofa. Untrained periods are skipped rather than counted as zero — a deload week is not a
 * collapse in volume, and treating it as one both breaks the run of met weeks and hands the week
 * after it a gain it did not earn.
 */
suspend fun increaseAgainstLastTrainedPeriod(
    currentValue: Double,
    currentTrained: Boolean,
    target: Double,
    isPercent: Boolean,
    maxPeriodsBack: Int = MAX_REFERENCE_PERIODS_BACK,
    trainedIn: suspend (Int) -> Boolean,
    valueIn: suspend (Int) -> Double,
): FitnessGoalProgress {
    if (!currentTrained) {
        return FitnessGoalProgress(
            value = 0.0,
            target = target,
            isPercent = isPercent,
            isPaused = true,
            hasReference = false,
        )
    }
    for (back in 1..maxPeriodsBack) {
        if (!trainedIn(back)) continue
        val reference = valueIn(back)
        // A reference of zero has no percentage: "unendlich viel mehr als nichts" is not a number
        // anyone trains towards, so the goal reads as having no comparison rather than as met.
        if (isPercent && reference <= 0.0) break
        return FitnessGoalProgress(
            value = if (isPercent) (currentValue - reference) / reference * 100.0 else currentValue - reference,
            target = target,
            isPercent = isPercent,
            referencePeriodsBack = back,
        )
    }
    return FitnessGoalProgress(
        value = 0.0,
        target = target,
        isPercent = isPercent,
        hasReference = false,
    )
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
    /** What the target works out to today — for a relative goal that moves with the body weight. */
    val targetKg: Double,
    val expectedKg: Double,
    val remainingKg: Double,
    val daysRemaining: Long,
    val isReached: Boolean,
    val isOnTrack: Boolean,
    /** Top set ÷ body weight, null until both are known. The point of a relative goal. */
    val relativeStrength: Double?,
    /** How much of the way from the starting weight to the target is done, for a bar. */
    val fraction: Float,
)

/**
 * What [StrengthMaxWeightGoal.targetBodyweightMultiple] works out to at [bodyWeightKg] — or the
 * stored absolute target when the goal is not a relative one, or no weight has ever been logged.
 */
fun StrengthMaxWeightGoal.effectiveTargetKg(bodyWeightKg: Double?): Double =
    targetBodyweightMultiple?.let { multiple -> bodyWeightKg?.let { it * multiple } } ?: targetWeightKg

fun maxWeightGoalProgress(
    goal: StrengthMaxWeightGoal,
    currentMaxKg: Double?,
    bodyWeightKg: Double? = null,
    today: Long = DateUtils.todayEpochDay(),
): MaxWeightGoalProgress {
    val current = currentMaxKg ?: goal.startWeightKg
    val targetKg = goal.effectiveTargetKg(bodyWeightKg)
    val totalDays = (goal.targetEpochDay - goal.startEpochDay).coerceAtLeast(1)
    val elapsed = (today - goal.startEpochDay).coerceIn(0, totalDays)
    val gain = targetKg - goal.startWeightKg
    val expected = goal.startWeightKg + gain * (elapsed.toDouble() / totalDays)
    val isReached = current >= targetKg
    return MaxWeightGoalProgress(
        currentKg = currentMaxKg,
        targetKg = targetKg,
        expectedKg = expected,
        remainingKg = (targetKg - current).coerceAtLeast(0.0),
        daysRemaining = goal.targetEpochDay - today,
        isReached = isReached,
        // Reached counts as on track even past the date: the goal was met, and a plan that keeps
        // scolding after the target was hit is measuring the calendar rather than the lifting.
        isOnTrack = isReached || current >= expected,
        relativeStrength = currentMaxKg?.let { max -> bodyWeightKg?.takeIf { it > 0 }?.let { max / it } },
        fraction = if (gain <= 0.0) {
            if (isReached) 1f else 0f
        } else {
            ((current - goal.startWeightKg) / gain).toFloat().coerceIn(0f, 1f)
        },
    )
}

/**
 * The plan as two points, for drawing it into the exercise's chart: where the goal started and what
 * it is due to be, each clipped to the window the chart is showing. Null when the window and the
 * plan do not overlap at all — a Soll line outside its own timeframe would be a straight line
 * through data it says nothing about.
 *
 * Clipped rather than drawn whole on purpose: the target date is usually months past the last
 * training day, and letting it stretch the x axis would squeeze the actual history into a corner.
 */
fun maxWeightGoalPlanPoints(
    goal: StrengthMaxWeightGoal,
    targetKg: Double,
    windowStart: Long,
    windowEnd: Long,
): List<Pair<Long, Double>>? {
    val from = maxOf(goal.startEpochDay, windowStart)
    val to = minOf(goal.targetEpochDay, windowEnd)
    if (from >= to) return null
    val totalDays = (goal.targetEpochDay - goal.startEpochDay).coerceAtLeast(1)
    fun valueAt(day: Long): Double {
        val elapsed = (day - goal.startEpochDay).coerceIn(0, totalDays)
        return goal.startWeightKg + (targetKg - goal.startWeightKg) * (elapsed.toDouble() / totalDays)
    }
    return listOf(from to valueAt(from), to to valueAt(to))
}
