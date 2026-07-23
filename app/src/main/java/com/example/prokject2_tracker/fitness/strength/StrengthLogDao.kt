package com.example.prokject2_tracker.fitness.strength

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projections matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailySetsTotal(val epochDay: Long, val value: Double)
data class DailyVolumeTotal(val epochDay: Long, val value: Double)

@Dao
interface StrengthLogDao {
    @Query("SELECT * FROM strength_log_entries ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<StrengthLogEntry>>

    @Query("SELECT * FROM strength_log_entries WHERE id = :id")
    suspend fun getById(id: String): StrengthLogEntry?

    @Query(
        "SELECT epochDay, SUM(sets) AS value FROM strength_log_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailySetsTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>>

    @Query(
        "SELECT epochDay, SUM(sets * reps * weightKg) AS value FROM strength_log_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyVolumeTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>>

    @Upsert
    suspend fun upsert(entry: StrengthLogEntry)

    @Delete
    suspend fun delete(entry: StrengthLogEntry)
}
