package com.example.prokject2_tracker.fitness

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
}
