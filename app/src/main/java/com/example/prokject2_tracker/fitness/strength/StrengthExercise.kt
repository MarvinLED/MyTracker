package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A user-created Kraftübung (strength exercise), part of the exercise library. */
@Entity(tableName = "strength_exercises", indices = [Index("name")])
data class StrengthExercise(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroupId: String,
    val muscleGroupName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
