package com.example.mytracker.measurement

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

    @Query("SELECT * FROM body_measurements ORDER BY epochDay")
    suspend fun getAllOnce(): List<BodyMeasurement>

    /** One site's measurement on one day — the unique key an import has to match on, not the id. */
    @Query("SELECT * FROM body_measurements WHERE bodySiteId = :bodySiteId AND epochDay = :epochDay")
    suspend fun getForSiteAndDay(bodySiteId: String, epochDay: Long): BodyMeasurement?

    /** Wipes the Maße for a replacing import; the Körperstellen themselves stay. */
    @Query("DELETE FROM body_measurements")
    suspend fun deleteAll()
}
