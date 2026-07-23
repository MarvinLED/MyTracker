package com.example.prokject2_tracker.nutrition.diary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyKcalTotal(val epochDay: Long, val value: Double)

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE epochDay = :epochDay ORDER BY mealType, createdAt")
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>>

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM diary_entries WHERE epochDay = :epochDay")
    fun observeDayTotalKcal(epochDay: Long): Flow<Double>

    @Query(
        "SELECT epochDay, SUM(kcal) AS value FROM diary_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyKcalTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyKcalTotal>>

    @Upsert
    suspend fun upsert(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)
}
