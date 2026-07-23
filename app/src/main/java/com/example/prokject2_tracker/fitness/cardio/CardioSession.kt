package com.example.prokject2_tracker.fitness.cardio

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class CardioActivityType { RUNNING, CYCLING, SWIMMING, WALKING, HIKING, ROWING, OTHER }

@Entity(tableName = "cardio_sessions", indices = [Index("epochDay")])
data class CardioSession(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val activityType: CardioActivityType,
    val durationMinutes: Double,
    val distanceKm: Double? = null,
    val caloriesBurned: Double,
    val note: String? = null,
)
