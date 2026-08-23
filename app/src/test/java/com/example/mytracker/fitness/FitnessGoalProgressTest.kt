package com.example.mytracker.fitness

import com.example.mytracker.core.util.GoalPeriod
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The period arithmetic behind the Steigerungen, and the "auf Kurs" line of a long-term goal. Both
 * are pure functions of a day so they can be checked without a clock — and both are the kind of
 * arithmetic that is wrong by exactly one day until someone writes the case down.
 */
class FitnessGoalProgressTest {
    private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

    @Test
    fun currentWeekRunsFromMondayToToday() {
        // 2026-08-19 is a Wednesday.
        val range = currentPeriod(GoalPeriod.WEEKLY, day("2026-08-19"))

        assertEquals(day("2026-08-17"), range.first)
        assertEquals(day("2026-08-19"), range.last)
    }

    @Test
    fun previousWeekIsAWholeOne() {
        val range = previousPeriod(GoalPeriod.WEEKLY, day("2026-08-19"))

        // Monday to Sunday, and it ends the day before the current week starts. Comparing three days
        // of this week against a full week gone by would report a fall every Wednesday.
        assertEquals(day("2026-08-10"), range.first)
        assertEquals(day("2026-08-16"), range.last)
        assertEquals(7, range.last - range.first + 1)
    }

    @Test
    fun previousMonthIsTheWholeCalendarMonthBefore() {
        val range = previousPeriod(GoalPeriod.MONTHLY, day("2026-03-05"))

        // February, leap year included — the month is read off the calendar, not as "30 days back".
        assertEquals(day("2026-02-01"), range.first)
        assertEquals(day("2026-02-28"), range.last)
    }

    @Test
    fun previousMonthCrossesTheYear() {
        val range = previousPeriod(GoalPeriod.MONTHLY, day("2026-01-15"))

        assertEquals(day("2025-12-01"), range.first)
        assertEquals(day("2025-12-31"), range.last)
    }

    private fun goal(
        target: Double = 100.0,
        targetDay: String = "2026-12-31",
        start: Double = 80.0,
        startDay: String = "2026-01-01",
    ) = StrengthMaxWeightGoal(
        id = "maxweight-bench",
        exerciseId = "bench",
        targetWeightKg = target,
        targetEpochDay = day(targetDay),
        startWeightKg = start,
        startEpochDay = day(startDay),
        createdAt = Instant.EPOCH,
    )

    @Test
    fun theExpectedWeightFollowsTheStraightLineFromStartToTarget() {
        // 2026-07-02 is the midpoint of the year: half the gain is due by then.
        val progress = maxWeightGoalProgress(goal(), currentMaxKg = 90.0, today = day("2026-07-02"))

        assertEquals(90.0, progress.expectedKg, 0.2)
        assertTrue(progress.isOnTrack)
        assertEquals(10.0, progress.remainingKg, 0.0001)
        assertEquals(0.5f, progress.fraction, 0.01f)
    }

    @Test
    fun behindTheLineIsBehindPlanEvenWhileThereIsTimeLeft() {
        val progress = maxWeightGoalProgress(goal(), currentMaxKg = 84.0, today = day("2026-07-02"))

        // Months before the date, "not reached yet" is no answer; the line is what makes it one.
        assertFalse(progress.isOnTrack)
        assertFalse(progress.isReached)
        assertTrue(progress.daysRemaining > 0)
    }

    @Test
    fun reachedStaysReachedPastTheDate() {
        val progress = maxWeightGoalProgress(goal(), currentMaxKg = 102.5, today = day("2027-03-01"))

        assertTrue(progress.isReached)
        // A plan that keeps scolding after the target was hit is measuring the calendar, not the
        // lifting.
        assertTrue(progress.isOnTrack)
        assertEquals(0.0, progress.remainingKg, 0.0001)
        assertEquals(1f, progress.fraction, 0.0001f)
        assertTrue(progress.daysRemaining < 0)
    }

    @Test
    fun anUntrainedExerciseFallsBackToTheStartingWeight() {
        // Null is "never lifted", not "0 kg": treating it as zero would report a 80 kg shortfall on
        // a goal whose plan has not started moving yet.
        val progress = maxWeightGoalProgress(goal(), currentMaxKg = null, today = day("2026-01-01"))

        assertEquals(80.0, progress.expectedKg, 0.0001)
        assertTrue(progress.isOnTrack)
        assertEquals(0f, progress.fraction, 0.0001f)
    }

    @Test
    fun aTargetAtOrBelowTheStartingWeightDoesNotDivideByZero() {
        val progress = maxWeightGoalProgress(
            goal(target = 80.0, start = 80.0),
            currentMaxKg = 80.0,
            today = day("2026-06-01"),
        )

        assertTrue(progress.isReached)
        assertEquals(1f, progress.fraction, 0.0001f)
    }
}
