package com.example.mytracker.goals

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.mytracker.core.datastore.Nutrient
import kotlinx.coroutines.flow.Flow

@Dao
interface NutrientGoalChangeDao {
    /**
     * Every recorded change, oldest first. The whole log rather than a date-windowed slice: the
     * change in force at the start of a window is usually *older* than the window, so a `BETWEEN`
     * here would drop exactly the row the chart needs to know where the line starts.
     */
    @Query("SELECT * FROM nutrient_goal_changes ORDER BY effectiveFromEpochDay, changedAt")
    fun observeAll(): Flow<List<NutrientGoalChange>>

    @Query("SELECT * FROM nutrient_goal_changes ORDER BY effectiveFromEpochDay, changedAt")
    suspend fun getAllOnce(): List<NutrientGoalChange>

    @Query("SELECT COUNT(*) FROM nutrient_goal_changes WHERE nutrient = :nutrient")
    suspend fun countForNutrient(nutrient: Nutrient): Int

    @Insert
    suspend fun insert(change: NutrientGoalChange)

    @Insert
    suspend fun insertAll(changes: List<NutrientGoalChange>)

    @Query("DELETE FROM nutrient_goal_changes")
    suspend fun deleteAll()
}
