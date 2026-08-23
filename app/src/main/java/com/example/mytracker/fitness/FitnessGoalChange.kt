package com.example.mytracker.fitness

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * One recorded change to a Fitness-Ziel, so it stays answerable later *when* a target was set or
 * moved. The same reasoning as [com.example.mytracker.goals.NutrientGoalChange]: the goal row itself
 * is overwritten in place, so without this log a target that has been raised twice looks like it was
 * always what it is now, and "seit wann trainiere ich eigentlich auf 100 kg?" has no answer at all.
 *
 * [label] is snapshotted rather than resolved on read: an exercise or muscle group can be deleted,
 * and a history that then says "Steigerung Maximalgewicht · (gelöscht)" has lost the only part that
 * made the row meaningful.
 *
 * [targetValue] null means the goal was cleared — that is a change worth keeping too, since it is
 * what explains a run of unmet weeks ending.
 */
@Entity(
    tableName = "fitness_goal_changes",
    indices = [Index(value = ["goalKey", "effectiveFromEpochDay"])],
)
data class FitnessGoalChange(
    @PrimaryKey val id: String,
    /** The goal's own id for a period goal, "maxweight-<exerciseId>" for a long-term one. */
    val goalKey: String,
    val label: String,
    val effectiveFromEpochDay: Long,
    val targetValue: Double?,
    val isPercent: Boolean = false,
    /** Set only for a long-term goal: the date it runs to, which can be moved without the target changing. */
    val targetEpochDay: Long? = null,
    val changedAt: Instant,
)

@Dao
interface FitnessGoalChangeDao {
    @Query("SELECT * FROM fitness_goal_changes ORDER BY effectiveFromEpochDay DESC, changedAt DESC")
    fun observeAll(): Flow<List<FitnessGoalChange>>

    /** Newest first — the head of this list is what is in force now. */
    @Query(
        "SELECT * FROM fitness_goal_changes WHERE goalKey = :goalKey " +
            "ORDER BY effectiveFromEpochDay DESC, changedAt DESC",
    )
    fun observeForGoal(goalKey: String): Flow<List<FitnessGoalChange>>

    @Insert
    suspend fun insert(change: FitnessGoalChange)

    @Insert
    suspend fun insertAll(changes: List<FitnessGoalChange>)

    @Query("SELECT * FROM fitness_goal_changes ORDER BY effectiveFromEpochDay, changedAt")
    suspend fun getAllOnce(): List<FitnessGoalChange>

    @Query("DELETE FROM fitness_goal_changes")
    suspend fun deleteAll()
}
