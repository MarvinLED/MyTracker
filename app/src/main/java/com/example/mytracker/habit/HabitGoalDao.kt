package com.example.prokject2_tracker.habit

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.prokject2_tracker.core.util.GoalPeriod
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitGoalDao {
    @Query("SELECT * FROM habit_goals WHERE habitId = :habitId")
    fun observeForHabit(habitId: String): Flow<List<HabitGoal>>

    @Query("SELECT * FROM habit_goals WHERE habitId = :habitId AND period = :period")
    suspend fun getForHabitAndPeriod(habitId: String, period: GoalPeriod): HabitGoal?

    @Upsert
    suspend fun upsert(goal: HabitGoal)

    @Query("DELETE FROM habit_goals WHERE habitId = :habitId AND period = :period")
    suspend fun deleteForHabitAndPeriod(habitId: String, period: GoalPeriod)

    @Query("SELECT * FROM habit_goals")
    fun observeAll(): Flow<List<HabitGoal>>

    @Query("SELECT * FROM habit_goals")
    suspend fun getAllOnce(): List<HabitGoal>

    @Query("DELETE FROM habit_goals WHERE habitId = :habitId")
    suspend fun deleteAllForHabit(habitId: String)

    @Upsert
    suspend fun upsertAll(goals: List<HabitGoal>)

    /** Wholesale-replaces a habit's goals (delete-then-insert). */
    @Transaction
    suspend fun replaceGoalsForHabit(habitId: String, goals: List<HabitGoal>) {
        deleteAllForHabit(habitId)
        if (goals.isNotEmpty()) {
            upsertAll(goals)
        }
    }
}
