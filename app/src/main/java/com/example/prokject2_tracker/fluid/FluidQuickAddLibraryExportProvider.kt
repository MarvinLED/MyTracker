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
data class FluidQuickAddDto(
    val id: String,
    val fluidTypeId: String,
    val symbol: String,
    val amountMl: Double,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

private fun FluidQuickAdd.toDto() = FluidQuickAddDto(
    id = id,
    fluidTypeId = fluidTypeId,
    symbol = symbol.name,
    amountMl = amountMl,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun FluidQuickAddDto.toEntity(symbol: FluidQuickAddSymbol) = FluidQuickAdd(
    id = id,
    fluidTypeId = fluidTypeId,
    symbol = symbol,
    amountMl = amountMl,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * Exports/imports the Tagebuch's Schnellauswahl — which drink is offered under which symbol. Logged
 * drinks are tracked data and stay out, the same line the other fluid providers draw.
 *
 * Imported after `"fluidTypes"` (see [importPriority]) since [FluidQuickAddDto.fluidTypeId] is a
 * foreign key into that data.
 */
class FluidQuickAddLibraryExportProvider @Inject constructor(
    private val fluidQuickAddDao: FluidQuickAddDao,
    private val fluidTypeDao: FluidTypeDao,
) : BackupExportProvider {
    override val key = "fluidQuickAdds"
    override val scope = BackupScope.LIBRARY

    override val importPriority = 5

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(fluidQuickAddDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<FluidQuickAddDto>>(json)
        var count = fluidQuickAddDao.getAllOnce().size
        dtos.forEach { dto ->
            // Never past the cap the Tagebuch draws: an import that merges into an already
            // configured device would otherwise leave rows that no screen ever shows.
            if (count >= FluidQuickAddLimit) return@forEach
            if (fluidQuickAddDao.getById(dto.id) != null) return@forEach
            // A button whose drink type did not come along has nothing to log; the export it came
            // from is free to be a partial one.
            if (fluidTypeDao.getById(dto.fluidTypeId) == null) return@forEach
            val symbol = FluidQuickAddSymbol.entries.firstOrNull { it.name == dto.symbol } ?: return@forEach
            fluidQuickAddDao.upsert(dto.toEntity(symbol))
            count++
        }
    }

    override suspend fun clear() {
        fluidQuickAddDao.deleteAll()
    }
}
