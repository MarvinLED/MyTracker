package com.example.mytracker.fitness.strength

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session list of the comparison card. Its whole job is saying how each training compares to the
 * one before it, so most of what is tested here is which session counts as "the one before".
 */
class SessionRowsTest {
    private fun day(date: String): Long = LocalDate.parse(date).toEpochDay()

    private fun session(date: String, maxKg: Double?, reps: Int = 8): SessionStats {
        val sets = listOf(SetDraft(reps, maxKg, isBodyweight = maxKg == null))
        return SessionStats(day(date), sets, maxWeightOf(sets), volumeOf(sets))
    }

    @Test
    fun theFirstRowIsTheSelectedDayAndTheRestAreCountedBackFromIt() {
        val rows = sessionRows(
            selectedEpochDay = day("2026-07-24"),
            current = session("2026-07-24", 60.0),
            older = listOf(session("2026-07-21", 57.5), session("2026-07-17", 55.0)),
        )

        assertEquals(listOf("Dieses", "vor 3 Tagen", "vor 7 Tagen"), rows.map { it.label })
        assertEquals(listOf("60 kg", "57,5 kg", "55 kg"), rows.map { it.maxWeightText })
    }

    @Test
    fun theDistanceIsToTheSelectedDayRatherThanToToday() {
        // Standing on a historic day, "vor 2 Tagen" has to mean two days before *that* day.
        val rows = sessionRows(
            selectedEpochDay = day("2020-01-10"),
            current = session("2020-01-10", 60.0),
            older = listOf(session("2020-01-08", 55.0)),
        )

        assertEquals("vor 2 Tagen", rows[1].label)
        assertTrue(rows[1].dateText.contains("8. Januar 2020"))
    }

    @Test
    fun aHeavierTopSetThanLastTimeIsAnImprovementAndALighterOneIsNot() {
        val rows = sessionRows(
            selectedEpochDay = day("2026-07-24"),
            current = session("2026-07-24", 62.5),
            older = listOf(session("2026-07-21", 60.0), session("2026-07-17", 65.0)),
        )

        assertEquals(MaxWeightTrend.IMPROVED, rows[0].trend)
        // 60 after 65: below the time before, which is the amber case.
        assertEquals(MaxWeightTrend.DECLINED, rows[1].trend)
    }

    @Test
    fun repeatingTheSameTopSetIsNeitherImprovedNorDeclined() {
        val rows = sessionRows(
            selectedEpochDay = day("2026-07-24"),
            current = session("2026-07-24", 60.0),
            older = listOf(session("2026-07-21", 60.0)),
        )

        // Holding a weight is not a step back; colouring it amber would call a plateau a failure.
        assertEquals(MaxWeightTrend.MATCHED, rows[0].trend)
    }

    @Test
    fun aBodyweightOnlyDayHasNothingToCompareAtEitherEnd() {
        val rows = sessionRows(
            selectedEpochDay = day("2026-07-24"),
            current = session("2026-07-24", null, reps = 12),
            older = listOf(session("2026-07-21", 60.0), session("2026-07-17", null, reps = 10)),
        )

        assertEquals(MaxWeightTrend.UNKNOWN, rows[0].trend)
        assertNull(rows[0].maxWeightText)
        // 60 kg against a bodyweight day: no earlier weight to have beaten.
        assertEquals(MaxWeightTrend.UNKNOWN, rows[1].trend)
    }

    @Test
    fun anEmptySelectedDayStillGetsItsOwnRow() {
        val rows = sessionRows(
            selectedEpochDay = day("2026-07-24"),
            current = null,
            older = listOf(session("2026-07-21", 60.0)),
        )

        assertEquals("Dieses", rows.first().label)
        assertNull(rows.first().setSummary)
        assertEquals(MaxWeightTrend.UNKNOWN, rows.first().trend)
        // And it does not swallow the row for the session that does exist.
        assertEquals(2, rows.size)
    }

    @Test
    fun theLastListedRowIsJudgedAgainstASessionBeyondTheLimit() {
        val older = (1..4).map { session("2026-07-${20 - it}", 60.0 - it) }
        val rows = sessionRows(
            selectedEpochDay = day("2026-07-24"),
            current = session("2026-07-24", 62.0),
            older = older,
            limit = 3,
        )

        // Three older rows shown, and the third is still IMPROVED because the fourth was handed in
        // as its reference — cutting the list off must not turn a real gain into "kein Vergleich".
        assertEquals(4, rows.size)
        assertEquals(MaxWeightTrend.IMPROVED, rows.last().trend)
    }
}
