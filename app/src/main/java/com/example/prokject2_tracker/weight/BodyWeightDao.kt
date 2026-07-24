package com.example.prokject2_tracker.weight

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyWeightEntry>>

    @Query("SELECT * FROM body_weight_entries WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<BodyWeightEntry?>

    @Query(
        "SELECT * FROM body_weight_entries WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "ORDER BY epochDay",
    )
    fun observeRange(startInclusive: Long, endInclusive: Long): Flow<List<BodyWeightEntry>>

    @Upsert
    suspend fun upsert(entry: BodyWeightEntry)

    @Delete
    suspend fun delete(entry: BodyWeightEntry)
}
