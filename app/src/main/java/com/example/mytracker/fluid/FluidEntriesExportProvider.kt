package com.example.mytracker.fluid

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
data class FluidEntryDto(
    val id: String,
    val epochDay: Long,
    val createdAtEpochMillis: Long,
    val fluidTypeId: String,
    val fluidTypeName: String,
    val amountMl: Double,
    val fluidUnitId: String? = null,
    val fluidUnitName: String? = null,
    val sourceDiaryEntryId: String? = null,
)

private fun FluidEntry.toDto() = FluidEntryDto(
    id = id,
    epochDay = epochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    fluidTypeId = fluidTypeId,
    fluidTypeName = fluidTypeName,
    amountMl = amountMl,
    fluidUnitId = fluidUnitId,
    fluidUnitName = fluidUnitName,
    sourceDiaryEntryId = sourceDiaryEntryId,
)

private fun FluidEntryDto.toEntity() = FluidEntry(
    id = id,
    epochDay = epochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    fluidTypeId = fluidTypeId,
    fluidTypeName = fluidTypeName,
    amountMl = amountMl,
    fluidUnitId = fluidUnitId,
    fluidUnitName = fluidUnitName,
    sourceDiaryEntryId = sourceDiaryEntryId,
)

/**
 * What was drunk, day by day. The rows carry the drink's name alongside its id and the table has no
 * foreign keys, so they restore intact even when the Getränkearten stayed behind.
 */
class FluidEntriesExportProvider @Inject constructor(
    private val fluidDao: FluidDao,
) : BackupExportProvider {
    override val key = "fluidEntries"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(fluidDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<FluidEntryDto>>(json)
        dtos.forEach { dto ->
            if (fluidDao.getById(dto.id) == null) {
                fluidDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        fluidDao.deleteAll()
    }
}
