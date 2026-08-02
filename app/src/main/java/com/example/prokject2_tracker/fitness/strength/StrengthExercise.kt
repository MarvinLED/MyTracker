package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A user-created Kraftübung (strength exercise), part of the exercise library.
 * [movementDirection] is optional — exercises tagged before it existed, and ones the user simply
 * doesn't want to classify, stay null.
 */
@Entity(tableName = "strength_exercises", indices = [Index("name")])
data class StrengthExercise(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val movementDirection: MovementDirection? = null,
    /**
     * Klimmzüge and friends: the body is the load, so logging one starts in bodyweight mode and the
     * weight steppers count *added* weight. Only the default for new sets — any exercise can still
     * log either kind of set.
     */
    val isBodyweight: Boolean = false,
)
