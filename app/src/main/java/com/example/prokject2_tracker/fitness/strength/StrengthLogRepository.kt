package com.example.prokject2_tracker.fitness.strength

import androidx.room.withTransaction
import com.example.prokject2_tracker.core.database.AppDatabase
import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StrengthLogRepository @Inject constructor(
    private val strengthLogDao: StrengthLogDao,
    private val strengthSetDao: StrengthSetDao,
    private val database: AppDatabase,
) {
    fun observeAll(): Flow<List<StrengthLogEntry>> = strengthLogDao.observeAll()

    fun observeSetsForExercise(exerciseId: String): Flow<List<StrengthSet>> =
        strengthSetDao.observeAllForExercise(exerciseId)

    fun observeEntriesForExercise(exerciseId: String): Flow<List<StrengthLogEntry>> =
        strengthLogDao.observeForExercise(exerciseId)

    fun observeLastTrainedDayPerExercise(): Flow<Map<String, Long>> =
        strengthSetDao.observeLastTrainedDayPerExercise()
            .map { rows -> rows.associate { it.exerciseId to it.epochDay } }

    suspend fun getById(id: String): StrengthLogEntry? = strengthLogDao.getById(id)

    suspend fun getSetsForEntry(entryId: String): List<StrengthSet> = strengthSetDao.getForLogEntry(entryId)

    suspend fun getMostRecentSetForExercise(exerciseId: String): StrengthSet? =
        strengthSetDao.getMostRecentForExercise(exerciseId)

    fun observeAllSets(): Flow<List<StrengthSet>> = strengthSetDao.observeAll()

    fun observeDailySetsTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>> =
        strengthSetDao.observeDailySetsTotals(startInclusive, endInclusive)

    fun observeDailyVolumeTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>> =
        strengthSetDao.observeDailyVolumeTotals(startInclusive, endInclusive)

    fun observeDailyVolumeTotalsForExercise(exerciseId: String, startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>> =
        strengthSetDao.observeDailyVolumeTotalsForExercise(exerciseId, startInclusive, endInclusive)

    fun observeDailySetsTotalsForExercise(exerciseId: String, startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>> =
        strengthSetDao.observeDailySetsTotalsForExercise(exerciseId, startInclusive, endInclusive)

    fun observeDailyVolumeTotalsForMuscleGroup(muscleGroupId: String, startInclusive: Long, endInclusive: Long): Flow<List<DailyVolumeTotal>> =
        strengthSetDao.observeDailyVolumeTotalsForMuscleGroup(muscleGroupId, startInclusive, endInclusive)

    fun observeDailySetsTotalsForMuscleGroup(muscleGroupId: String, startInclusive: Long, endInclusive: Long): Flow<List<DailySetsTotal>> =
        strengthSetDao.observeDailySetsTotalsForMuscleGroup(muscleGroupId, startInclusive, endInclusive)

    fun observeDailyVolumeTotalsForMovementDirection(
        direction: MovementDirection,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailyVolumeTotal>> =
        strengthSetDao.observeDailyVolumeTotalsForMovementDirection(direction.name, startInclusive, endInclusive)

    fun observeDailySetsTotalsForMovementDirection(
        direction: MovementDirection,
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<DailySetsTotal>> =
        strengthSetDao.observeDailySetsTotalsForMovementDirection(direction.name, startInclusive, endInclusive)

    suspend fun save(
        existingId: String?,
        epochDay: Long,
        exerciseId: String,
        exerciseName: String,
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

    /**
     * Writes the whole set list for one (exercise, day) session. Returns the surviving entry id, or
     * null when [sets] is empty — an empty session is deleted rather than left as an entry with no
     * sets, which would still count towards "days since last strength training" and every sets goal.
     * It never *creates* an entry for an empty list either, so merely opening the detail page or
     * paging through dates writes nothing.
     *
     * Also collapses the several-entries-per-day case the previous entry form could produce: the
     * oldest entry wins and keeps its [StrengthLogEntry.createdAt], and the discarded duplicates'
     * notes are appended rather than silently dropped.
     */
    suspend fun saveSession(
        exerciseId: String,
        exerciseName: String,
        epochDay: Long,
        note: String?,
        sets: List<Pair<Int, Double?>>,
    ): String? = database.withTransaction {
        val existing = strengthLogDao.getForExerciseOnDay(exerciseId, epochDay)
        if (sets.isEmpty()) {
            existing.forEach { strengthLogDao.delete(it) }
            return@withTransaction null
        }
        val survivor = existing.firstOrNull()
        val duplicates = existing.drop(1)
        val mergedNote = (listOfNotNull(note?.takeIf { it.isNotBlank() }) + duplicates.mapNotNull { it.note })
            .distinct()
            .joinToString(" · ")
            .takeIf { it.isNotBlank() }
        duplicates.forEach { strengthLogDao.delete(it) }
        save(survivor?.id, epochDay, exerciseId, exerciseName, mergedNote, sets)
    }
}
