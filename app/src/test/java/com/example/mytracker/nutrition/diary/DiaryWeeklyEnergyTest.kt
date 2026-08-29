package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.metrics.EpochDayRange
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.nutrition.NutritionTotals
import com.example.mytracker.weight.BodyWeightEntry
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryWeeklyEnergyTest {
    private val anInstant = Instant.ofEpochMilli(1_700_000_000_000)

    /** Week 0 starts here; week n starts at [week] n. */
    private val firstMonday = DateUtils.startOfWeekEpochDay(LocalDate.parse("2026-06-03").toEpochDay())

    private fun week(index: Int) = firstMonday + 7L * index

    private fun rangeOfWeeks(count: Int) =
        EpochDayRange(startInclusive = firstMonday, endInclusive = week(count) - 1)

    /** [days] logged days of that week, all at [kcal]. */
    private fun kcalWeek(index: Int, kcal: Double, days: Int = 7) =
        (0 until days).map { offset ->
            DailyNutritionTotals(
                epochDay = week(index) + offset,
                totals = NutritionTotals(kcal = kcal, protein = 0.0, carbs = 0.0, fat = 0.0),
            )
        }

    /** Two weigh-ins either side of [mean], so the week's mean is exactly it. */
    private fun weightWeek(index: Int, mean: Double) = listOf(
        BodyWeightEntry("w${index}a", week(index), mean - 0.2, anInstant),
        BodyWeightEntry("w${index}b", week(index) + 3, mean + 0.2, anInstant),
    )

    @Test
    fun aWeekIsMeasuredAgainstThePreviousWeeksMean() {
        val points = weeklyEnergyPoints(
            range = rangeOfWeeks(2),
            nutritionTotals = kcalWeek(0, 2000.0) + kcalWeek(1, 2400.0),
            weights = weightWeek(0, 80.0) + weightWeek(1, 80.5),
        )

        // Mean against mean, not weigh-in against weigh-in: the single readings are 79,8 and 80,7,
        // which would report nearly a kilo of water as a gain.
        val point = points.single()
        assertEquals(week(1), point.weekStart)
        assertEquals(2400.0, point.kcalPerDay, 1e-9)
        assertEquals(0.5, point.weightChangeKg, 1e-9)
        assertEquals(7, point.loggedDays)
    }

    @Test
    fun theFirstCompleteWeekHasNothingToBeMeasuredAgainst() {
        val points = weeklyEnergyPoints(
            range = rangeOfWeeks(3),
            nutritionTotals = (0..2).flatMap { kcalWeek(it, 2200.0) },
            weights = (0..2).flatMap { weightWeek(it, 80.0) },
        )

        assertEquals(listOf(week(1), week(2)), points.map { it.weekStart })
    }

    @Test
    fun apartWeekAtTheEdgeIsLeftOutOnBothSides() {
        // The window starts on the Tuesday of week 0, so week 0 is only six days long.
        val points = weeklyEnergyPoints(
            range = EpochDayRange(startInclusive = week(0) + 1, endInclusive = week(3) - 1),
            nutritionTotals = (0..2).flatMap { kcalWeek(it, 2200.0) },
            weights = (0..2).flatMap { weightWeek(it, 80.0) },
        )

        // Week 1 is the first complete one and has no complete predecessor; only week 2 survives.
        // Comparing a full week against six days of another would report the missing day as weight.
        assertEquals(listOf(week(2)), points.map { it.weekStart })
    }

    @Test
    fun aWeekLoggedOnlyOnTheWeekendIsLeftOut() {
        val points = weeklyEnergyPoints(
            range = rangeOfWeeks(2),
            nutritionTotals = kcalWeek(0, 2000.0) + kcalWeek(1, 3000.0, days = 3),
            weights = weightWeek(0, 80.0) + weightWeek(1, 80.5),
        )

        // Three days of eating averaged into a week's dot would put a long weekend on the chart as
        // though it were the whole week.
        assertTrue(points.isEmpty())
    }

    @Test
    fun aWeekWithoutAWeighInIsLeftOut() {
        val points = weeklyEnergyPoints(
            range = rangeOfWeeks(2),
            nutritionTotals = kcalWeek(0, 2000.0) + kcalWeek(1, 2400.0),
            weights = weightWeek(0, 80.0),
        )

        assertTrue(points.isEmpty())
    }

    /**
     * Weeks along a straight line: 0,45 kg per week for every 500 kcal a day, weight held at 2200.
     * [count] weeks, plus the baseline week they are measured from.
     */
    private fun straightLineWeeks(count: Int, maintenance: Double = 2200.0): List<WeeklyEnergyPoint> {
        val kcal = List(count) { 1800.0 + it * 200.0 }
        val totals = kcalWeek(0, maintenance) + kcal.flatMapIndexed { index, value ->
            kcalWeek(index + 1, value)
        }
        var mean = 80.0
        val weights = weightWeek(0, mean) + kcal.flatMapIndexed { index, value ->
            mean += (value - maintenance) * 0.0009
            weightWeek(index + 1, mean)
        }
        return weeklyEnergyPoints(rangeOfWeeks(count + 1), totals, weights)
    }

    @Test
    fun theSummaryReadsTheSlopeAndWhereTheWeightHolds() {
        val summary = weeklyEnergySummary(
            points = straightLineWeeks(count = 8),
            goalTimeline = listOf(MetricPoint(week(1), 2000.0), MetricPoint(week(2), 2000.0)),
        )

        assertEquals(8, summary.points.size)
        assertTrue(summary.hasEnoughWeeks)
        assertEquals(2200.0, summary.maintenanceKcal!!, 1.0)
        assertEquals(2000.0, summary.goalKcalPerDay!!, 1e-9)
        assertEquals("8 Wochen · Zusammenhang stark (r² = 1)", summary.relationshipText())
        assertEquals("Je 500 kcal/Tag mehr: +0,5 kg pro Woche", summary.slopeText())
        assertEquals(
            "Gewicht gehalten bei ~2200 kcal/Tag · dein Ziel: 2000 kcal/Tag",
            summary.maintenanceText(),
        )
    }

    @Test
    fun aHandfulOfWeeksNamesNoMaintenance() {
        val summary = weeklyEnergySummary(points = straightLineWeeks(count = 4), goalTimeline = emptyList())

        // Four weeks are enough to draw a line through, not enough to hang a calorie figure on.
        assertTrue(summary.hasEnoughWeeks)
        assertNull(summary.maintenanceKcal)
        assertNull(summary.maintenanceText())
    }

    @Test
    fun tooFewWeeksClaimNothingAtAll() {
        val summary = weeklyEnergySummary(points = straightLineWeeks(count = 2), goalTimeline = emptyList())

        assertEquals(2, summary.points.size)
        assertTrue(!summary.hasEnoughWeeks)
        // Two points make a perfect line and no trend at all.
        assertNull(summary.fit)
        assertEquals("2 Wochen", summary.relationshipText())
        assertNull(summary.slopeText())
    }

    @Test
    fun aCrossingOutsideWhatWasEatenIsNotAMaintenanceFigure() {
        // Every week well above maintenance: where the line would cross zero is a guess about
        // intakes this diary has never seen.
        val summary = weeklyEnergySummary(
            points = straightLineWeeks(count = 8, maintenance = 1000.0),
            goalTimeline = emptyList(),
        )

        assertTrue(summary.fit!!.slope > 0.0)
        assertNull(summary.maintenanceKcal)
    }
}
