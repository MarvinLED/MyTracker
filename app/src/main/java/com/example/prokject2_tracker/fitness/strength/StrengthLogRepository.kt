package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class StrengthLogRepository @Inject constructor(
    private val strengthLogDao: StrengthLogDao,
    private val strengthSetDao: StrengthSetDao,
) {
    fun observeAll(): Flow<List<StrengthLogEntry>> = strengthLogDao.observeAll()

    suspend fun getById(id: String): StrengthLogEntry? = strengthLogDao.getById(id)

    suspend fun getSetsForEntry(entryId: String): List<StrengthSet> = strengthSetDao.getForLogEntry(entryId)

    suspend fun getMostRecentSetForExercise(exerciseId: String): StrengthSet? =
        strengthSetDao.getMostRecentForExercise(exerciseId)

    fun observeAllSets(): Flow<List<StrengthSet>> = strengthSetDao.observeAll()

    fun observeDailySetsTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>> =
        strengthSetDao.observeDailySetsTotals(startInclusive, endInclusive)

    fun observeDailyVolumeTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>> =
        strengthSetDao.observeDailyVolumeTotals(startInclusive, endInclusive)

    suspend fun save(
        existingId: String?,
        epochDay: Long,
        exerciseId: String,
        exerciseName: String,
        muscleGroupId: String,
        note: String?,
        sets: List<Pair<Int, Double?>>,
    ): String {
        val id = existingId ?: IdGenerator.newId()
        val createdAt = existingId?.let { strengthLogDao.getById(it)?.createdAt } ?: Instant.now()
        strengthLogDao.upsert(
            StrengthLogEntry(
                id = id,
                epochDay = epochDay,
                createdAt = createdAt,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                note = note,
            ),
        )
        strengthSetDao.replaceSetsForLogEntry(
            id,
            sets.mapIndexed { index, (reps, weightKg) ->
                StrengthSet(
                    id = IdGenerator.newId(),
                    logEntryId = id,
                    epochDay = epochDay,
                    exerciseId = exerciseId,
                    muscleGroupId = muscleGroupId,
                    setIndex = index,
                    reps = reps,
                    weightKg = weightKg,
                )
            },
        )
        return id
    }

    suspend fun delete(entry: StrengthLogEntry) {
        strengthLogDao.delete(entry)
    }
}
