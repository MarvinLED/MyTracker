package com.example.mytracker.achievements

import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.fitness.strength.StrengthSet
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the wall is built out of. Every mark here is derived rather than stored, so these are the
 * tests that decide whether a record means what it says.
 */
class AchievementDerivationsTest {
    private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

    private fun set(
        iso: String,
        weightKg: Double?,
        reps: Int = 5,
        exerciseId: String = "bench",
        isBodyweight: Boolean = false,
    ) = StrengthSet(
        id = "$exerciseId-$iso-$weightKg-$reps",
        logEntryId = "entry-$iso",
        epochDay = day(iso),
        exerciseId = exerciseId,
        setIndex = 0,
        reps = reps,
        weightKg = weightKg,
        isBodyweight = isBodyweight,
    )

    @Test
    fun aRecordCarriesTheMarkItBeat() {
        val mark = recordMark(
            listOf(
                MetricPoint(day("2026-01-05"), 100.0),
                MetricPoint(day("2026-02-05"), 105.0),
                MetricPoint(day("2026-03-05"), 102.0),
            ),
        )!!

        assertEquals(105.0, mark.value, 0.0001)
        assertEquals(day("2026-02-05"), mark.epochDay)
        // "105 kg, davor waren es 100" is the sentence worth reading; the bare number is not.
        assertEquals(100.0, mark.previous!!, 0.0001)
    }

    @Test
    fun theFirstMarkHasNothingBehindIt() {
        val mark = recordMark(listOf(MetricPoint(day("2026-01-05"), 100.0)))!!

        assertNull(mark.previous)
    }

    @Test
    fun repeatingTheBestDoesNotMoveItsDate() {
        val mark = recordMark(
            listOf(
                MetricPoint(day("2026-01-05"), 100.0),
                MetricPoint(day("2026-06-05"), 100.0),
            ),
        )!!

        // The day the mark was set is the first day it was reached. Matching it later is the same
        // lift, not a new record, and dating it to the repeat would quietly rewrite history.
        assertEquals(day("2026-01-05"), mark.epochDay)
        assertNull(mark.previous)
    }

    @Test
    fun outOfOrderPointsStillYieldTheChronologicalStory() {
        val mark = recordMark(
            listOf(
                MetricPoint(day("2026-03-05"), 110.0),
                MetricPoint(day("2026-01-05"), 100.0),
            ),
        )!!

        assertEquals(110.0, mark.value, 0.0001)
        assertEquals(100.0, mark.previous!!, 0.0001)
    }

    @Test
    fun lessCanBeTheAchievement() {
        val mark = recordMark(
            listOf(
                MetricPoint(day("2026-01-05"), 84.0),
                MetricPoint(day("2026-02-05"), 81.5),
            ),
            higherIsBetter = false,
        )!!

        assertEquals(81.5, mark.value, 0.0001)
        assertEquals(84.0, mark.previous!!, 0.0001)
    }

    @Test
    fun recordsAreKeptPerExercise() {
        val records = topSetRecords(
            listOf(
                set("2026-01-05", 100.0),
                set("2026-02-05", 105.0),
                set("2026-02-05", 60.0, exerciseId = "row"),
            ),
        )

        assertEquals(105.0, records.getValue("bench").value, 0.0001)
        assertEquals(60.0, records.getValue("row").value, 0.0001)
    }

    @Test
    fun anExerciseOnlyEverDoneAtBodyweightHasNoTopSet() {
        val records = topSetRecords(
            listOf(set("2026-01-05", null, exerciseId = "pullup", isBodyweight = true)),
        )

        // Reading a bodyweight set as 0 kg would put a "Rekord: 0 kg" on the wall for Klimmzüge.
        assertTrue(records.isEmpty())
    }

    @Test
    fun volumeIsSummedPerCalendarWeek() {
        val points = weeklyVolumePoints(
            listOf(
                // 2026-08-19 is a Wednesday, 2026-08-21 the Friday of the same week.
                set("2026-08-19", 100.0, reps = 5),
                set("2026-08-21", 100.0, reps = 5),
                set("2026-08-25", 50.0, reps = 10),
            ),
        )

        assertEquals(2, points.size)
        // Keyed on the Monday, so the weeks line up with everything else the app buckets weekly.
        assertEquals(day("2026-08-17"), points[0].epochDay)
        assertEquals(1000.0, points[0].value, 0.0001)
        assertEquals(day("2026-08-24"), points[1].epochDay)
        assertEquals(500.0, points[1].value, 0.0001)
    }

    @Test
    fun aSessionIsAnExerciseOnADayNotALogEntry() {
        val count = strengthSessionCount(
            listOf(
                set("2026-08-19", 100.0),
                set("2026-08-19", 105.0),
                set("2026-08-19", 60.0, exerciseId = "row"),
                set("2026-08-21", 100.0),
            ),
        )

        // Three: Bankdrücken twice on two days, Rudern once — not four sets and not two log entries.
        assertEquals(3, count)
    }

    @Test
    fun bodyweightSetsCarryNoVolume() {
        val total = totalVolume(
            listOf(
                set("2026-08-19", 100.0, reps = 5),
                set("2026-08-19", null, reps = 12, exerciseId = "pullup", isBodyweight = true),
            ),
        )

        // The same rule the Fitness-Ziele already count by, so the milestone and the goal never
        // disagree about how much was moved.
        assertEquals(500.0, total, 0.0001)
    }
}
