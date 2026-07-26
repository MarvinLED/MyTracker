package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class StrengthExerciseWithMuscleGroups(
    val exercise: StrengthExercise,
    val muscleGroups: List<MuscleGroup>,
)

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

    /** Every exercise's currently attached muscle groups, keyed by exerciseId — for the Bibliothek list. */
    fun observeMuscleGroupsByExerciseId(): Flow<Map<String, List<MuscleGroup>>> =
        combine(muscleGroupDao.observeAll(), strengthExerciseDao.observeAllExerciseMuscleGroups()) { groups, crossRefs ->
            val groupById = groups.associateBy { it.id }
            crossRefs
                .groupBy({ it.exerciseId }) { groupById[it.muscleGroupId] }
                .mapValues { (_, groupList) -> groupList.filterNotNull() }
        }

    fun observeAllWithMuscleGroups(): Flow<List<StrengthExerciseWithMuscleGroups>> =
        combine(observeAll(), observeMuscleGroupsByExerciseId()) { exercises, groupsByExerciseId ->
            exercises.map { exercise ->
                StrengthExerciseWithMuscleGroups(exercise, groupsByExerciseId[exercise.id].orEmpty())
            }
        }

    suspend fun getMuscleGroupsForExerciseOnce(exerciseId: String): List<MuscleGroup> {
        val ids = strengthExerciseDao.getMuscleGroupCrossRefsForExercise(exerciseId).map { it.muscleGroupId }.toSet()
        return muscleGroupDao.getAllOnce().filter { it.id in ids }
    }

    suspend fun create(name: String, muscleGroupIds: List<String>, movementDirection: MovementDirection?) {
        val now = Instant.now()
        val id = IdGenerator.newId()
        strengthExerciseDao.upsert(
            StrengthExercise(
                id = id,
                name = name,
                createdAt = now,
                updatedAt = now,
                movementDirection = movementDirection,
            ),
        )
        strengthExerciseDao.replaceMuscleGroupsForExercise(id, muscleGroupIds)
    }

    suspend fun update(
        existing: StrengthExercise,
        name: String,
        muscleGroupIds: List<String>,
        movementDirection: MovementDirection?,
    ) {
        strengthExerciseDao.upsert(
            existing.copy(name = name, updatedAt = Instant.now(), movementDirection = movementDirection),
        )
        strengthExerciseDao.replaceMuscleGroupsForExercise(existing.id, muscleGroupIds)
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
