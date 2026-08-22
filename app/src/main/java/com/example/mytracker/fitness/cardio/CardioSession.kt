package com.example.mytracker.fitness.cardio

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "cardio_sessions", indices = [Index("epochDay")])
data class CardioSession(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val activityTypeId: String,
    val activityTypeName: String,
    val durationMinutes: Double,
    val distanceKm: Double? = null,
    val caloriesBurned: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val note: String? = null,
)
