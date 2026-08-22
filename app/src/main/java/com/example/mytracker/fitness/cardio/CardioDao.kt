package com.example.mytracker.fitness.cardio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projections matching [com.example.mytracker.core.metrics.MetricPoint]'s field names. */
data class DailyMinutesTotal(val epochDay: Long, val value: Double)
data class DailyCaloriesBurnedTotal(val epochDay: Long, val value: Double)
data class DailyAvgHeartRateTotal(val epochDay: Long, val value: Double)
data class DailySessionCount(val epochDay: Long, val value: Double)
data class DailyDistanceKmTotal(val epochDay: Long, val value: Double)
data class DailyAvgPace(val epochDay: Long, val value: Double)

/** When each activity type was last done — the subtitle in the Kardio list. */
data class ActivityTypeLastTrained(val activityTypeId: String, val epochDay: Long)

@Dao
interface CardioDao {
    @Query("SELECT * FROM cardio_sessions ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<CardioSession>>

    @Query("SELECT * FROM cardio_sessions WHERE id = :id")
    suspend fun getById(id: String): CardioSession?

    /**
     * One activity's whole history, newest first — the detail page derives its day stats and weekly
     * chart from these rows. No index on `activityTypeId`: the table is a personal log (a few
     * hundred rows a year) and adding one would cost a schema migration for a scan that is already
     * far below a frame.
     */
    @Query("SELECT * FROM cardio_sessions WHERE activityTypeId = :activityTypeId ORDER BY epochDay DESC, createdAt DESC")
    fun observeForActivityType(activityTypeId: String): Flow<List<CardioSession>>

    @Query("SELECT activityTypeId, MAX(epochDay) AS epochDay FROM cardio_sessions GROUP BY activityTypeId")
    fun observeLastSessionDayPerActivityType(): Flow<List<ActivityTypeLastTrained>>

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

    @Query(
        "SELECT epochDay, CAST(COUNT(*) AS REAL) AS value FROM cardio_sessions " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailySessionCountTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailySessionCount>>

    @Query(
        "SELECT epochDay, SUM(distanceKm) AS value FROM cardio_sessions " +
            "WHERE distanceKm IS NOT NULL AND epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyDistanceKmTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyDistanceKmTotal>>

    @Query(
        "SELECT epochDay, SUM(durationMinutes) / SUM(distanceKm) AS value FROM cardio_sessions " +
            "WHERE distanceKm IS NOT NULL AND distanceKm > 0 AND epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyAvgPace(startInclusive: Long, endInclusive: Long): Flow<List<DailyAvgPace>>

    @Upsert
    suspend fun upsert(session: CardioSession)

    @Delete
    suspend fun delete(session: CardioSession)

    @Query("SELECT COUNT(*) FROM cardio_sessions WHERE epochDay BETWEEN :startInclusive AND :endInclusive")
    suspend fun countSessionsBetween(startInclusive: Long, endInclusive: Long): Int

    @Query("SELECT COALESCE(SUM(durationMinutes),0) FROM cardio_sessions WHERE epochDay BETWEEN :startInclusive AND :endInclusive")
    suspend fun sumDurationMinutesBetween(startInclusive: Long, endInclusive: Long): Double

    @Query("SELECT * FROM cardio_sessions ORDER BY epochDay, createdAt")
    suspend fun getAllOnce(): List<CardioSession>

    /** Wipes the Kardio-Einheiten for a replacing import; the Kardio-Arten themselves stay. */
    @Query("DELETE FROM cardio_sessions")
    suspend fun deleteAll()
}
