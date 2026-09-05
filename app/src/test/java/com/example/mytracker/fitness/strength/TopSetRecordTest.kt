package com.example.mytracker.fitness.strength

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** When the exercise screen is allowed to claim a new all-time best. */
class TopSetRecordTest {
    private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

    private fun set(iso: String, weightKg: Double?, isBodyweight: Boolean = false) = StrengthSet(
        id = "$iso-$weightKg",
        logEntryId = "entry-$iso",
        epochDay = day(iso),
        exerciseId = "bench",
        setIndex = 0,
        reps = 5,
        weightKg = weightKg,
        isBodyweight = isBodyweight,
    )

    private fun session(iso: String, maxWeightKg: Double?) = SessionStats(
        epochDay = day(iso),
        sets = emptyList(),
        maxWeightKg = maxWeightKg,
        volumeKg = 0.0,
    )

    @Test
    fun beatingEveryEarlierDayIsARecord() {
        val record = topSetRecord(
            current = session("2026-09-04", 105.0),
            allSets = listOf(set("2026-08-01", 100.0), set("2026-08-20", 102.5), set("2026-09-04", 105.0)),
            day = day("2026-09-04"),
        )!!

        assertEquals(105.0, record.weightKg, 0.0001)
        assertEquals(102.5, record.previousKg!!, 0.0001)
    }

    @Test
    fun matchingTheOldBestIsNotARecord() {
        val record = topSetRecord(
            current = session("2026-09-04", 102.5),
            allSets = listOf(set("2026-08-20", 102.5), set("2026-09-04", 102.5)),
            day = day("2026-09-04"),
        )

        // Strictly greater, like the volume target: a banner that fired on every repeat of an old
        // best would stop meaning anything inside a week.
        assertNull(record)
    }

    @Test
    fun theFirstWeightedSessionIsAFirstMarkNotABrokenOne() {
        val record = topSetRecord(
            current = session("2026-09-04", 60.0),
            allSets = listOf(set("2026-09-04", 60.0)),
            day = day("2026-09-04"),
        )!!

        // Null rather than 0 kg: "davor 0 kg" would invent a session that never happened.
        assertNull(record.previousKg)
    }

    @Test
    fun aBodyweightOnlyDayClaimsNothing() {
        val record = topSetRecord(
            current = session("2026-09-04", null),
            allSets = listOf(set("2026-09-04", null, isBodyweight = true)),
            day = day("2026-09-04"),
        )

        assertNull(record)
    }

    @Test
    fun aWeekOffDoesNotLowerTheBar() {
        val record = topSetRecord(
            current = session("2026-09-04", 95.0),
            // The best is months old; the session right before was a light one.
            allSets = listOf(set("2026-03-01", 110.0), set("2026-08-28", 90.0), set("2026-09-04", 95.0)),
            day = day("2026-09-04"),
        )

        // Beating the last session is the volume target's business. A record is measured against
        // everything, or it is not a record.
        assertNull(record)
    }

    @Test
    fun anEarlierDayIsJudgedAgainstWhatCameBeforeIt() {
        val allSets = listOf(set("2026-08-01", 100.0), set("2026-08-20", 102.5), set("2026-09-04", 105.0))

        val record = topSetRecord(current = session("2026-08-20", 102.5), allSets = allSets, day = day("2026-08-20"))!!

        // Scrolling back to an old session must show what it was at the time, not what it looks
        // like now that it has since been beaten.
        assertEquals(102.5, record.weightKg, 0.0001)
        assertEquals(100.0, record.previousKg!!, 0.0001)
    }
}
