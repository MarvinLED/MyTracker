package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.core.ui.ChartLineStyle
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
    fun aNutrientsLinesShareItsHueAndAreToldApartByTheStroke() {
        val result = lines(
            setOf(
                DiaryHistorySeries.KCAL_GOAL,
                DiaryHistorySeries.KCAL_ACTUAL,
                DiaryHistorySeries.KCAL_AVERAGE,
            ),
        )
        val (goal, actual, average) = result

        assertEquals(goal.color, actual.color)
        assertEquals(goal.color, average.color)
        assertEquals(
            listOf(ChartLineStyle.DASHED, ChartLineStyle.SOLID, ChartLineStyle.DOTTED),
            result.map { it.style },
        )
    }

    @Test
    fun onlyTheMeasuredLineGetsADotPerDay() {
        val result = lines(
            setOf(
                DiaryHistorySeries.KCAL_GOAL,
                DiaryHistorySeries.KCAL_ACTUAL,
                DiaryHistorySeries.KCAL_AVERAGE,
            ),
        )

        assertEquals(listOf(false, true, false), result.map { it.markers })
    }

    @Test
    fun theAverageIsTheWeeksMeanHeldAcrossItsDays() {
        // Epoch day 10 is a Sunday: it closes one week on its own, 11 and 12 open the next.
        val result = lines(setOf(DiaryHistorySeries.KCAL_AVERAGE))

        val points = result.single().points
        assertEquals(listOf(10L, 11L, 12L), points.map { it.epochDay })
        assertEquals(listOf(2000.0, 2000.0, 2000.0), points.map { it.value })
    }

    @Test
    fun aWeekWithoutALoggedDayGetsNoAveragePoint() {
        // Only day 11 is logged, so day 10's week — a different one — stays empty rather than 0.
        val result = lines(setOf(DiaryHistorySeries.KCAL_AVERAGE), totals = listOf(day(11, 2400.0)))

        assertEquals(listOf(11L, 12L), result.single().points.map { it.epochDay })
        assertEquals(listOf(2400.0, 2400.0), result.single().points.map { it.value })
    }

    @Test
    fun oneNutrientsLinesShareAScaleButAMixedSelectionDoesNot() {
        assertTrue(
            isSingleNutrientSelection(
                setOf(DiaryHistorySeries.KCAL_GOAL, DiaryHistorySeries.KCAL_AVERAGE),
            ),
        )
        assertFalse(
            isSingleNutrientSelection(
                setOf(DiaryHistorySeries.KCAL_ACTUAL, DiaryHistorySeries.SALT_ACTUAL),
            ),
        )
        // Gewicht is not a nutrient, so it can never share a nutrient's axis.
        assertFalse(
            isSingleNutrientSelection(
                setOf(DiaryHistorySeries.KCAL_ACTUAL, DiaryHistorySeries.WEIGHT),
            ),
        )
        assertFalse(isSingleNutrientSelection(setOf(DiaryHistorySeries.WEIGHT)))
        assertFalse(isSingleNutrientSelection(emptySet()))
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
            goals = mapOf(
                Nutrient.SUGAR to NutrientGoal(max = 50.0),
                Nutrient.SALT to NutrientGoal(max = 6.0),
            ),
        )

        assertEquals(listOf(40.0), result[0].points.map { it.value })
        assertEquals(listOf(5.0), result[1].points.map { it.value })
    }

    @Test
    fun anIstPointNeedsADayWithAGoal() {
        // Zucker is logged but never had a target: there is nothing for the intake to be read
        // against, so the line stays empty rather than implying a shortfall or an excess.
        val result = lines(
            setOf(DiaryHistorySeries.SUGAR_ACTUAL),
            totals = listOf(day(10, 2000.0, sugar = 40.0)),
        )

        assertEquals(emptyList<Double>(), result.single().points.map { it.value })
    }

    @Test
    fun aGoalOfZeroCountsAsNoGoal() {
        val result = lines(
            setOf(DiaryHistorySeries.KCAL_ACTUAL),
            goals = mapOf(Nutrient.KCAL to NutrientGoal(min = 0.0)),
        )

        assertEquals(emptyList<Double>(), result.single().points.map { it.value })
    }

    @Test
    fun aClearedGoalTakesTheIstLineWithIt() {
        // The target was dropped on day 12. Day 12 was still logged, but there is no longer
        // anything for those 1800 kcal to be read against.
        val changes = listOf(
            NutrientGoalChange("seed", Nutrient.KCAL, 0, minValue = 2100.0, changedAt = instant),
            NutrientGoalChange("cleared", Nutrient.KCAL, 12, changedAt = instant),
        )

        val result = lines(
            setOf(DiaryHistorySeries.KCAL_GOAL, DiaryHistorySeries.KCAL_ACTUAL),
            changes = changes,
        )

        val (goal, actual) = result
        assertEquals(listOf(10L, 11L), goal.points.map { it.epochDay })
        assertEquals(listOf(10L, 11L), actual.points.map { it.epochDay })
        assertEquals(listOf(2000.0, 2200.0), actual.points.map { it.value })
    }

    @Test
    fun theAverageIsTheMeanOfTheDaysTheIstLineDraws() {
        // Day 11 keeps its goal, day 12 loses it. The week of 11 and 12 therefore averages 2200
        // alone — not the 2000 the two logged days would make together.
        val changes = listOf(
            NutrientGoalChange("seed", Nutrient.KCAL, 0, minValue = 2100.0, changedAt = instant),
            NutrientGoalChange("cleared", Nutrient.KCAL, 12, changedAt = instant),
        )

        val result = lines(setOf(DiaryHistorySeries.KCAL_AVERAGE), changes = changes)

        val points = result.single().points
        assertEquals(listOf(10L, 11L, 12L), points.map { it.epochDay })
        assertEquals(listOf(2000.0, 2200.0, 2200.0), points.map { it.value })
    }

    @Test
    fun gewichtIsDrawnWithoutAnyGoal() {
        // Gewicht has no Soll anywhere in the app; gating it on one would erase the line entirely.
        val weights = listOf(BodyWeightEntry(id = "w-10", epochDay = 10, weightKg = 80.0, createdAt = instant))

        val result = lines(setOf(DiaryHistorySeries.WEIGHT), weights = weights, goals = emptyMap())

        assertEquals(listOf(80.0), result.single().points.map { it.value })
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
