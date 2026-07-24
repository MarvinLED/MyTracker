package com.example.prokject2_tracker.fitness.cardio

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A user-manageable cardio activity type (Laufen, Radfahren, Schwimmen, ...), part of the
 * Cardio-Aktivitäten library. [sortOrder] keeps the user's/default ordering stable regardless of
 * creation order.
 */
@Entity(tableName = "cardio_activity_types", indices = [Index("name")])
data class CardioActivityType(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAt: Instant,
)
