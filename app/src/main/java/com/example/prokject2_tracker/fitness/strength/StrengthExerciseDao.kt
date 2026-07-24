package com.example.prokject2_tracker.fitness.strength

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StrengthExerciseDao {
    @Query("SELECT * FROM strength_exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<StrengthExercise>>

    @Query("SELECT * FROM strength_exercises WHERE id = :id")
    suspend fun getById(id: String): StrengthExercise?

    @Query("SELECT * FROM strength_exercises ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<StrengthExercise>

    @Upsert
    suspend fun upsert(exercise: StrengthExercise)

    @Delete
    suspend fun delete(exercise: StrengthExercise)

    @Query("SELECT EXISTS(SELECT 1 FROM strength_log_entries WHERE exerciseId = :exerciseId)")
    suspend fun isUsedInAnyLogEntry(exerciseId: String): Boolean

    @Query("SELECT * FROM strength_exercise_muscle_groups")
    fun observeAllExerciseMuscleGroups(): Flow<List<StrengthExerciseMuscleGroup>>

    @Query("SELECT * FROM strength_exercise_muscle_groups WHERE exerciseId = :exerciseId")
    suspend fun getMuscleGroupCrossRefsForExercise(exerciseId: String): List<StrengthExerciseMuscleGroup>

    @Query("DELETE FROM strength_exercise_muscle_groups WHERE exerciseId = :exerciseId")
    suspend fun deleteAllMuscleGroupsForExercise(exerciseId: String)

    @Insert
    suspend fun insertExerciseMuscleGroups(rows: List<StrengthExerciseMuscleGroup>)

    /** Wholesale-replaces an exercise's muscle-group associations (delete-then-insert). */
    @Transaction
    suspend fun replaceMuscleGroupsForExercise(exerciseId: String, muscleGroupIds: List<String>) {
        deleteAllMuscleGroupsForExercise(exerciseId)
        if (muscleGroupIds.isNotEmpty()) {
            insertExerciseMuscleGroups(muscleGroupIds.map { StrengthExerciseMuscleGroup(exerciseId = exerciseId, muscleGroupId = it) })
        }
    }
}
