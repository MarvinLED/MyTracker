package com.example.mytracker.fitness.strength

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupScope
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
    /** Enum name, or null for untagged exercises — also the default for backups written before the tag existed. */
    val movementDirection: String? = null,
    /** False for backups written before bodyweight exercises existed — which is what they all were. */
    val isBodyweight: Boolean = false,
)

private fun StrengthExercise.toDto(muscleGroupIds: List<String>) = StrengthExerciseDto(
    id = id,
    name = name,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    muscleGroupIds = muscleGroupIds,
    movementDirection = movementDirection?.name,
    isBodyweight = isBodyweight,
)

private fun StrengthExerciseDto.toEntity() = StrengthExercise(
    id = id,
    name = name,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    // An unknown value (backup from a newer build) degrades to "untagged" rather than failing the import.
    movementDirection = movementDirection?.let { name ->
        MovementDirection.entries.firstOrNull { it.name == name }
    },
    isBodyweight = isBodyweight,
)

/** Imported after `"muscleGroups"` (see [importPriority]) since [StrengthExerciseDto.muscleGroupIds] are foreign keys into that data. */
class StrengthExerciseLibraryExportProvider @Inject constructor(
    private val strengthExerciseDao: StrengthExerciseDao,
) : BackupExportProvider {
    override val key = "strengthExercises"
    override val scope = BackupScope.LIBRARY
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

    /** The muscle-group cross-refs cascade with the Übungen. */
    override suspend fun clear() {
        strengthExerciseDao.deleteAll()
    }
}
