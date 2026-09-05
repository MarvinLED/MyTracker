package com.example.mytracker.core.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The run fold behind both the Habit streak and the Erfassungsserie. Pure over a set of days, so
 * every case that used to need a device and a wait can be written down instead.
 */
class DayStreakTest {
    private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

    private fun days(vararg iso: String): Set<Long> = iso.map { day(it) }.toSet()

    @Test
    fun aRunEndingTodayIsTheCurrentOne() {
        val streak = dayStreak(days("2026-09-02", "2026-09-03", "2026-09-04"), today = day("2026-09-04"))

        assertEquals(3, streak.current)
        assertEquals(3, streak.best)
    }

    @Test
    fun todayNotDoneYetDoesNotBreakTheRun() {
        val streak = dayStreak(days("2026-09-01", "2026-09-02", "2026-09-03"), today = day("2026-09-04"))

        // The day is still running, so it is not a day the habit was missed in. Counting it as a
        // break would put the fire out every morning and light it again each evening.
        assertEquals(3, streak.current)
    }

    @Test
    fun aRunThatEndedBeforeYesterdayIsOver() {
        val streak = dayStreak(days("2026-08-20", "2026-08-21", "2026-08-22"), today = day("2026-09-04"))

        assertEquals(0, streak.current)
        // Gone, but not forgotten: that is the whole reason the best is kept.
        assertEquals(3, streak.best)
    }

    @Test
    fun theBestRunSurvivesTheOneThatBrokeIt() {
        val streak = dayStreak(
            days(
                "2026-08-01", "2026-08-02", "2026-08-03", "2026-08-04", "2026-08-05",
                // A gap on the 6th and 7th.
                "2026-08-08", "2026-08-09",
            ),
            today = day("2026-09-04"),
        )

        assertEquals(0, streak.current)
        assertEquals(5, streak.best)
    }

    @Test
    fun theRunningStreakCanBeTheBestOne() {
        val streak = dayStreak(
            days("2026-08-01", "2026-08-02", "2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04"),
            today = day("2026-09-04"),
        )

        assertEquals(4, streak.current)
        assertEquals(4, streak.best)
    }

    @Test
    fun nothingLoggedIsNoRunAtAll() {
        val streak = dayStreak(emptySet(), today = day("2026-09-04"))

        assertEquals(0, streak.current)
        assertEquals(0, streak.best)
    }

    @Test
    fun aSingleDayCountsAsARunOfOne() {
        val streak = dayStreak(days("2026-09-04"), today = day("2026-09-04"))

        assertEquals(1, streak.current)
        assertEquals(1, streak.best)
    }

    @Test
    fun daysAfterTodayAreNotCountedIntoTheCurrentRun() {
        // A body weight can be logged for tomorrow; the run is still the one leading up to today.
        val streak = dayStreak(days("2026-09-03", "2026-09-04", "2026-09-05"), today = day("2026-09-04"))

        assertEquals(2, streak.current)
        // The best does look at the whole set, future days included — it is a record over what was
        // logged, not a statement about today.
        assertEquals(3, streak.best)
    }
}
