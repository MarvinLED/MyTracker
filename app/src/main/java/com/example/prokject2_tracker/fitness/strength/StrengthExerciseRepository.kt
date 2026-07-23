package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class StrengthExerciseRepository @Inject constructor(
    private val strengthExerciseDao: StrengthExerciseDao,
) {
    fun observeAll(): Flow<List<StrengthExercise>> = strengthExerciseDao.observeAll()

    suspend fun getById(id: String): StrengthExercise? = strengthExerciseDao.getById(id)

    suspend fun create(name: String, muscleGroup: MuscleGroup) {
        val now = Instant.now()
        strengthExerciseDao.upsert(
            StrengthExercise(
                id = IdGenerator.newId(),
                name = name,
                muscleGroup = muscleGroup,
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
}
