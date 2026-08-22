package com.example.prokject2_tracker.fluid

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
data class FluidTypeDto(
    val id: String,
    val name: String,
    val defaultQuickAddMl: Double,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
    val dailyGoalMinMl: Double? = null,
    val dailyGoalMaxMl: Double? = null,
    val colorArgb: Int? = null,
)

private fun FluidType.toDto() = FluidTypeDto(
    id = id,
    name = name,
    defaultQuickAddMl = defaultQuickAddMl,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    dailyGoalMinMl = dailyGoalMinMl,
    dailyGoalMaxMl = dailyGoalMaxMl,
    colorArgb = colorArgb,
)

private fun FluidTypeDto.toEntity() = FluidType(
    id = id,
    name = name,
    defaultQuickAddMl = defaultQuickAddMl,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    dailyGoalMinMl = dailyGoalMinMl,
    dailyGoalMaxMl = dailyGoalMaxMl,
    colorArgb = colorArgb,
)

/** Exports/imports drink-type definitions only — logged fluid entries are tracked data and never included. */
class FluidTypeLibraryExportProvider @Inject constructor(
    private val fluidTypeDao: FluidTypeDao,
) : BackupExportProvider {
    override val key = "fluidTypes"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(fluidTypeDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<FluidTypeDto>>(json)
        dtos.forEach { dto ->
            val existing = fluidTypeDao.getById(dto.id)
            if (existing == null) {
                fluidTypeDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        fluidTypeDao.deleteAll()
    }
}
