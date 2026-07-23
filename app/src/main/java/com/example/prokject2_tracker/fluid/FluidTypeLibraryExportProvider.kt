package com.example.prokject2_tracker.fluid

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
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
)

private fun FluidType.toDto() = FluidTypeDto(
    id = id,
    name = name,
    defaultQuickAddMl = defaultQuickAddMl,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun FluidTypeDto.toEntity() = FluidType(
    id = id,
    name = name,
    defaultQuickAddMl = defaultQuickAddMl,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/** Exports/imports drink-type definitions only — logged fluid entries are tracked data and never included. */
class FluidTypeLibraryExportProvider @Inject constructor(
    private val fluidTypeDao: FluidTypeDao,
) : LibraryExportProvider {
    override val key = "fluidTypes"

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
}
