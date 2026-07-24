package com.example.prokject2_tracker.fitness.cardio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projections matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyMinutesTotal(val epochDay: Long, val value: Double)
data class DailyCaloriesBurnedTotal(val epochDay: Long, val value: Double)
data class DailyAvgHeartRateTotal(val epochDay: Long, val value: Double)

@Dao
interface CardioDao {
    @Query("SELECT * FROM cardio_sessions ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<CardioSession>>

    @Query("SELECT * FROM cardio_sessions WHERE id = :id")
    suspend fun getById(id: String): CardioSession?

    @Query(
        "SELECT epochDay, SUM(durationMinutes) AS value FROM cardio_sessions " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyMinutesTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyMinutesTotal>>

    @Query(
        "SELECT epochDay, SUM(caloriesBurned) AS value FROM cardio_sessions " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyCaloriesBurnedTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyCaloriesBurnedTotal>>

    @Query(
        "SELECT epochDay, AVG(avgHeartRateBpm) AS value FROM cardio_sessions " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyAvgHeartRateTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyAvgHeartRateTotal>>

    @Upsert
    suspend fun upsert(session: CardioSession)

    @Delete
    suspend fun delete(session: CardioSession)

    @Query("SELECT COUNT(*) FROM cardio_sessions WHERE epochDay BETWEEN :startInclusive AND :endInclusive")
    suspend fun countSessionsBetween(startInclusive: Long, endInclusive: Long): Int

    @Query("SELECT COALESCE(SUM(durationMinutes),0) FROM cardio_sessions WHERE epochDay BETWEEN :startInclusive AND :endInclusive")
    suspend fun sumDurationMinutesBetween(startInclusive: Long, endInclusive: Long): Double
}
