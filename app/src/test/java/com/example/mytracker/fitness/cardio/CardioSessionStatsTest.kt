package com.example.prokject2_tracker.fitness.cardio

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioSessionStatsTest {
    private fun day(date: String): Long = LocalDate.parse(date).toEpochDay()

    private var nextId = 0
    private fun session(day: Long, minutes: Double, km: Double?) = CardioSession(
        id = "session-${nextId++}",
        epochDay = day,
        createdAt = Instant.EPOCH,
        activityTypeId = "cardiotype-laufen",
        activityTypeName = "Laufen",
        durationMinutes = minutes,
        distanceKm = km,
    )

    @Test
    fun pace_isTotalMinutesOverTotalDistance_notTheMeanOfPaces() {
        // 1 km at 7:00 and 20 km at 5:00. The unweighted mean of the two paces is 6:00, but the
        // ground actually covered was run at (7 + 100) / 21 ≈ 5,095 min/km.
        val sessions = listOf(
            session(day("2026-07-20"), minutes = 7.0, km = 1.0),
            session(day("2026-07-20"), minutes = 100.0, km = 20.0),
        )
        assertEquals(107.0 / 21.0, paceMinPerKm(sessions)!!, 0.0001)
    }

    @Test
    fun pace_ignoresSessionsWithoutDistance() {
        val sessions = listOf(
            session(day("2026-07-20"), minutes = 30.0, km = 6.0),
            session(day("2026-07-20"), minutes = 45.0, km = null), // treadmill, no distance
        )
        assertEquals(5.0, paceMinPerKm(sessions)!!, 0.0001)
    }

    @Test
    fun pace_isNullWhenNothingHasADistance() {
        assertNull(paceMinPerKm(listOf(session(day("2026-07-20"), minutes = 45.0, km = null))))
    }

    @Test
    fun dayStats_totalsMinutesIncludingDistancelessSessions() {
        val sessions = listOf(
            session(day("2026-07-20"), minutes = 30.0, km = 6.0),
            session(day("2026-07-20"), minutes = 45.0, km = null),
        )
        val stats = sessions.dayStatsOn(day("2026-07-20"))!!
        assertEquals(75.0, stats.totalMinutes, 0.0001)
        assertEquals(6.0, stats.totalDistanceKm!!, 0.0001)
        assertEquals(2, stats.sessions.size)
    }

    @Test
    fun dayStats_isNullForAnUntrainedDay() {
        val sessions = listOf(session(day("2026-07-20"), minutes = 30.0, km = 6.0))
        assertNull(sessions.dayStatsOn(day("2026-07-21")))
    }

    @Test
    fun dayStats_distanceIsNullWhenNoSessionHasOne() {
        val sessions = listOf(session(day("2026-07-20"), minutes = 45.0, km = null))
        assertNull(sessions.dayStatsOn(day("2026-07-20"))!!.totalDistanceKm)
    }

    @Test
    fun previousSessionDay_findsTheDayBefore() {
        val sessions = listOf(
            session(day("2026-07-24"), 30.0, 6.0),
            session(day("2026-07-20"), 40.0, 8.0),
        )
        assertEquals(day("2026-07-20"), sessions.previousSessionDay(before = day("2026-07-24")))
        assertNull(sessions.previousSessionDay(before = day("2026-07-20")))
    }

    @Test
    fun weeklyPace_weightsAcrossTheWholeWeek() {
        // Same trap as the day-level case, spread over two days of one week.
        val sessions = listOf(
            session(day("2026-07-20"), minutes = 7.0, km = 1.0),
            session(day("2026-07-22"), minutes = 100.0, km = 20.0),
        )
        val weekly = sessions.weeklyPacePoints()
        assertEquals(1, weekly.size)
        assertEquals(day("2026-07-20"), weekly[0].epochDay) // bucketed on the Monday
        assertEquals(107.0 / 21.0, weekly[0].value, 0.0001)
    }

    @Test
    fun weeklyPace_skipsWeeksWithoutAnyDistance() {
        val sessions = listOf(session(day("2026-07-20"), minutes = 45.0, km = null))
        assertEquals(emptyList<Long>(), sessions.weeklyPacePoints().map { it.epochDay })
    }

    @Test
    fun dailyDistance_skipsDaysWithoutDistanceButMinutesDoNot() {
        val sessions = listOf(
            session(day("2026-07-20"), minutes = 45.0, km = null),
            session(day("2026-07-22"), minutes = 30.0, km = 6.0),
        )
        assertEquals(listOf(day("2026-07-22")), sessions.dailyDistancePoints().map { it.epochDay })
        assertEquals(2, sessions.dailyMinutePoints().size)
    }
}
