package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class MuscleGroup { CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE, FULL_BODY, OTHER }

/** A user-created Kraftübung (strength exercise), part of the exercise library. */
@Entity(tableName = "strength_exercises", indices = [Index("name")])
data class StrengthExercise(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val createdAt: Instant,
    val updatedAt: Instant,
)
