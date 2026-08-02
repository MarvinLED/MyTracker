package com.example.prokject2_tracker.sleep

import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.util.MINUTES_PER_DAY
import com.example.prokject2_tracker.core.util.formatDuration
import com.example.prokject2_tracker.core.util.formatMinuteOfDay

/**
 * A bedtime is compared on an evening timeline, not a clock: 00:30 is *later* than 23:00, even
 * though its minute-of-day number is smaller. Everything from this hour onwards counts as "the
 * evening being measured"; anything before it is the small hours of the next morning and sorts
 * after. 18:00 leaves room for an early night without ever catching an afternoon nap's start.
 */
private const val EVENING_STARTS_AT_MINUTE = 18 * 60

/** Position on that evening timeline — see [EVENING_STARTS_AT_MINUTE]. */
fun bedtimeOrderKey(minuteOfDay: Int): Int =
    if (minuteOfDay < EVENING_STARTS_AT_MINUTE) minuteOfDay + MINUTES_PER_DAY else minuteOfDay

/**
 * Whether [startMinuteOfDay] is at or before the goal bedtime. A goal of "23:00" is met by 22:40 and
 * by 23:00 itself, and missed by 23:20 and by 00:30.
 */
fun isBedtimeMet(startMinuteOfDay: Int, goalMinuteOfDay: Int): Boolean =
    bedtimeOrderKey(startMinuteOfDay) <= bedtimeOrderKey(goalMinuteOfDay)

/**
 * How far off the goal bedtime a night was, in minutes: negative early, positive late. The number
 * the "23:20 · 20 min zu spät" line is built from.
 */
fun bedtimeDeviationMinutes(startMinuteOfDay: Int, goalMinuteOfDay: Int): Int =
    bedtimeOrderKey(startMinuteOfDay) - bedtimeOrderKey(goalMinuteOfDay)

/** "20 min zu spät" / "15 min früher" / "genau pünktlich" — how a bedtime reads against its goal. */
fun bedtimeDeviationLabel(startMinuteOfDay: Int, goalMinuteOfDay: Int): String {
    val deviation = bedtimeDeviationMinutes(startMinuteOfDay, goalMinuteOfDay)
    return when {
        deviation == 0 -> "genau pünktlich"
        deviation > 0 -> "${formatDuration(deviation)} zu spät"
        else -> "${formatDuration(-deviation)} früher"
    }
}

/**
 * The two sleep goals as the Tagesziele screen needs them. Kept here rather than in `goals/` so the
 * bedtime's evening-timeline rule lives next to the only code that understands it.
 *
 * Both are optional and independent: a duration goal without a bedtime is a perfectly ordinary
 * setup, and a night that isn't logged at all shows as 0 rather than being hidden — an unlogged
 * night is exactly what the screen should nag about.
 */
data class SleepGoalStatus(
    val label: String,
    val valueText: String,
    val isMet: Boolean,
    /** Null for the bedtime: "23:20 statt 23:00" is not a matter of degree, so it gets no bar. */
    val fraction: Float?,
)

fun sleepGoalStatuses(
    entry: SleepEntry?,
    durationGoalMinutes: NutrientGoal?,
    bedtimeGoalMinuteOfDay: Int?,
): List<SleepGoalStatus> = buildList {
    if (durationGoalMinutes != null && !durationGoalMinutes.isEmpty) {
        val slept = entry?.durationMinutes ?: 0
        add(
            SleepGoalStatus(
                label = "Schlafdauer",
                valueText = "${formatDuration(slept)} / ${durationGoalLabel(durationGoalMinutes)}",
                isMet = entry != null && durationGoalMinutes.isMetBy(slept.toDouble()),
                fraction = durationGoalMinutes.fractionOf(slept.toDouble()),
            ),
        )
    }
    if (bedtimeGoalMinuteOfDay != null) {
        val start = entry?.startMinuteOfDay
        add(
            SleepGoalStatus(
                label = "Schlafenszeit",
                valueText = if (start == null) {
                    "noch nicht eingetragen · Ziel ${formatMinuteOfDay(bedtimeGoalMinuteOfDay)}"
                } else {
                    "${formatMinuteOfDay(start)} · ${bedtimeDeviationLabel(start, bedtimeGoalMinuteOfDay)}"
                },
                isMet = start != null && isBedtimeMet(start, bedtimeGoalMinuteOfDay),
                fraction = null,
            ),
        )
    }
}

/** "mind. 7 h", "höchstens 9 h", or "7 h – 9 h" — the target a duration goal states. */
fun durationGoalLabel(goal: NutrientGoal): String {
    val min = goal.min?.toInt()
    val max = goal.max?.toInt()
    return when {
        min != null && max != null -> "${formatDuration(min)} – ${formatDuration(max)}"
        min != null -> "mind. ${formatDuration(min)}"
        max != null -> "höchstens ${formatDuration(max)}"
        else -> "—"
    }
}
