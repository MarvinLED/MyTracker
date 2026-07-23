package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One logged strength-training entry (an exercise done for N sets x reps x weight on a day).
 * [exerciseName] is snapshotted at logging time so editing/deleting the exercise later never
 * changes history.
 */
@Entity(tableName = "strength_log_entries", indices = [Index("epochDay"), Index("exerciseId")])
data class StrengthLogEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val note: String? = null,
)
