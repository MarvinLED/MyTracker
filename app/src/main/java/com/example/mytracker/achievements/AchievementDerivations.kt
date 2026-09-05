package com.example.mytracker.achievements

import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.fitness.strength.StrengthSet
import com.example.mytracker.fitness.strength.maxWeightOf
import com.example.mytracker.fitness.strength.toDraft
import com.example.mytracker.fitness.strength.volumeOf

/**
 * A best mark and what it displaced. [previous] is the point of it: "102,5 kg" is a number, but
 * "102,5 kg, und davor waren es 100" is a story, and it is what turns a wall of badges into a record
 * of what actually happened.
 */
data class RecordMark(val value: Double, val epochDay: Long, val previous: Double?)

/**
 * The best of [points] and the day it was first reached, together with the mark it beat.
 *
 * Walked in day order rather than simply taking the maximum, because [RecordMark.previous] only
 * exists in that order — and because the *first* day the best was reached is the day it was set. A
 * repeat of the same value later is not a new record, so it must not move the date.
 *
 * [higherIsBetter] false turns this around for the marks where less is the achievement, such as the
 * lowest body weight of a year.
 */
fun recordMark(points: List<MetricPoint>, higherIsBetter: Boolean = true): RecordMark? {
    var best: RecordMark? = null
    for (point in points.sortedBy { it.epochDay }) {
        val standing = best
        val beaten = standing == null ||
            (if (higherIsBetter) point.value > standing.value else point.value < standing.value)
        if (beaten) best = RecordMark(point.value, point.epochDay, standing?.value)
    }
    return best
}

/** Every exercise's heaviest set as a record, keyed by exercise. Bodyweight-only days carry none. */
fun topSetRecords(sets: List<StrengthSet>): Map<String, RecordMark> = sets
    .groupBy { it.exerciseId }
    .mapNotNull { (exerciseId, exerciseSets) ->
        recordMark(exerciseSets.dailyTopSetPoints())?.let { exerciseId to it }
    }
    .toMap()

/**
 * The heaviest external weight per day. A bodyweight-only day contributes no point at all rather
 * than a zero — see [maxWeightOf]; counting it as 0 kg would read as a catastrophic day.
 */
private fun List<StrengthSet>.dailyTopSetPoints(): List<MetricPoint> = groupBy { it.epochDay }
    .mapNotNull { (day, daySets) -> maxWeightOf(daySets.map { it.toDraft() })?.let { MetricPoint(day, it) } }

/**
 * Volume per calendar week, keyed by the week's Monday — what "größte Wochentonnage" is read off.
 * Weeks without sets are absent, which is right: a week off is not a week of zero tonnes to be
 * beaten, it simply has no entry in the record book.
 */
fun weeklyVolumePoints(sets: List<StrengthSet>): List<MetricPoint> = sets
    .groupBy { DateUtils.startOfWeekEpochDay(it.epochDay) }
    .map { (weekStart, weekSets) -> MetricPoint(weekStart, volumeOf(weekSets.map { it.toDraft() })) }
    .sortedBy { it.epochDay }

/** Every kilo ever moved. Bodyweight sets contribute nothing, exactly as the goals count them. */
fun totalVolume(sets: List<StrengthSet>): Double = volumeOf(sets.map { it.toDraft() })

/**
 * How many strength sessions were logged. A session is the pair (exercise, day) and not a log entry,
 * the same definition the exercise detail screen uses — nothing stops a day from holding two entries
 * for one exercise, and counting those as two sessions would inflate the tally.
 */
fun strengthSessionCount(sets: List<StrengthSet>): Int =
    sets.map { it.exerciseId to it.epochDay }.distinct().size
