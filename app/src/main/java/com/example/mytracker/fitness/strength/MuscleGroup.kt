package com.example.prokject2_tracker.fitness.strength

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A user-manageable muscle group (Brust, Rücken, Beine, ...), part of the Muskelgruppen library.
 * [sortOrder] keeps the user's/default ordering stable regardless of creation order.
 */
@Entity(tableName = "muscle_groups", indices = [Index("name")])
data class MuscleGroup(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Instant,
)
