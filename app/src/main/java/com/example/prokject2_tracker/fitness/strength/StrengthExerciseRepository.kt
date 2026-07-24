package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Seeded once on first run; the user can rename/add/remove freely afterwards. */
private val DEFAULT_MUSCLE_GROUPS = listOf(
    "musclegroup-brust" to "Brust",
    "musclegroup-ruecken" to "Rücken",
    "musclegroup-beine" to "Beine",
    "musclegroup-schultern" to "Schultern",
    "musclegroup-arme" to "Arme",
    "musclegroup-rumpf" to "Rumpf",
    "musclegroup-ganzkoerper" to "Ganzkörper",
    "musclegroup-sonstiges" to "Sonstiges",
)

@Singleton
class StrengthExerciseRepository @Inject constructor(
    private val strengthExerciseDao: StrengthExerciseDao,
    private val muscleGroupDao: MuscleGroupDao,
) {
    fun observeAll(): Flow<List<StrengthExercise>> = strengthExerciseDao.observeAll()

    suspend fun getById(id: String): StrengthExercise? = strengthExerciseDao.getById(id)

    suspend fun create(name: String, muscleGroupId: String, muscleGroupName: String) {
        val now = Instant.now()
        strengthExerciseDao.upsert(
            StrengthExercise(
                id = IdGenerator.newId(),
                name = name,
                muscleGroupId = muscleGroupId,
                muscleGroupName = muscleGroupName,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(existing: StrengthExercise, updated: StrengthExercise) {
        strengthExerciseDao.upsert(updated.copy(createdAt = existing.createdAt, updatedAt = Instant.now()))
    }

    suspend fun canDelete(exerciseId: String): Boolean = !strengthExerciseDao.isUsedInAnyLogEntry(exerciseId)

    suspend fun delete(exercise: StrengthExercise) {
        strengthExerciseDao.delete(exercise)
    }

    fun observeMuscleGroups(): Flow<List<MuscleGroup>> = muscleGroupDao.observeAll()

    suspend fun ensureDefaultMuscleGroupsSeeded() {
        if (muscleGroupDao.getAllOnce().isNotEmpty()) return
        val now = Instant.now()
        muscleGroupDao.upsertAll(
            DEFAULT_MUSCLE_GROUPS.mapIndexed { index, (id, name) ->
                MuscleGroup(
                    id = id,
                    name = name,
                    sortOrder = index,
                    createdAt = now,
                )
            },
        )
    }

    suspend fun createMuscleGroup(name: String) {
        val maxSortOrder = muscleGroupDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1
        muscleGroupDao.upsert(
            MuscleGroup(
                id = IdGenerator.newId(),
                name = name,
                sortOrder = maxSortOrder + 1,
                createdAt = Instant.now(),
            ),
        )
    }

    suspend fun updateMuscleGroup(existing: MuscleGroup, name: String) {
        muscleGroupDao.upsert(existing.copy(name = name))
    }

    suspend fun canDeleteMuscleGroup(id: String): Boolean = !muscleGroupDao.isUsedInAnyEntry(id)

    suspend fun deleteMuscleGroup(group: MuscleGroup) {
        muscleGroupDao.delete(group)
    }
}
