package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One logged set within a [StrengthLogEntry]. [epochDay]/[exerciseId] are denormalized from the
 * parent entry (like [com.example.prokject2_tracker.fitness.cardio.CardioSession] snapshots its
 * activity type) so "latest weight for this exercise" needs no join. "Sets per muscle group" is
 * computed by joining [exerciseId] against the exercise's *current* [StrengthExerciseMuscleGroup]
 * assignments rather than snapshotting a muscle group here, since one exercise can target several.
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
    indices = [Index("logEntryId"), Index("epochDay"), Index("exerciseId")],
)
data class StrengthSet(
    @PrimaryKey val id: String,
    val logEntryId: String,
    val epochDay: Long,
    val exerciseId: String,
    val setIndex: Int,
    val reps: Int,
    /** External weight only — the added plates on a bodyweight set. See [SetDraft.weightKg]. */
    val weightKg: Double?,
    /** True when the body was the load (Klimmzüge). See [SetDraft.isBodyweight]. */
    val isBodyweight: Boolean = false,
)
