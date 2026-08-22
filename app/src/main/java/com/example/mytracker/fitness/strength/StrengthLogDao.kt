package com.example.mytracker.fitness.strength

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.mytracker.core.metrics.MetricPoint]'s field names. */
data class DailyActiveFlag(val epochDay: Long, val value: Double)

@Dao
interface StrengthLogDao {
    @Query("SELECT * FROM strength_log_entries ORDER BY epochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<StrengthLogEntry>>

    @Query("SELECT * FROM strength_log_entries WHERE id = :id")
    suspend fun getById(id: String): StrengthLogEntry?

    /**
     * Every entry this exercise has on one day, oldest first. Normally one — but nothing in the
     * schema enforces that, and the outgoing entry form could create several, so the session layer
     * has to see all of them to merge them.
     */
    @Query(
        "SELECT * FROM strength_log_entries WHERE exerciseId = :exerciseId AND epochDay = :epochDay " +
            "ORDER BY createdAt",
    )
    suspend fun getForExerciseOnDay(exerciseId: String, epochDay: Long): List<StrengthLogEntry>

    @Query("SELECT * FROM strength_log_entries WHERE exerciseId = :exerciseId ORDER BY epochDay DESC, createdAt DESC")
    fun observeForExercise(exerciseId: String): Flow<List<StrengthLogEntry>>

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

    @Query("SELECT * FROM strength_log_entries ORDER BY epochDay, createdAt")
    suspend fun getAllOnce(): List<StrengthLogEntry>

    /** Wipes the Krafttraining log for a replacing import; the Sätze cascade with the entries. */
    @Query("DELETE FROM strength_log_entries")
    suspend fun deleteAll()
}
