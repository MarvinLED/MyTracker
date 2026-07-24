package com.example.prokject2_tracker.habit

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyCompletedCount(val epochDay: Long, val value: Double)

@Dao
interface HabitCheckInDao {
    @Query("SELECT * FROM habit_check_ins WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<HabitCheckIn>>

    @Query("SELECT * FROM habit_check_ins WHERE habitId = :habitId")
    suspend fun getAllForHabit(habitId: String): List<HabitCheckIn>

    @Upsert
    suspend fun upsert(checkIn: HabitCheckIn)

    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteForHabitAndDay(habitId: String, epochDay: Long)

    @Query(
        "SELECT epochDay, COUNT(*) AS value FROM habit_check_ins " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyCompletedCounts(startInclusive: Long, endInclusive: Long): Flow<List<DailyCompletedCount>>
}
