package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class StrengthLogRepository @Inject constructor(
    private val strengthLogDao: StrengthLogDao,
) {
    fun observeAll(): Flow<List<StrengthLogEntry>> = strengthLogDao.observeAll()

    suspend fun getById(id: String): StrengthLogEntry? = strengthLogDao.getById(id)

    fun observeDailySetsTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>> =
        strengthLogDao.observeDailySetsTotals(startInclusive, endInclusive)

    fun observeDailyVolumeTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>> =
        strengthLogDao.observeDailyVolumeTotals(startInclusive, endInclusive)

    suspend fun save(
        existing: StrengthLogEntry?,
        epochDay: Long,
        exerciseId: String,
        exerciseName: String,
        sets: Int,
        reps: Int,
        weightKg: Double,
        note: String?,
    ) {
        strengthLogDao.upsert(
            StrengthLogEntry(
                id = existing?.id ?: IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = existing?.createdAt ?: Instant.now(),
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                sets = sets,
                reps = reps,
                weightKg = weightKg,
                note = note,
            ),
        )
    }

    suspend fun delete(entry: StrengthLogEntry) {
        strengthLogDao.delete(entry)
    }
}
