package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import com.example.prokject2_tracker.core.backup.BackupScope
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class MuscleGroupDto(
    val id: String,
    val name: String,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

private fun MuscleGroup.toDto() = MuscleGroupDto(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun MuscleGroupDto.toEntity() = MuscleGroup(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/** Exports/imports muscle-group definitions only — logged exercises are tracked data and never included. */
class MuscleGroupLibraryExportProvider @Inject constructor(
    private val muscleGroupDao: MuscleGroupDao,
) : BackupExportProvider {
    override val key = "muscleGroups"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(muscleGroupDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<MuscleGroupDto>>(json)
        dtos.forEach { dto ->
            val existing = muscleGroupDao.getById(dto.id)
            if (existing == null) {
                muscleGroupDao.upsert(dto.toEntity())
            }
        }
    }

    /** The exercise cross-refs cascade with the Muskelgruppen. */
    override suspend fun clear() {
        muscleGroupDao.deleteAll()
    }
}
