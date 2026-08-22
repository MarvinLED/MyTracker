package com.example.prokject2_tracker.sleep

import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.util.formatDuration
import com.example.prokject2_tracker.core.util.formatMinuteOfDay
import com.example.prokject2_tracker.core.util.minutesBetweenTimesOfDay
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute

private fun night(
    start: Int = at(23),
    end: Int = at(7),
    lastMeal: Int? = null,
    fitness: Int? = null,
) = SleepEntry(
    id = "sleep-20000",
    epochDay = 20_000L,
    startMinuteOfDay = start,
    endMinuteOfDay = end,
    morningFitness = fitness,
    lastMealMinuteOfDay = lastMeal,
    createdAt = Instant.EPOCH,
)

/**
 * A night crosses midnight, so every duration and every comparison here counts forwards rather than
 * subtracting clock numbers. These pin that: the length of a night, the gap to the last meal, and
 * both goals — including the one case a naive comparison always gets wrong, going to bed *after*
 * midnight against a 23:00 target.
 */
class SleepGoalsTest {
    @Test
    fun aNightIsCountedForwardsAcrossMidnight() {
        assertEquals(7 * 60 + 35, minutesBetweenTimesOfDay(at(23, 10), at(6, 45)))
        assertEquals(465, night(start = at(22, 30), end = at(6, 15)).durationMinutes)
        // An afternoon nap needs no midnight at all.
        assertEquals(90, night(start = at(14), end = at(15, 30)).durationMinutes)
        // Equal times are 0, not a full day.
        assertEquals(0, night(start = at(23), end = at(23)).durationMinutes)
    }

    @Test
    fun theGapToTheLastMealIsCountedTheSameWay() {
        assertEquals(2 * 60 + 40, night(start = at(23, 10), lastMeal = at(20, 30)).minutesBetweenLastMealAndSleep)
        // Eaten after midnight, asleep at 1:00.
        assertEquals(30, night(start = at(1), lastMeal = at(0, 30)).minutesBetweenLastMealAndSleep)
        assertEquals(null, night().minutesBetweenLastMealAndSleep)
    }

    @Test
    fun aBedtimeAfterMidnightIsLaterThanOneBefore() {
        val goal = at(23)
        assertTrue(isBedtimeMet(at(22, 40), goal))
        assertTrue(isBedtimeMet(at(23), goal))
        assertFalse(isBedtimeMet(at(23, 20), goal))
        // The case a plain minute comparison gets backwards: 00:30 is half an hour *past* the goal.
        assertFalse(isBedtimeMet(at(0, 30), goal))
        assertEquals(90, bedtimeDeviationMinutes(at(0, 30), goal))
    }

    @Test
    fun aBedtimeGoalAfterMidnightWorksToo() {
        val goal = at(0, 30)
        assertTrue(isBedtimeMet(at(23, 45), goal))
        assertTrue(isBedtimeMet(at(0, 30), goal))
        assertFalse(isBedtimeMet(at(1), goal))
    }

    @Test
    fun theDurationGoalUsesTheSameBoundsAsEveryOtherGoal() {
        val goal = NutrientGoal(min = 7.0 * 60, max = 9.0 * 60)

        val short = sleepGoalStatuses(night(start = at(0), end = at(6)), goal, null).single()
        assertFalse(short.isMet)
        assertEquals("6 h / 7 h – 9 h", short.valueText)

        val good = sleepGoalStatuses(night(start = at(23), end = at(7)), goal, null).single()
        assertTrue(good.isMet)

        // Oversleeping blows the upper bound, exactly as an upper nutrient bound would.
        val long = sleepGoalStatuses(night(start = at(21), end = at(9)), goal, null).single()
        assertFalse(long.isMet)
    }

    @Test
    fun anUnloggedNightCountsAsZeroRatherThanDisappearing() {
        val rows = sleepGoalStatuses(null, NutrientGoal(min = 7.0 * 60), at(23))

        assertEquals(listOf("Schlafdauer", "Schlafenszeit"), rows.map { it.label })
        assertTrue(rows.none { it.isMet })
        assertEquals("0 min / mind. 7 h", rows[0].valueText)
    }

    @Test
    fun withoutGoalsThereAreNoRowsAtAll() {
        assertTrue(sleepGoalStatuses(night(), durationGoalMinutes = null, bedtimeGoalMinuteOfDay = null).isEmpty())
        assertTrue(sleepGoalStatuses(night(), durationGoalMinutes = NutrientGoal(), bedtimeGoalMinuteOfDay = null).isEmpty())
    }

    @Test
    fun theBedtimeRowSaysHowFarOffItWas() {
        val row = sleepGoalStatuses(night(start = at(23, 20)), null, at(23)).single()

        assertEquals("Schlafenszeit", row.label)
        assertEquals("23:20 · 20 min zu spät", row.valueText)
        // No bar: being 20 minutes late is not a fraction of anything.
        assertEquals(null, row.fraction)
    }

    @Test
    fun timesAndDurationsAreFormattedTheWayTheScreensReadThem() {
        assertEquals("23:05", formatMinuteOfDay(at(23, 5)))
        assertEquals("07:00", formatMinuteOfDay(at(7)))
        assertEquals("7 h 35 min", formatDuration(455))
        assertEquals("8 h", formatDuration(480))
        assertEquals("45 min", formatDuration(45))
    }
}
