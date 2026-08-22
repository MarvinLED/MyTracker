package com.example.prokject2_tracker.bloodpressure

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class BloodPressureRepository @Inject constructor(
    private val bloodPressureDao: BloodPressureDao,
) {
    fun observeAll(): Flow<List<BloodPressureEntry>> = bloodPressureDao.observeAll()

    /** Upserts at the deterministic id for (day, time of day) — see [BloodPressureEntry]. */
    suspend fun logEntry(
        epochDay: Long,
        timeOfDay: BloodPressureTimeOfDay,
        systolic: Double,
        diastolic: Double,
        comment: String?,
    ) {
        bloodPressureDao.upsert(
            BloodPressureEntry(
                id = "bloodpressure-$epochDay-$timeOfDay",
                epochDay = epochDay,
                timeOfDay = timeOfDay,
                systolic = systolic,
                diastolic = diastolic,
                comment = comment?.trim()?.takeIf { it.isNotBlank() },
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun delete(entry: BloodPressureEntry) {
        bloodPressureDao.delete(entry)
    }
}
