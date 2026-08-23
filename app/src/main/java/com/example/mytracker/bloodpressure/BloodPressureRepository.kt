package com.example.mytracker.bloodpressure

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class BloodPressureRepository @Inject constructor(
    private val bloodPressureDao: BloodPressureDao,
) {
    fun observeAll(): Flow<List<BloodPressureEntry>> = bloodPressureDao.observeAll()

    /**
     * Upserts at the deterministic id for (day, time of day) — see [BloodPressureEntry]. The second
     * measurement's values are null for a slot that was measured once; the mean the app goes by is
     * derived from them rather than stored, so a correction can never leave the two disagreeing.
     */
    suspend fun logEntry(
        epochDay: Long,
        timeOfDay: BloodPressureTimeOfDay,
        systolic: Double,
        diastolic: Double,
        pulse: Double? = null,
        systolic2: Double? = null,
        diastolic2: Double? = null,
        pulse2: Double? = null,
        comment: String?,
    ) {
        bloodPressureDao.upsert(
            BloodPressureEntry(
                id = "bloodpressure-$epochDay-$timeOfDay",
                epochDay = epochDay,
                timeOfDay = timeOfDay,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                systolic2 = systolic2,
                diastolic2 = diastolic2,
                pulse2 = pulse2,
                comment = comment?.trim()?.takeIf { it.isNotBlank() },
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun delete(entry: BloodPressureEntry) {
        bloodPressureDao.delete(entry)
    }
}
