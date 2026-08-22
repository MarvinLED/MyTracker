package com.example.mytracker.core.util

/** Minutes in a day — the modulus every wrap-around below is done in. */
const val MINUTES_PER_DAY = 24 * 60

/**
 * Times of day are stored as minutes since midnight rather than as an [java.time.Instant]: a night
 * is "23:10 bis 6:45", and those two clock readings are the fact worth keeping. The date they belong
 * to lives beside them (`SleepEntry.epochDay`), so nothing here needs a calendar.
 */

/** "23:05" — always two digits, so a column of times lines up. */
fun formatMinuteOfDay(minuteOfDay: Int): String {
    val wrapped = Math.floorMod(minuteOfDay, MINUTES_PER_DAY)
    return "%02d:%02d".format(wrapped / 60, wrapped % 60)
}

/**
 * Minutes from [startMinuteOfDay] to [endMinuteOfDay], counting forwards across midnight — 23:10 to
 * 6:45 is 7 h 35 min, not −16 h. Equal times are 0, not a full day: nobody logs a 24-hour night, and
 * reading it as one would put a wild outlier in every average.
 */
fun minutesBetweenTimesOfDay(startMinuteOfDay: Int, endMinuteOfDay: Int): Int =
    Math.floorMod(endMinuteOfDay - startMinuteOfDay, MINUTES_PER_DAY)

/** "7 h 35 min", or "45 min" below the hour — the shape durations are read in throughout. */
fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}

/** Whole hours as a decimal ("7,5"), for goals that are typed and read in hours rather than minutes. */
fun Int.minutesAsHours(): Double = this / 60.0
