package com.example.mytracker.weight

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class BodyWeightRepository @Inject constructor(
    private val bodyWeightDao: BodyWeightDao,
) {
    fun observeAll(): Flow<List<BodyWeightEntry>> = bodyWeightDao.observeAll()

    fun observeForDay(epochDay: Long): Flow<BodyWeightEntry?> = bodyWeightDao.observeForDay(epochDay)

    fun observeRange(startInclusive: Long, endInclusive: Long): Flow<List<BodyWeightEntry>> =
        bodyWeightDao.observeRange(startInclusive, endInclusive)

    /** Upserts at the deterministic id for [epochDay], so logging the same day twice overwrites in place. */
    suspend fun logWeight(epochDay: Long, weightKg: Double) {
        bodyWeightDao.upsert(
            BodyWeightEntry(
                id = "weight-$epochDay",
                epochDay = epochDay,
                weightKg = weightKg,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun delete(entry: BodyWeightEntry) {
        bodyWeightDao.delete(entry)
    }
}
