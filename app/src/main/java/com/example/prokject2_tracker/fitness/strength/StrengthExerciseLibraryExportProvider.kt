package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class StrengthExerciseDto(
    val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val muscleGroupIds: List<String> = emptyList(),
)

private fun StrengthExercise.toDto(muscleGroupIds: List<String>) = StrengthExerciseDto(
    id = id,
    name = name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    muscleGroupIds = muscleGroupIds,
)

private fun StrengthExerciseDto.toEntity() = StrengthExercise(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

/** Imported after `"muscleGroups"` (see [importPriority]) since [StrengthExerciseDto.muscleGroupIds] are foreign keys into that data. */
class StrengthExerciseLibraryExportProvider @Inject constructor(
    private val strengthExerciseDao: StrengthExerciseDao,
) : LibraryExportProvider {
    override val key = "strengthExercises"
    override val importPriority = 5

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val dtos = strengthExerciseDao.getAllOnce().map { exercise ->
            val muscleGroupIds = strengthExerciseDao.getMuscleGroupCrossRefsForExercise(exercise.id).map { it.muscleGroupId }
            exercise.toDto(muscleGroupIds)
        }
        return json.encodeToJsonElement(dtos)
    }

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<StrengthExerciseDto>>(json)
        dtos.forEach { dto ->
            val existing = strengthExerciseDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                strengthExerciseDao.upsert(dto.toEntity())
                strengthExerciseDao.replaceMuscleGroupsForExercise(dto.id, dto.muscleGroupIds)
            }
        }
    }
}
