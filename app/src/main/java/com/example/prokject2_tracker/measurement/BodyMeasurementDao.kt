package com.example.prokject2_tracker.measurement

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY epochDay")
    fun observeAll(): Flow<List<BodyMeasurement>>

    @Query("SELECT COUNT(*) FROM body_measurements WHERE bodySiteId = :bodySiteId")
    suspend fun countForSite(bodySiteId: String): Int

    @Upsert
    suspend fun upsert(measurement: BodyMeasurement)

    @Delete
    suspend fun delete(measurement: BodyMeasurement)
}
