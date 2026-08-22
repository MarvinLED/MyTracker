package com.example.mytracker.fitness

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessGoalDao {
    @Query("SELECT * FROM fitness_goals")
    fun observeAll(): Flow<List<FitnessGoal>>

    @Upsert
    suspend fun upsert(goal: FitnessGoal)

    @Query("DELETE FROM fitness_goals WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM fitness_goals")
    suspend fun getAllOnce(): List<FitnessGoal>

    @Query("SELECT * FROM fitness_goals WHERE id = :id")
    suspend fun getById(id: String): FitnessGoal?

    /** Wipes the Fitness-Ziele for a replacing import. */
    @Query("DELETE FROM fitness_goals")
    suspend fun deleteAll()
}
