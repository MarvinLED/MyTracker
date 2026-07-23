package com.example.prokject2_tracker.habit

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** One completed check-in of a [Habit] on a given day. Tracked/logged data, never exported. */
@Entity(
    tableName = "habit_check_ins",
    indices = [Index("epochDay"), Index(value = ["habitId", "epochDay"], unique = true)],
)
data class HabitCheckIn(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val createdAt: Instant,
)
