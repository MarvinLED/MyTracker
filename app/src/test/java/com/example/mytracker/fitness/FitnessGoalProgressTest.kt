package com.example.mytracker.fitness

import com.example.mytracker.core.util.GoalPeriod
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        bodyweightMultiple: Double? = null,
    ) = StrengthMaxWeightGoal(
        targetBodyweightMultiple = bodyweightMultiple,
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

    @Test
    fun periodsFurtherBackAreWholeOnesToo() {
        // Three weeks back from a Wednesday is still Monday to Sunday, not a rolling 21 days.
        val range = periodBefore(GoalPeriod.WEEKLY, day("2026-08-19"), periodsBack = 3)

        assertEquals(day("2026-07-27"), range.first)
        assertEquals(day("2026-08-02"), range.last)
    }

    @Test
    fun periodEndIsTheDeadlineNotToday() {
        assertEquals(day("2026-08-23"), periodEndDay(GoalPeriod.WEEKLY, day("2026-08-19")))
        assertEquals(day("2026-02-28"), periodEndDay(GoalPeriod.MONTHLY, day("2026-02-05")))
        assertEquals(day("2026-08-19"), periodEndDay(GoalPeriod.DAILY, day("2026-08-19")))
    }

    @Test
    fun anUntrainedPeriodIsPausedRatherThanFailed() = runBlocking {
        val progress = increaseAgainstLastTrainedPeriod(
            currentValue = 0.0,
            currentTrained = false,
            target = 300.0,
            isPercent = false,
            trainedIn = { true },
            valueIn = { 1000.0 },
        )

        // A deload week is not a collapse in volume, and a red bar for having rested is exactly the
        // signal that makes people stop trusting the goal.
        assertTrue(progress.isPaused)
        assertFalse(progress.isMet)
        assertEquals(0f, progress.fraction, 0.0001f)
    }

    @Test
    fun emptyPeriodsAreSkippedWhenLookingForSomethingToBeat() = runBlocking {
        // Trained this week and three weeks ago; the two weeks between were a break.
        val progress = increaseAgainstLastTrainedPeriod(
            currentValue = 1400.0,
            currentTrained = true,
            target = 200.0,
            isPercent = false,
            trainedIn = { back -> back == 3 },
            valueIn = { 1000.0 },
        )

        // Counting the empty weeks as zero volume would hand this week a gain of 1400 kg it did not
        // earn, and tick the goal off for coming back from a holiday.
        assertEquals(400.0, progress.value, 0.0001)
        assertEquals(3, progress.referencePeriodsBack)
        assertTrue(progress.isMet)
    }

    @Test
    fun nothingToCompareAgainstIsSaidRatherThanCountedAsZero() = runBlocking {
        val progress = increaseAgainstLastTrainedPeriod(
            currentValue = 1400.0,
            currentTrained = true,
            target = 200.0,
            isPercent = false,
            trainedIn = { false },
            valueIn = { 0.0 },
        )

        assertFalse(progress.hasReference)
        assertFalse(progress.isMet)
        assertFalse(progress.isPaused)
    }

    @Test
    fun percentIsMeasuredAgainstTheReferencePeriod() = runBlocking {
        val progress = increaseAgainstLastTrainedPeriod(
            currentValue = 1100.0,
            currentTrained = true,
            target = 5.0,
            isPercent = true,
            trainedIn = { it == 1 },
            valueIn = { 1000.0 },
        )

        // +2,5 kg is a different demand on Kreuzheben than on Seitheben; a percentage scales with
        // whatever the lift already is.
        assertEquals(10.0, progress.value, 0.0001)
        assertTrue(progress.isPercent)
        assertTrue(progress.isMet)
    }

    @Test
    fun aReferenceOfZeroHasNoPercentage() = runBlocking {
        val progress = increaseAgainstLastTrainedPeriod(
            currentValue = 1100.0,
            currentTrained = true,
            target = 5.0,
            isPercent = true,
            // A bodyweight-only week: trained, but no volume at all to be a percentage of.
            trainedIn = { true },
            valueIn = { 0.0 },
        )

        assertFalse(progress.hasReference)
        assertFalse(progress.isMet)
    }

    @Test
    fun aRelativeTargetMovesWithTheBodyWeight() {
        val relative = goal(target = 0.0, bodyweightMultiple = 1.5)

        val heavier = maxWeightGoalProgress(relative, currentMaxKg = 110.0, bodyWeightKg = 80.0, today = day("2026-07-02"))
        val lighter = maxWeightGoalProgress(relative, currentMaxKg = 110.0, bodyWeightKg = 70.0, today = day("2026-07-02"))

        // 1,5 × 80 kg is not reached at 110 kg; 1,5 × 70 kg is. Dropping five kilos really does lower
        // the bar, and a goal that ignored that would quietly turn into a heavier one.
        assertEquals(120.0, heavier.targetKg, 0.0001)
        assertFalse(heavier.isReached)
        assertEquals(105.0, lighter.targetKg, 0.0001)
        assertTrue(lighter.isReached)
        assertEquals(1.375, heavier.relativeStrength!!, 0.001)
    }

    @Test
    fun aRelativeTargetFallsBackToItsStoredKilosWithoutABodyWeight() {
        val relative = goal(target = 120.0, bodyweightMultiple = 1.5)

        val progress = maxWeightGoalProgress(relative, currentMaxKg = 110.0, bodyWeightKg = null, today = day("2026-07-02"))

        // Multiplying by a body weight nobody logged would make the target zero kilos — i.e. reached.
        assertEquals(120.0, progress.targetKg, 0.0001)
        assertFalse(progress.isReached)
        assertNull(progress.relativeStrength)
    }

    @Test
    fun thePlanIsClippedToTheWindowTheChartShows() {
        val plan = maxWeightGoalPlanPoints(
            goal(),
            targetKg = 100.0,
            windowStart = day("2026-04-02"),
            windowEnd = day("2026-10-01"),
        )!!

        // Two points, both inside the window: letting the line run to a target date months past the
        // last training day would stretch the x axis and squeeze the history into a corner.
        assertEquals(2, plan.size)
        assertEquals(day("2026-04-02"), plan.first().first)
        assertEquals(day("2026-10-01"), plan.last().first)
        assertTrue(plan.first().second in 80.0..100.0)
        assertTrue(plan.last().second > plan.first().second)
    }

    @Test
    fun aPlanOutsideTheWindowIsNotDrawnAtAll() {
        assertNull(
            maxWeightGoalPlanPoints(
                goal(),
                targetKg = 100.0,
                windowStart = day("2027-06-01"),
                windowEnd = day("2027-12-01"),
            ),
        )
    }

    /** Shorthand for the three answers a period can give. */
    private fun met(target: Double = 5.0) = FitnessGoalProgress(value = target, target = target, referencePeriodsBack = 1)
    private fun missed(target: Double = 5.0) = FitnessGoalProgress(value = 0.0, target = target, referencePeriodsBack = 1)
    private fun paused(target: Double = 5.0) =
        FitnessGoalProgress(value = 0.0, target = target, isPaused = true, hasReference = false)

    @Test
    fun aStreakCountsOnlyTheFinishedPeriodsItCouldJudge() = runBlocking {
        val results = listOf(met(), met(), missed(), met(), paused(), met(), met(), missed())

        val streak = goalStreak(periods = 8) { back -> results[back - 1] }

        // Seven judged, one paused — and the paused week is not counted as missed.
        assertEquals(5, streak.met)
        assertEquals(7, streak.considered)
        assertEquals(1, streak.paused)
    }

    @Test
    fun aPauseDoesNotBreakTheRun() = runBlocking {
        // Newest first: met, met, paused, met — the pause sits inside the run.
        val results = listOf(met(), met(), paused(), met(), missed())

        val streak = goalStreak(periods = 5) { back -> results[back - 1] }

        // Resetting the run for a deload week would punish exactly the weeks a plan is meant to have.
        assertEquals(3, streak.currentRun)
    }

    @Test
    fun aMissedPeriodEndsTheRunButNotTheCount() = runBlocking {
        val results = listOf(met(), missed(), met(), met())

        val streak = goalStreak(periods = 4) { back -> results[back - 1] }

        assertEquals(1, streak.currentRun)
        assertEquals(3, streak.met)
        assertEquals(4, streak.considered)
    }

    @Test
    fun periodsWithNothingToCompareAgainstAreLeftOutEntirely() = runBlocking {
        val noReference = FitnessGoalProgress(value = 0.0, target = 5.0, hasReference = false)

        val streak = goalStreak(periods = 3) { noReference }

        // Neither met nor missed: a period that had nothing to beat cannot fail at beating it.
        assertEquals(0, streak.met)
        assertEquals(0, streak.considered)
        assertEquals(0, streak.paused)
        assertFalse(streak.hasHistory)
    }

    @Test
    fun aStreakSaysNothingUntilThereAreTwoPeriodsToLookBackOver() {
        assertNull(FitnessGoalStreak(met = 1, considered = 1, paused = 0, currentRun = 1).summaryText(GoalPeriod.WEEKLY))
        assertEquals(
            "6 von 8 Wochen erreicht · 3 in Folge · 1 pausiert",
            FitnessGoalStreak(met = 6, considered = 8, paused = 1, currentRun = 3).summaryText(GoalPeriod.WEEKLY),
        )
        // A run of one is just "the last one" and claims nothing worth a clause of its own.
        assertEquals(
            "2 von 4 Monaten erreicht",
            FitnessGoalStreak(met = 2, considered = 4, paused = 0, currentRun = 1).summaryText(GoalPeriod.MONTHLY),
        )
    }
}
