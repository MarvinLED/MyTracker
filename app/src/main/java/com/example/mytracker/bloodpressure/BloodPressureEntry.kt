package com.example.mytracker.bloodpressure

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Which of the day's two measurements this is. Blood pressure follows a daily rhythm, so a morning
 * and an evening reading are not interchangeable samples of the same thing — they are tracked, and
 * charted, apart.
 */
enum class BloodPressureTimeOfDay { MORNING, EVENING }

fun BloodPressureTimeOfDay.label(): String = when (this) {
    BloodPressureTimeOfDay.MORNING -> "Morgens"
    BloodPressureTimeOfDay.EVENING -> "Abends"
}

/**
 * One blood-pressure reading: the two values a cuff shows, in mmHg. Unlike
 * [com.example.mytracker.measurement.BodySite] there is no library of user-defined entries
 * here — systolisch and diastolisch are what a blood-pressure meter measures, so they are fixed
 * columns rather than rows of a Stellen table.
 *
 * Keyed by (day, [timeOfDay]) through the deterministic id "bloodpressure-$epochDay-$timeOfDay" plus
 * a unique index, the same idempotent-logging convention as
 * [com.example.mytracker.weight.BodyWeightEntry]: re-entering this morning's reading
 * corrects it instead of adding a second point.
 *
 * [comment] is per reading, never carried over — "nach dem Sport", "schlecht geschlafen" is exactly
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
    val systolic: Double,
    val diastolic: Double,
    val comment: String?,
    val createdAt: Instant,
)
