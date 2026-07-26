package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.metrics.Granularity
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.bucketBy
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StrengthSessionStatsTest {
    private fun day(date: String): Long = LocalDate.parse(date).toEpochDay()

    private var nextId = 0
    private fun row(day: Long, reps: Int, weightKg: Double?, setIndex: Int = 0) = StrengthSet(
        id = "set-${nextId++}",
        logEntryId = "entry-$day",
        epochDay = day,
        exerciseId = "bench",
        setIndex = setIndex,
        reps = reps,
        weightKg = weightKg,
    )

    @Test
    fun volume_countsBodyweightSetsAsZero() {
        val sets = listOf(SetDraft(8, 60.0), SetDraft(12, null))
        assertEquals(480.0, volumeOf(sets), 0.0001)
    }

    @Test
    fun maxWeight_ignoresBodyweightSets() {
        assertEquals(60.0, maxWeightOf(listOf(SetDraft(8, 60.0), SetDraft(12, null)))!!, 0.0001)
    }

    @Test
    fun maxWeight_isNullForABodyweightOnlyDay() {
        // Not 0.0 — a pull-up day has no "heaviest weight", and reporting 0 would flatten the chart.
        assertNull(maxWeightOf(listOf(SetDraft(12, null), SetDraft(10, null))))
    }

    @Test
    fun sessionOn_collectsThatDayInLoggingOrder() {
        val sets = listOf(
            row(day("2026-07-22"), 5, 50.0, setIndex = 0),
            row(day("2026-07-22"), 8, 60.0, setIndex = 1),
            row(day("2026-07-20"), 8, 55.0),
        )
        val session = sets.sessionOn(day("2026-07-22"))!!
        assertEquals(2, session.setCount)
        assertEquals(60.0, session.maxWeightKg!!, 0.0001)
        assertEquals(730.0, session.volumeKg, 0.0001)
        assertEquals("50 kg × 5 · 60 kg × 8", formatSetSummary(session.sets))
    }

    @Test
    fun sessionOn_isNullForAnUntrainedDay() {
        val sets = listOf(row(day("2026-07-22"), 8, 60.0))
        assertNull(sets.sessionOn(day("2026-07-21")))
    }

    @Test
    fun previousSessionDay_skipsDaysWithoutSets() {
        val sets = listOf(
            row(day("2026-07-24"), 8, 60.0),
            row(day("2026-07-20"), 8, 55.0),
        )
        assertEquals(day("2026-07-20"), sets.previousSessionDay(before = day("2026-07-24")))
    }

    @Test
    fun previousSessionDay_isNullForTheFirstEverSession() {
        val sets = listOf(row(day("2026-07-20"), 8, 55.0))
        assertNull(sets.previousSessionDay(before = day("2026-07-20")))
    }

    @Test
    fun recentSessions_areNewestFirstAndCapped() {
        val sets = listOf(
            row(day("2026-07-24"), 8, 60.0),
            row(day("2026-07-22"), 8, 58.0),
            row(day("2026-07-20"), 8, 55.0),
        )
        val recent = sets.recentSessions(limit = 2)
        assertEquals(listOf(day("2026-07-24"), day("2026-07-22")), recent.map { it.epochDay })
    }

    @Test
    fun dailyMaxWeight_emitsNoPointForABodyweightOnlyDay() {
        val sets = listOf(
            row(day("2026-07-20"), 12, null),
            row(day("2026-07-22"), 8, 60.0),
        )
        assertEquals(listOf(day("2026-07-22")), sets.dailyMaxWeightPoints().map { it.epochDay })
        // Volume and set count still cover that day.
        assertEquals(2, sets.dailyVolumePoints().size)
        assertEquals(2, sets.dailySetCountPoints().size)
    }

    @Test
    fun weeklyMax_equalsTheHeaviestSetOfTheWeek() {
        val sets = listOf(
            row(day("2026-07-20"), 8, 60.0),
            row(day("2026-07-22"), 3, 82.5),
            row(day("2026-07-24"), 8, 65.0),
        )
        val weekly = sets.dailyMaxWeightPoints().bucketBy(Granularity.WEEKLY, MetricAggregation.MAX)
        assertEquals(1, weekly.size)
        assertEquals(day("2026-07-20"), weekly[0].epochDay)
        assertEquals(82.5, weekly[0].value, 0.0001)
    }

    @Test
    fun weeklyVolume_sumsTheWholeWeek() {
        val sets = listOf(
            row(day("2026-07-20"), 10, 50.0), // 500
            row(day("2026-07-24"), 8, 60.0), // 480
        )
        val weekly = sets.dailyVolumePoints().bucketBy(Granularity.WEEKLY, MetricAggregation.SUM)
        assertEquals(980.0, weekly[0].value, 0.0001)
    }
}
