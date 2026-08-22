package com.example.prokject2_tracker.task

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val AnInstant: Instant = Instant.ofEpochMilli(1_700_000_000_000)

private fun day(iso: String): Long = LocalDate.parse(iso).toEpochDay()

private fun task(
    recurrence: TaskRecurrence,
    start: String,
    intervalCount: Int = 1,
    weekdayMask: Int = 0,
    dayOfMonth: Int = 1,
) = Task(
    id = "task-1",
    name = "Aufgabe",
    recurrence = recurrence,
    intervalCount = intervalCount,
    weekdayMask = weekdayMask,
    dayOfMonth = dayOfMonth,
    startEpochDay = day(start),
    createdAt = AnInstant,
    updatedAt = AnInstant,
)

/** The rhythms the task screen offers, checked against the calendar rather than against arithmetic. */
class TaskScheduleTest {
    @Test
    fun aOneOffIsDueOnItsDayAndNeverAgain() {
        val once = task(TaskRecurrence.ONCE, start = "2026-08-03")

        assertEquals(day("2026-08-03"), once.nextDueOnOrAfter(day("2026-07-01")))
        assertEquals(day("2026-08-03"), once.nextDueOnOrAfter(day("2026-08-03")))
        assertNull(once.nextDueAfter(day("2026-08-03")))
    }

    @Test
    fun everyThreeDaysCountsFromTheStartDay() {
        val every3 = task(TaskRecurrence.EVERY_N_DAYS, start = "2026-08-03", intervalCount = 3)

        assertEquals(day("2026-08-03"), every3.nextDueOnOrAfter(day("2026-08-03")))
        // A day between two grid points rounds forward, it does not fall on the day asked about.
        assertEquals(day("2026-08-06"), every3.nextDueOnOrAfter(day("2026-08-04")))
        assertEquals(day("2026-08-06"), every3.nextDueOnOrAfter(day("2026-08-06")))
        assertEquals(day("2026-08-09"), every3.nextDueAfter(day("2026-08-06")))
        // Nothing before the start, however far back the question reaches.
        assertEquals(day("2026-08-03"), every3.nextDueOnOrAfter(day("2020-01-01")))
    }

    @Test
    fun everyThreeWeeksKeepsTheStartWeekday() {
        // 2026-08-03 is a Monday.
        val every3Weeks = task(TaskRecurrence.EVERY_N_WEEKS, start = "2026-08-03", intervalCount = 3)

        assertEquals(day("2026-08-24"), every3Weeks.nextDueAfter(day("2026-08-03")))
        assertEquals(day("2026-09-14"), every3Weeks.nextDueAfter(day("2026-08-24")))
        assertEquals(
            DayOfWeek.MONDAY,
            LocalDate.ofEpochDay(every3Weeks.nextDueAfter(day("2026-08-24"))!!).dayOfWeek,
        )
    }

    @Test
    fun weekdaysFireEveryWeekOnTheTickedDays() {
        val monWedFri = task(
            TaskRecurrence.WEEKDAYS,
            start = "2026-08-03",
            weekdayMask = DayOfWeek.MONDAY.bit() or DayOfWeek.WEDNESDAY.bit() or DayOfWeek.FRIDAY.bit(),
        )

        assertEquals(day("2026-08-03"), monWedFri.nextDueOnOrAfter(day("2026-08-03")))
        assertEquals(day("2026-08-05"), monWedFri.nextDueAfter(day("2026-08-03")))
        assertEquals(day("2026-08-07"), monWedFri.nextDueAfter(day("2026-08-05")))
        // Over the weekend and into the next week.
        assertEquals(day("2026-08-10"), monWedFri.nextDueAfter(day("2026-08-07")))
    }

    @Test
    fun aWeekdayTaskWithNothingTickedIsNeverDue() {
        val nothing = task(TaskRecurrence.WEEKDAYS, start = "2026-08-03", weekdayMask = 0)

        assertNull(nothing.nextDueOnOrAfter(day("2026-08-03")))
        assertNull(nothing.oldestOpenOccurrence(day("2027-01-01")) { false })
    }

    @Test
    fun theFirstOfTheMonthComesRoundEveryMonth() {
        val monthly = task(TaskRecurrence.DAY_OF_MONTH, start = "2026-08-03", dayOfMonth = 1)

        // The 1st of the start month is already behind the start day, so the series opens in September.
        assertEquals(day("2026-09-01"), monthly.nextDueOnOrAfter(day("2026-08-03")))
        assertEquals(day("2026-10-01"), monthly.nextDueAfter(day("2026-09-01")))
        assertEquals(day("2027-01-01"), monthly.nextDueAfter(day("2026-12-01")))
    }

    @Test
    fun aDayPastTheEndOfTheMonthLandsOnItsLastDay() {
        val thirtyFirst = task(TaskRecurrence.DAY_OF_MONTH, start = "2027-01-01", dayOfMonth = 31)

        assertEquals(day("2027-01-31"), thirtyFirst.nextDueOnOrAfter(day("2027-01-01")))
        // February 2027 has 28 days — the occurrence is kept and moved, not skipped.
        assertEquals(day("2027-02-28"), thirtyFirst.nextDueAfter(day("2027-01-31")))
        assertEquals(day("2027-03-31"), thirtyFirst.nextDueAfter(day("2027-02-28")))
    }

    @Test
    fun everyThreeMonthsStaysInPhaseWithTheStartMonth() {
        val quarterly = task(TaskRecurrence.DAY_OF_MONTH, start = "2026-08-01", intervalCount = 3, dayOfMonth = 1)

        assertEquals(day("2026-08-01"), quarterly.nextDueOnOrAfter(day("2026-08-01")))
        assertEquals(day("2026-11-01"), quarterly.nextDueAfter(day("2026-08-01")))
        // Asking from a month that is out of phase jumps to the next one that is in phase.
        assertEquals(day("2026-11-01"), quarterly.nextDueOnOrAfter(day("2026-09-15")))
    }

    @Test
    fun aMissedDayStaysOpenInsteadOfBeingSkipped() {
        val daily = task(TaskRecurrence.EVERY_N_DAYS, start = "2026-08-01", intervalCount = 1)
        val today = day("2026-08-05")

        // Nothing done at all: the oldest open occurrence is the very first one.
        assertEquals(day("2026-08-01"), daily.oldestOpenOccurrence(today) { false })

        // The first three ticked off; the 4th is what is still owed, not today's.
        val done = setOf(day("2026-08-01"), day("2026-08-02"), day("2026-08-03"))
        assertEquals(day("2026-08-04"), daily.oldestOpenOccurrence(today) { it in done })

        // All caught up including today: nothing open.
        assertNull(daily.oldestOpenOccurrence(today) { it <= today })
    }

    @Test
    fun tickingTodayWhileYesterdayIsOpenStillReportsYesterday() {
        val daily = task(TaskRecurrence.EVERY_N_DAYS, start = "2026-08-01", intervalCount = 1)
        val today = day("2026-08-05")
        val done = setOf(day("2026-08-01"), day("2026-08-02"), day("2026-08-03"), today)

        // Working out of order does not bury the gap: the 4th is still the oldest thing owed.
        assertEquals(day("2026-08-04"), daily.oldestOpenOccurrence(today) { it in done })
    }

    @Test
    fun aFutureTaskHasNothingOpenYet() {
        val later = task(TaskRecurrence.ONCE, start = "2026-09-01")

        assertNull(later.oldestOpenOccurrence(day("2026-08-03")) { false })
        assertEquals(day("2026-09-01"), later.nextDueOnOrAfter(day("2026-08-03")))
    }

    @Test
    fun aFinishedOneOffFallsOutOfTheOpenList() {
        val once = task(TaskRecurrence.ONCE, start = "2026-08-03")
        val today = day("2026-08-10")

        assertEquals(day("2026-08-03"), once.oldestOpenOccurrence(today) { false })
        assertNull(once.oldestOpenOccurrence(today) { it == day("2026-08-03") })
    }
}
