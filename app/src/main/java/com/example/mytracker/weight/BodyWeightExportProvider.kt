package com.example.prokject2_tracker.weight

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
data class BodyWeightEntryDto(
    val id: String,
    val epochDay: Long,
    val weightKg: Double,
    val createdAtEpochMillis: Long,
)

private fun BodyWeightEntry.toDto() = BodyWeightEntryDto(
    id = id,
    epochDay = epochDay,
    weightKg = weightKg,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun BodyWeightEntryDto.toEntity() = BodyWeightEntry(
    id = id,
    epochDay = epochDay,
    weightKg = weightKg,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * The Gewichtsverlauf. Always in kg, whatever unit the app is showing — the entity stores kg and the
 * chosen unit is a display setting that travels separately.
 *
 * Matched on the day rather than on the id: `epochDay` is unique, so a second row for a day the
 * device already has would break on the index however the row is named.
 */
class BodyWeightExportProvider @Inject constructor(
    private val bodyWeightDao: BodyWeightDao,
) : BackupExportProvider {
    override val key = "bodyWeightEntries"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(bodyWeightDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<BodyWeightEntryDto>>(json)
        dtos.forEach { dto ->
            if (bodyWeightDao.getForDayOnce(dto.epochDay) == null) {
                bodyWeightDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        bodyWeightDao.deleteAll()
    }
}
