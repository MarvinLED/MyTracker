package com.example.mytracker.bloodpressure

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

    @Query("SELECT * FROM blood_pressure_entries ORDER BY epochDay")
    suspend fun getAllOnce(): List<BloodPressureEntry>

    /** The (day, Tageszeit) slot — unique, so it and not the id is what an import matches on. */
    @Query("SELECT * FROM blood_pressure_entries WHERE epochDay = :epochDay AND timeOfDay = :timeOfDay")
    suspend fun getForDayAndTime(epochDay: Long, timeOfDay: BloodPressureTimeOfDay): BloodPressureEntry?

    /** Wipes the Blutdruckwerte for a replacing import. */
    @Query("DELETE FROM blood_pressure_entries")
    suspend fun deleteAll()
}
