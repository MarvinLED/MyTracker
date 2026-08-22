package com.example.mytracker.goals

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.metrics.EpochDayRange
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Soll line's shape. Getting this wrong would draw a target the user never had, which is worse
 * than drawing none — so each fallback is pinned separately.
 */
class NutrientGoalTimelineTest {
    private fun change(effectiveFrom: Long, min: Double? = null, max: Double? = null) =
        NutrientGoalChange(
            id = "change-$effectiveFrom-$min-$max",
            nutrient = Nutrient.KCAL,
            effectiveFromEpochDay = effectiveFrom,
            minValue = min,
            maxValue = max,
            changedAt = Instant.ofEpochMilli(1_700_000_000_000),
        )

    private val range = EpochDayRange(startInclusive = 10, endInclusive = 14)

    @Test
    fun withoutAnyHistory_theCurrentGoalRunsFlatAcrossTheRange() {
        val points = nutrientGoalTimeline(range, changes = emptyList(), currentGoal = NutrientGoal(min = 2000.0))

        assertEquals(listOf(10L, 11L, 12L, 13L, 14L), points.map { it.epochDay })
        assertTrue(points.all { it.value == 2000.0 })
    }

    @Test
    fun withoutHistoryAndWithoutGoal_thereIsNothingToDraw() {
        assertEquals(emptyList<Long>(), nutrientGoalTimeline(range, emptyList(), null).map { it.epochDay })
    }

    @Test
    fun aChangeStepsTheLineOnItsEffectiveDay() {
        val changes = listOf(change(0, min = 2000.0), change(12, min = 2500.0))

        val points = nutrientGoalTimeline(range, changes, currentGoal = NutrientGoal(min = 2500.0))

        // The step lands on the effective day itself, not the day after it.
        assertEquals(listOf(2000.0, 2000.0, 2500.0, 2500.0, 2500.0), points.map { it.value })
    }

    @Test
    fun severalChangesEachTakeEffectInTurn() {
        val changes = listOf(change(0, min = 1800.0), change(11, min = 2000.0), change(13, min = 2200.0))

        val points = nutrientGoalTimeline(range, changes, currentGoal = NutrientGoal(min = 2200.0))

        assertEquals(listOf(1800.0, 2000.0, 2000.0, 2200.0, 2200.0), points.map { it.value })
    }

    @Test
    fun beforeTheOldestChange_thatOldestValueIsExtendedBackwards() {
        // Nothing is known about day 10-11, and inventing today's target for them would claim a
        // history the log does not have. The oldest recorded value is the honest stand-in.
        val changes = listOf(change(12, min = 2500.0))

        val points = nutrientGoalTimeline(range, changes, currentGoal = NutrientGoal(min = 9999.0))

        assertEquals(listOf(2500.0, 2500.0, 2500.0, 2500.0, 2500.0), points.map { it.value })
    }

    @Test
    fun aClearedGoalLeavesAGapRatherThanAZero() {
        val changes = listOf(change(0, min = 2000.0), change(13, min = null, max = null))

        val points = nutrientGoalTimeline(range, changes, currentGoal = null)

        // Days 13 and 14 had no target at all; a 0 there would read as "aimed at nothing".
        assertEquals(listOf(10L, 11L, 12L), points.map { it.epochDay })
    }

    @Test
    fun theLowerBoundWinsOverTheUpper() {
        // The deliberate mirror image of NutrientGoal.barTarget, which the diary's bars use.
        val changes = listOf(change(0, min = 100.0, max = 150.0))

        val points = nutrientGoalTimeline(range, changes, currentGoal = null)

        assertTrue(points.all { it.value == 100.0 })
    }

    @Test
    fun anEmptyRangeYieldsNothing() {
        val inverted = EpochDayRange(startInclusive = 14, endInclusive = 10)

        assertEquals(emptyList<Long>(), nutrientGoalTimeline(inverted, emptyList(), NutrientGoal(min = 1.0)).map { it.epochDay })
    }
}
