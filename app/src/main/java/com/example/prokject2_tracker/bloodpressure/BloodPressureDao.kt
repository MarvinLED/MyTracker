package com.example.prokject2_tracker.bloodpressure

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodPressureDao {
    @Query("SELECT * FROM blood_pressure_entries ORDER BY epochDay")
    fun observeAll(): Flow<List<BloodPressureEntry>>

    @Upsert
    suspend fun upsert(entry: BloodPressureEntry)

    @Delete
    suspend fun delete(entry: BloodPressureEntry)
}
