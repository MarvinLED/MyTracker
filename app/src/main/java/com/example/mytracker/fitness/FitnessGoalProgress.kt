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
 * There is exactly one way of being neither met nor missed, and it has to be sayable: [hasReference]
 * false, when there is no earlier period to measure a Steigerung against. Everything else is judged
 * — a period without training is a period the goal was not reached in, not a state of its own.
 */
data class FitnessGoalProgress(
    val value: Double,
    val target: Double,
    val isPercent: Boolean = false,
    val hasReference: Boolean = true,
    /** True for a Steigerung — a difference against the period before, which carries a sign. */
    val isIncrease: Boolean = false,
) {
    val isMet: Boolean get() = hasReference && target > 0 && value >= target

    val fraction: Float
        get() = if (!hasReference || target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
}

/**
 * The gain over the period right before this one — the one rule every Steigerung follows, in one
 * place, whatever the scope's queries happen to be.
 *
 * [previousTrained] decides whether there is a comparison at all, and it is the set count and not
 * the value that answers it: a bodyweight week carries no volume, and reading that zero as "not
 * trained" would put Klimmzüge on the same footing as a week on the sofa. A period that was not
 * trained is never skipped over in favour of an older one — a Steigerung is measured against the
 * period immediately before it or against nothing.
 *
 * [currentValue] is simply what this period holds, zero included: no training means no gain, which
 * is a missed goal like any other.
 */
fun increaseAgainstPreviousPeriod(
    currentValue: Double,
    previousTrained: Boolean,
    previousValue: Double,
    target: Double,
    isPercent: Boolean,
): FitnessGoalProgress {
    // A reference of zero has no percentage: "unendlich viel mehr als nichts" is not a number anyone
    // trains towards, so the goal reads as having no comparison rather than as met.
    if (!previousTrained || (isPercent && previousValue <= 0.0)) {
        return FitnessGoalProgress(
            value = 0.0,
            target = target,
            isPercent = isPercent,
            hasReference = false,
        )
    }
    return FitnessGoalProgress(
        value = if (isPercent) {
            (currentValue - previousValue) / previousValue * 100.0
        } else {
            currentValue - previousValue
        },
        target = target,
        isPercent = isPercent,
        isIncrease = true,
    )
}

/** How many finished periods a streak looks back over. Two months of weeks, or two quarters of months. */
const val STREAK_PERIODS = 8

/**
 * How a goal has been going over the periods that are already finished — the current one is left
 * out, since a half-finished week is neither met nor missed.
 *
 * [currentRun] is a run in the strict sense: only periods that were actually met, one after the
 * other. Anything else ends it — a missed period, a period without training, a period there was
 * nothing to compare against. A streak that survived the gaps in between would not be a streak.
 */
data class FitnessGoalStreak(
    val met: Int,
    /** Periods that could be judged at all — the ones with something to measure against. */
    val considered: Int,
    /** Met periods in a row, counted back from the most recent finished one. */
    val currentRun: Int,
    /**
     * The longest run **within the periods that were looked at** — not an all-time record. It is
     * what makes a broken streak leave something behind instead of vanishing: the run is gone, the
     * mark it set is not. How far back "within" reaches is the caller's `periods`, which is why
     * every place that shows this also says how many periods it looked at.
     */
    val bestRun: Int = currentRun,
) {
    val hasHistory: Boolean get() = considered > 0
}

/**
 * Folds one goal's finished periods, newest first, into a streak. [progressForPeriodsBack] is asked
 * for each of them in turn — the caller knows how to evaluate a goal, this knows what the answers
 * add up to.
 */
suspend fun goalStreak(
    periods: Int = STREAK_PERIODS,
    progressForPeriodsBack: suspend (Int) -> FitnessGoalProgress,
): FitnessGoalStreak {
    var met = 0
    var considered = 0
    var run = 0
    var runOpen = true
    // Walking backwards traverses each run from its end rather than its start, which changes nothing
    // about how long it is — so the longest one can be picked up in the same pass.
    var currentSpan = 0
    var bestSpan = 0
    for (back in 1..periods) {
        val progress = progressForPeriodsBack(back)
        // A period with nothing to compare against cannot fail at beating it, so it stays out of
        // "x von y erreicht" — but it is not a met period either, so the run stops there.
        if (progress.hasReference) {
            considered++
            if (progress.isMet) met++
        }
        if (progress.isMet) {
            if (runOpen) run++
            currentSpan++
            if (currentSpan > bestSpan) bestSpan = currentSpan
        } else {
            runOpen = false
            currentSpan = 0
        }
    }
    return FitnessGoalStreak(met = met, considered = considered, currentRun = run, bestRun = bestSpan)
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
