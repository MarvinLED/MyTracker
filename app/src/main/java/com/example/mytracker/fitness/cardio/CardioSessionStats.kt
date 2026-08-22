package com.example.mytracker.fitness.cardio

import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.util.DateUtils

/**
 * One activity type's work on one day. Unlike a strength session, the individual [CardioSession]s
 * are kept: two runs on one day each have their own duration, distance and heart rate, and merging
 * them would invent a single session that never happened. The totals are for the day's overview.
 */
data class CardioDayStats(
    val epochDay: Long,
    val sessions: List<CardioSession>,
    val totalMinutes: Double,
    val totalDistanceKm: Double?,
    /** Minutes per km over the whole day, or null when nothing with a distance was logged. */
    val paceMinPerKm: Double?,
)

/**
 * Pace over a set of sessions: total minutes divided by total distance, **not** the mean of the
 * individual paces. A 1 km jog at 7:00 and a 20 km run at 5:00 average to 6:00 min/km as an
 * unweighted mean, while the distance actually covered was run at ≈5:05.
 *
 * Only sessions with a positive distance count, matching `CardioDao.observeDailyAvgPace`'s filter —
 * otherwise a treadmill session logged without distance would inflate the numerator for free.
 */
fun paceMinPerKm(sessions: List<CardioSession>): Double? {
    val withDistance = sessions.filter { (it.distanceKm ?: 0.0) > 0.0 }
    if (withDistance.isEmpty()) return null
    val km = withDistance.sumOf { it.distanceKm!! }
    return if (km > 0.0) withDistance.sumOf { it.durationMinutes } / km else null
}

/** The sessions of this activity on [epochDay], or null when there are none. */
fun List<CardioSession>.dayStatsOn(epochDay: Long): CardioDayStats? {
    val ofDay = filter { it.epochDay == epochDay }
    if (ofDay.isEmpty()) return null
    val distance = ofDay.mapNotNull { it.distanceKm }.takeIf { it.isNotEmpty() }?.sum()
    return CardioDayStats(
        epochDay = epochDay,
        sessions = ofDay,
        totalMinutes = ofDay.sumOf { it.durationMinutes },
        totalDistanceKm = distance,
        paceMinPerKm = paceMinPerKm(ofDay),
    )
}

/** The most recent day strictly before [before] that has a session of this activity. */
fun List<CardioSession>.previousSessionDay(before: Long): Long? =
    filter { it.epochDay < before }.maxOfOrNull { it.epochDay }

/** Daily minutes, for `bucketBy(WEEKLY, SUM)`. */
fun List<CardioSession>.dailyMinutePoints(): List<MetricPoint> =
    groupBy { it.epochDay }
        .map { (day, sessions) -> MetricPoint(day, sessions.sumOf { it.durationMinutes }) }
        .sortedBy { it.epochDay }

/** Daily distance, for `bucketBy(WEEKLY, SUM)`. Days without any distance emit no point. */
fun List<CardioSession>.dailyDistancePoints(): List<MetricPoint> =
    groupBy { it.epochDay }
        .mapNotNull { (day, sessions) ->
            sessions.mapNotNull { it.distanceKm }.takeIf { it.isNotEmpty() }?.let { MetricPoint(day, it.sum()) }
        }
        .sortedBy { it.epochDay }

/**
 * Weekly pace, computed straight from each week's summed minutes and kilometres. This one can't go
 * through `bucketBy` at all: no per-day aggregation preserves enough information to weight the
 * days correctly afterwards (see [paceMinPerKm]).
 */
fun List<CardioSession>.weeklyPacePoints(): List<MetricPoint> =
    groupBy { DateUtils.startOfWeekEpochDay(it.epochDay) }
        .mapNotNull { (weekStart, sessions) -> paceMinPerKm(sessions)?.let { MetricPoint(weekStart, it) } }
        .sortedBy { it.epochDay }
