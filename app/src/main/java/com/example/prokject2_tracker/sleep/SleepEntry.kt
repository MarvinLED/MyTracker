package com.example.prokject2_tracker.sleep

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.prokject2_tracker.core.util.minutesBetweenTimesOfDay
import java.time.Instant

/**
 * One night's sleep.
 *
 * [epochDay] is the **morning** it ended, not the evening it started: that is the day the tiredness
 * belongs to, it is what [morningFitness] rates, and it makes "letzte Nacht" on any given day a
 * single lookup. Going to bed at 23:10 on the 4th and getting up at 6:45 on the 5th is one entry on
 * the 5th, with [startMinuteOfDay] 23:10 — which reads as "the evening before" precisely because it
 * is later in the clock than [endMinuteOfDay].
 *
 * Keyed by day through the deterministic id "sleep-$epochDay" plus a unique index, the same
 * idempotent-logging convention as [com.example.prokject2_tracker.weight.BodyWeightEntry]: correcting
 * last night's times rewrites that night instead of adding a second one.
 */
@Entity(tableName = "sleep_entries", indices = [Index(value = ["epochDay"], unique = true)])
data class SleepEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    /** Minutes since midnight — usually an evening time, i.e. the day before [epochDay]. */
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    /** How fit the morning felt, 1–10. Null when it wasn't rated. */
    val morningFitness: Int?,
    /** When the last thing before bed was eaten, minutes since midnight. Null when not recorded. */
    val lastMealMinuteOfDay: Int?,
    val createdAt: Instant,
) {
    /** Counted forwards across midnight — see [minutesBetweenTimesOfDay]. */
    val durationMinutes: Int get() = minutesBetweenTimesOfDay(startMinuteOfDay, endMinuteOfDay)

    /**
     * How long before falling asleep the last meal was, or null when none was recorded. Same
     * forward-counting: eating at 20:30 and going to bed at 23:10 is 2 h 40 min.
     */
    val minutesBetweenLastMealAndSleep: Int?
        get() = lastMealMinuteOfDay?.let { minutesBetweenTimesOfDay(it, startMinuteOfDay) }
}

/** The lowest and highest [SleepEntry.morningFitness] the rating scale offers. */
const val MIN_MORNING_FITNESS = 1
const val MAX_MORNING_FITNESS = 10

/**
 * A user-created label for a night ("heiß", "viel geträumt", "durchgeschlafen"). A library like the
 * Getränkearten: created once, then a tap per night. [sortOrder] keeps the quick-add row in the
 * order they were created, so a tag never moves under the finger.
 */
@Entity(tableName = "sleep_tags", indices = [Index(value = ["name"], unique = true)])
data class SleepTag(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Instant,
)

/** Join row attaching a [SleepTag] to a [SleepEntry]. */
@Entity(
    tableName = "sleep_entry_tags",
    primaryKeys = ["sleepEntryId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = SleepEntry::class, parentColumns = ["id"], childColumns = ["sleepEntryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = SleepTag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sleepEntryId"), Index("tagId")],
)
data class SleepEntryTag(
    val sleepEntryId: String,
    val tagId: String,
)
