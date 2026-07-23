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
    val muscleGroup: MuscleGroup,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

private fun StrengthExercise.toDto() = StrengthExerciseDto(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun StrengthExerciseDto.toEntity() = StrengthExercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

class StrengthExerciseLibraryExportProvider @Inject constructor(
    private val strengthExerciseDao: StrengthExerciseDao,
) : LibraryExportProvider {
    override val key = "strengthExercises"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(strengthExerciseDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<StrengthExerciseDto>>(json)
        dtos.forEach { dto ->
            val existing = strengthExerciseDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                strengthExerciseDao.upsert(dto.toEntity())
            }
        }
    }
}
