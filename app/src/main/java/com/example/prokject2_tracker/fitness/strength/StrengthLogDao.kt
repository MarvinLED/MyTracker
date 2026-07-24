package com.example.prokject2_tracker.fitness.strength

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyActiveFlag(val epochDay: Long, val value: Double)

@Dao
interface StrengthLogDao {
    @Query("SELECT * FROM strength_log_entries ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<StrengthLogEntry>>

    @Query("SELECT * FROM strength_log_entries WHERE id = :id")
    suspend fun getById(id: String): StrengthLogEntry?

    @Query(
        "SELECT epochDay, 1.0 AS value FROM strength_log_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay",
    )
    fun observeDailyActiveFlag(startInclusive: Long, endInclusive: Long): Flow<List<DailyActiveFlag>>

    @Upsert
    suspend fun upsert(entry: StrengthLogEntry)

    @Delete
    suspend fun delete(entry: StrengthLogEntry)
}
