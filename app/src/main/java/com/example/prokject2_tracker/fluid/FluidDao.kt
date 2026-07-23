package com.example.prokject2_tracker.fluid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyMlTotal(val epochDay: Long, val value: Double)

@Dao
interface FluidDao {
    @Query("SELECT * FROM fluid_entries WHERE epochDay = :epochDay ORDER BY createdAt")
    fun observeForDay(epochDay: Long): Flow<List<FluidEntry>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM fluid_entries WHERE epochDay = :epochDay")
    fun observeDayTotalMl(epochDay: Long): Flow<Double>

    @Query(
        "SELECT epochDay, SUM(amountMl) AS value FROM fluid_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyMlTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyMlTotal>>

    @Upsert
    suspend fun upsert(entry: FluidEntry)

    @Delete
    suspend fun delete(entry: FluidEntry)
}
