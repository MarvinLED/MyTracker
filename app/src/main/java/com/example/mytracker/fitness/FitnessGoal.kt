package com.example.prokject2_tracker.fitness

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.prokject2_tracker.core.util.GoalPeriod
import com.example.prokject2_tracker.fitness.strength.MovementDirection
import java.time.Instant

enum class FitnessGoalMetric {
    CARDIO_SESSIONS,
    CARDIO_DURATION_MINUTES,
    STRENGTH_SETS_TOTAL,
    STRENGTH_SETS_MUSCLE_GROUP,
    STRENGTH_SETS_MOVEMENT_DIRECTION,
}

/** [muscleGroupId] and [movementDirection] each only apply to the one metric that is scoped by them. */
@Entity(tableName = "fitness_goals")
data class FitnessGoal(
    @PrimaryKey val id: String,
    val metric: FitnessGoalMetric,
    val period: GoalPeriod,
    val muscleGroupId: String? = null,
    val targetValue: Double,
    val createdAt: Instant,
    val movementDirection: MovementDirection? = null,
)
