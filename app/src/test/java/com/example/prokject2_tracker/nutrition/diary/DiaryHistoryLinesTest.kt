package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.goals.NutrientGoalChange
import com.example.prokject2_tracker.nutrition.NutritionTotals
import com.example.prokject2_tracker.weight.BodyWeightEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryHistoryLinesTest {
    private val instant = Instant.ofEpochMilli(1_700_000_000_000)
    private val range = EpochDayRange(startInclusive = 10, endInclusive = 12)

    private fun day(epochDay: Long, kcal: Double, sugar: Double = 0.0, salt: Double = 0.0) =
        DailyNutritionTotals(
            epochDay = epochDay,
            totals = NutritionTotals(kcal = kcal, protein = 0.0, carbs = 0.0, fat = 0.0, sugar = sugar, salt = salt),
        )

    private fun lines(
        selected: Set<DiaryHistorySeries>,
        granularity: Granularity = Granularity.DAILY,
        totals: List<DailyNutritionTotals> = listOf(day(10, 2000.0), day(11, 2200.0), day(12, 1800.0)),
        weights: List<BodyWeightEntry> = emptyList(),
        changes: List<NutrientGoalChange> = emptyList(),
        goals: Map<Nutrient, NutrientGoal> = mapOf(Nutrient.KCAL to NutrientGoal(min = 2100.0)),
    ) = diaryHistoryLines(selected, range, granularity, totals, weights, changes, goals)

    @Test
    fun onlySelectedSeriesAreDrawn() {
        val result = lines(setOf(DiaryHistorySeries.KCAL_ACTUAL))

        assertEquals(listOf("Kalorien Ist"), result.map { it.label })
        assertEquals(listOf(2000.0, 2200.0, 1800.0), result.single().points.map { it.value })
    }

    @Test
    fun linesKeepEnumOrderRegardlessOfSelectionOrder() {
        val result = lines(setOf(DiaryHistorySeries.WEIGHT, DiaryHistorySeries.KCAL_GOAL))

        assertEquals(listOf("Kalorien Soll", "Gewicht"), result.map { it.label })
    }

    @Test
    fun aPairSharesItsHueAndIsToldApartByTheDash() {
        val result = lines(setOf(DiaryHistorySeries.KCAL_GOAL, DiaryHistorySeries.KCAL_ACTUAL))
        val (goal, actual) = result

        assertEquals(goal.color, actual.color)
        assertTrue(goal.dashed)
        assertFalse(actual.dashed)
    }

    @Test
    fun theGoalLineFollowsTheRecordedHistory() {
        val changes = listOf(
            NutrientGoalChange("seed", Nutrient.KCAL, 0, minValue = 1800.0, changedAt = instant),
            NutrientGoalChange("bump", Nutrient.KCAL, 12, minValue = 2400.0, changedAt = instant),
        )

        val result = lines(setOf(DiaryHistorySeries.KCAL_GOAL), changes = changes)

        assertEquals(listOf(1800.0, 1800.0, 2400.0), result.single().points.map { it.value })
    }

    @Test
    fun sugarAndSaltReachTheirLines() {
        val totals = listOf(day(10, 2000.0, sugar = 40.0, salt = 5.0))

        val result = lines(
            setOf(DiaryHistorySeries.SUGAR_ACTUAL, DiaryHistorySeries.SALT_ACTUAL),
            totals = totals,
        )

        assertEquals(listOf(40.0), result[0].points.map { it.value })
        assertEquals(listOf(5.0), result[1].points.map { it.value })
    }

    @Test
    fun weightIsNotZeroBased() {
        val weights = listOf(
            BodyWeightEntry(id = "w-10", epochDay = 10, weightKg = 80.0, createdAt = instant),
            BodyWeightEntry(id = "w-11", epochDay = 11, weightKg = 80.4, createdAt = instant),
        )

        val result = lines(setOf(DiaryHistorySeries.WEIGHT, DiaryHistorySeries.KCAL_ACTUAL), weights = weights)

        // A zero-based axis would flatten a 0,4 kg move into a straight edge; the nutrients keep it.
        assertFalse(result.first { it.label == "Gewicht" }.zeroBased)
        assertTrue(result.first { it.label == "Kalorien Ist" }.zeroBased)
    }

    @Test
    fun coarserGranularitiesAverageRatherThanSum() {
        // Epoch day 10 is a Sunday, so it closes one week while 11 and 12 open the next. The second
        // bucket has to come out as the mean of its two days, not their total — a summed 4000 kcal
        // could not be read against a 2100 kcal daily goal at all.
        val result = lines(setOf(DiaryHistorySeries.KCAL_ACTUAL), granularity = Granularity.WEEKLY)

        val points = result.single().points
        assertEquals(listOf(4L, 11L), points.map { it.epochDay })
        assertEquals(listOf(2000.0, 2000.0), points.map { it.value })
    }

    @Test
    fun nothingSelectedDrawsNothing() {
        assertEquals(emptyList<String>(), lines(emptySet()).map { it.label })
    }
}
