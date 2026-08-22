package com.example.prokject2_tracker.fitness.strength

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MuscleGroupDao {
    @Query("SELECT * FROM muscle_groups ORDER BY sortOrder")
    fun observeAll(): Flow<List<MuscleGroup>>

    @Query("SELECT * FROM muscle_groups ORDER BY sortOrder")
    suspend fun getAllOnce(): List<MuscleGroup>

    @Query("SELECT * FROM muscle_groups WHERE id = :id")
    suspend fun getById(id: String): MuscleGroup?

    @Upsert
    suspend fun upsert(group: MuscleGroup)

    @Upsert
    suspend fun upsertAll(groups: List<MuscleGroup>)

    @Delete
    suspend fun delete(group: MuscleGroup)

    @Query("SELECT EXISTS(SELECT 1 FROM strength_exercise_muscle_groups WHERE muscleGroupId = :id)")
    suspend fun isUsedInAnyEntry(id: String): Boolean

    /** Wipes the Muskelgruppen for a replacing import; the exercise cross-refs cascade with them. */
    @Query("DELETE FROM muscle_groups")
    suspend fun deleteAll()
}
