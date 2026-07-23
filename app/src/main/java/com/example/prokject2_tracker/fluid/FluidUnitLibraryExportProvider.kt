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
data class FluidUnitDto(
    val id: String,
    val name: String,
    val amountMl: Double,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

private fun FluidUnit.toDto() = FluidUnitDto(
    id = id,
    name = name,
    amountMl = amountMl,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun FluidUnitDto.toEntity() = FluidUnit(
    id = id,
    name = name,
    amountMl = amountMl,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/** Exports/imports Maßeinheiten definitions only — logged fluid entries are tracked data and never included. */
class FluidUnitLibraryExportProvider @Inject constructor(
    private val fluidUnitDao: FluidUnitDao,
) : LibraryExportProvider {
    override val key = "fluidUnits"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(fluidUnitDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<FluidUnitDto>>(json)
        dtos.forEach { dto ->
            val existing = fluidUnitDao.getById(dto.id)
            if (existing == null) {
                fluidUnitDao.upsert(dto.toEntity())
            }
        }
    }
}
