package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged set within a [StrengthLogEntry]. [epochDay]/[exerciseId]/[muscleGroupId] are
 * denormalized from the parent entry/exercise (like [com.example.prokject2_tracker.fitness.cardio.CardioSession]
 * snapshots its activity type) so "latest weight for this exercise" and "sets per muscle group"
 * queries need no join.
 */
@Entity(
    tableName = "strength_sets",
    foreignKeys = [
        ForeignKey(
            entity = StrengthLogEntry::class,
            parentColumns = ["id"],
            childColumns = ["logEntryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("logEntryId"), Index("epochDay"), Index("exerciseId"), Index("muscleGroupId")],
)
data class StrengthSet(
    @PrimaryKey val id: String,
    val logEntryId: String,
    val epochDay: Long,
    val exerciseId: String,
    val muscleGroupId: String,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double?,
)
