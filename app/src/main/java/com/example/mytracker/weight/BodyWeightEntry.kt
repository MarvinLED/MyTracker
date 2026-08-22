package com.example.mytracker.weight

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One body-weight measurement per day. Always stored in kg regardless of the user's display unit
 * preference ([com.example.mytracker.core.datastore.WeightUnit]) — only read paths convert.
 * Deterministic id ("weight-$epochDay") + upsert-on-conflict makes logging idempotent, mirroring
 * [com.example.mytracker.habit.HabitCheckIn]'s one-entry-per-day convention. Tracked/logged
 * data, never exported.
 */
@Entity(tableName = "body_weight_entries", indices = [Index("epochDay", unique = true)])
data class BodyWeightEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val weightKg: Double,
    val createdAt: Instant,
)
