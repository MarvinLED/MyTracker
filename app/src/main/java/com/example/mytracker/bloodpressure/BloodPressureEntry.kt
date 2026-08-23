package com.example.mytracker.bloodpressure

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Which half of the day a reading belongs to. Blood pressure follows a daily rhythm, so a morning
 * and an evening reading are not interchangeable samples of the same thing — they are tracked, and
 * charted, apart. Not to be confused with the two *measurements* one such slot can hold, which are
 * the same reading taken twice — see [BloodPressureEntry].
 */
enum class BloodPressureTimeOfDay { MORNING, EVENING }

/** Blood pressure is always in mmHg — no unit choice anywhere on this screen. */
const val BLOOD_PRESSURE_UNIT = "mmHg"

/** Beats per minute, written the way a cuff writes it: "72/min". */
const val PULSE_UNIT = "/min"

fun BloodPressureTimeOfDay.label(): String = when (this) {
    BloodPressureTimeOfDay.MORNING -> "Morgens"
    BloodPressureTimeOfDay.EVENING -> "Abends"
}

/**
 * One blood-pressure reading: the two values a cuff shows, in mmHg, plus the pulse it usually shows
 * alongside them. Unlike [com.example.mytracker.measurement.BodySite] there is no library of
 * user-defined entries here — systolisch, diastolisch and Puls are what a blood-pressure meter
 * measures, so they are fixed columns rather than rows of a Stellen table.
 *
 * A slot holds **up to two measurements**, because that is how a cuff is meant to be used: measure
 * twice a few minutes apart and go by the mean, since the first reading of a sitting is regularly
 * the highest. Both are stored raw — [systolic2] and friends are null until a second one is taken —
 * and the value that counts is derived from them ([averageSystolic] and friends). Storing the mean
 * instead of the readings would make the form unable to show what was actually measured, and the
 * two raw numbers are also the only way to see a first-reading effect at all.
 *
 * [pulse] is optional on its own: not every meter shows one, and a reading is complete without it.
 * A second measurement without a pulse still averages the cuff values — each column averages over
 * the measurements that carry it.
 *
 * Keyed by (day, [timeOfDay]) through the deterministic id "bloodpressure-$epochDay-$timeOfDay" plus
 * a unique index, the same idempotent-logging convention as
 * [com.example.mytracker.weight.BodyWeightEntry]: re-entering this morning's reading
 * corrects it instead of adding a second point.
 *
 * [comment] is per slot, never carried over — "nach dem Sport", "schlecht geschlafen" is exactly
 * the context that stops a one-off outlier from being read as a trend.
 */
@Entity(
    tableName = "blood_pressure_entries",
    indices = [Index(value = ["epochDay", "timeOfDay"], unique = true)],
)
data class BloodPressureEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val timeOfDay: BloodPressureTimeOfDay,
    /** Measurement 1 — always present; a slot exists because this reading was taken. */
    val systolic: Double,
    val diastolic: Double,
    val pulse: Double? = null,
    /** Measurement 2 — null for a slot that was measured once. */
    val systolic2: Double? = null,
    val diastolic2: Double? = null,
    val pulse2: Double? = null,
    val comment: String?,
    val createdAt: Instant,
) {
    /** True once a second cuff reading is on file, i.e. once the stored values are means of two. */
    val hasSecondMeasurement: Boolean get() = systolic2 != null || diastolic2 != null

    /** What the chart, the history and every average of a day read: the mean of what was measured. */
    val averageSystolic: Double get() = meanOf(systolic, systolic2)
    val averageDiastolic: Double get() = meanOf(diastolic, diastolic2)

    /**
     * Null when neither measurement carried a pulse. One pulse across two measurements is that
     * pulse, not half of it — the mean is taken over the readings that have one.
     */
    val averagePulse: Double? get() = listOfNotNull(pulse, pulse2).takeIf { it.isNotEmpty() }?.average()
}

/** The mean of the measurements that exist. [second] null means the slot was measured once. */
private fun meanOf(first: Double, second: Double?): Double =
    if (second == null) first else (first + second) / 2.0
