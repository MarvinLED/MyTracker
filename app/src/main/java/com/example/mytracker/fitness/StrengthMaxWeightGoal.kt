package com.example.mytracker.fitness

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * The long-term target for one exercise's top set: "Bankdrücken 100 kg bis zum 31.12.".
 *
 * Its own table rather than another [FitnessGoal] row, because it is a different kind of goal: it
 * has no period to recur in and a date it runs to instead, and it is reached once rather than met
 * again every week.
 *
 * [startWeightKg] and [startEpochDay] are snapshotted when the goal is set, and that is the whole
 * point of them: without a starting point, "auf Kurs" cannot be computed at all — only "reached"
 * or "not reached", which on a six-month goal is no answer for five and a half of those months.
 * They are kept when the target is edited later, so moving the date does not silently restart the
 * plan from today's weight.
 *
 * One goal per exercise ([exerciseId] unique): two targets for the same lift with different dates
 * would be two answers to "am I on track?".
 */
@Entity(
    tableName = "strength_max_weight_goals",
    indices = [Index(value = ["exerciseId"], unique = true)],
)
data class StrengthMaxWeightGoal(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val targetWeightKg: Double,
    val targetEpochDay: Long,
    val startWeightKg: Double,
    val startEpochDay: Long,
    val createdAt: Instant,
)

@Dao
interface StrengthMaxWeightGoalDao {
    @Query("SELECT * FROM strength_max_weight_goals")
    fun observeAll(): Flow<List<StrengthMaxWeightGoal>>

    @Query("SELECT * FROM strength_max_weight_goals")
    suspend fun getAllOnce(): List<StrengthMaxWeightGoal>

    @Query("SELECT * FROM strength_max_weight_goals WHERE exerciseId = :exerciseId")
    suspend fun getForExercise(exerciseId: String): StrengthMaxWeightGoal?

    @Upsert
    suspend fun upsert(goal: StrengthMaxWeightGoal)

    @Query("DELETE FROM strength_max_weight_goals WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: String)

    /** Wipes the Langzeit-Ziele for a replacing import. */
    @Query("DELETE FROM strength_max_weight_goals")
    suspend fun deleteAll()
}
