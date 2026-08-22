package com.example.mytracker.fluid

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.mytracker.core.metrics.MetricPoint]'s field names. */
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

    @Query("SELECT * FROM fluid_entries WHERE sourceDiaryEntryId = :diaryEntryId")
    suspend fun getForDiaryEntry(diaryEntryId: String): List<FluidEntry>

    @Query("DELETE FROM fluid_entries WHERE sourceDiaryEntryId = :diaryEntryId")
    suspend fun deleteForDiaryEntry(diaryEntryId: String)

    @Upsert
    suspend fun upsert(entry: FluidEntry)

    @Delete
    suspend fun delete(entry: FluidEntry)

    @Query("SELECT * FROM fluid_entries ORDER BY epochDay, createdAt")
    suspend fun getAllOnce(): List<FluidEntry>

    @Query("SELECT * FROM fluid_entries WHERE id = :id")
    suspend fun getById(id: String): FluidEntry?

    /** Wipes the getrunkenen Mengen for a replacing import. */
    @Query("DELETE FROM fluid_entries")
    suspend fun deleteAll()
}
