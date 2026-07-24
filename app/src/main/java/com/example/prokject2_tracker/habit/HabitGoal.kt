package com.example.prokject2_tracker.habit

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class GoalPeriod { DAILY, WEEKLY, MONTHLY }

/**
 * A target for a [Habit] over a given [period]. Habit-owned child, deleted along with its habit.
 * At most one goal per period per habit; a habit can have DAILY, WEEKLY and MONTHLY goals at once.
 */
@Entity(
    tableName = "habit_goals",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId"), Index(value = ["habitId", "period"], unique = true)],
)
data class HabitGoal(
    @PrimaryKey val id: String,
    val habitId: String,
    val period: GoalPeriod,
    val targetValue: Double,
    val createdAt: Instant,
)
