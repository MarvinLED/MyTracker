package com.example.mytracker.fitness

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mytracker.core.util.GoalPeriod
import com.example.mytracker.fitness.strength.MovementDirection
import java.time.Instant

enum class FitnessGoalMetric {
    CARDIO_SESSIONS,
    CARDIO_DURATION_MINUTES,
    STRENGTH_SETS_TOTAL,
    STRENGTH_SETS_MUSCLE_GROUP,
    STRENGTH_SETS_MOVEMENT_DIRECTION,

    /**
     * One exercise's top set, as a **gain**: "2,5 kg mehr als bisher" per week or month. A gain and
     * not an absolute target, because an absolute one is already on file — the long-term goal with
     * its date, see [StrengthMaxWeightGoal] — and because week to week, what is actually decided is
     * whether to put another plate on, not what the bar should weigh in December.
     */
    STRENGTH_MAX_WEIGHT_INCREASE,

    /**
     * One exercise's total volume (Wiederholungen × Gewicht), as the gain over the period before.
     * The counterpart to the top set: volume is how much work was done, and it moves when sets or
     * reps go up rather than the weight.
     */
    STRENGTH_VOLUME_INCREASE,
}

/**
 * One target the user set. The scope columns are each only meaningful for the metrics scoped by
 * them: [muscleGroupId] for the per-muscle-group metric, [movementDirection] for the per-direction
 * one, and [exerciseId] for the two per-exercise increase metrics.
 *
 * [targetValue] is read in the metric's own unit — Sätze, Minuten, or kg of gain.
 */
@Entity(tableName = "fitness_goals")
data class FitnessGoal(
    @PrimaryKey val id: String,
    val metric: FitnessGoalMetric,
    val period: GoalPeriod,
    val muscleGroupId: String? = null,
    val targetValue: Double,
    val createdAt: Instant,
    val movementDirection: MovementDirection? = null,
    val exerciseId: String? = null,
)
