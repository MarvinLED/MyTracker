package com.example.mytracker.measurement

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
data class BodyMeasurementDto(
    val id: String,
    val bodySiteId: String,
    val epochDay: Long,
    val valueCm: Double,
    val createdAtEpochMillis: Long,
)

private fun BodyMeasurement.toDto() = BodyMeasurementDto(
    id = id,
    bodySiteId = bodySiteId,
    epochDay = epochDay,
    valueCm = valueCm,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun BodyMeasurementDto.toEntity() = BodyMeasurement(
    id = id,
    bodySiteId = bodySiteId,
    epochDay = epochDay,
    valueCm = valueCm,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * The measured Maße. `bodySiteId` is a real foreign key, so a measurement whose Körperstelle didn't
 * come along is skipped rather than allowed to fail the import — the same rule the Schnellauswahl
 * already follows for its drink type.
 *
 * Matched on (Körperstelle, Tag), which is unique, rather than on the id.
 */
class BodyMeasurementExportProvider @Inject constructor(
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val bodySiteDao: BodySiteDao,
) : BackupExportProvider {
    override val key = "bodyMeasurements"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(bodyMeasurementDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<BodyMeasurementDto>>(json)
        dtos.forEach { dto ->
            if (bodySiteDao.getById(dto.bodySiteId) == null) return@forEach
            if (bodyMeasurementDao.getForSiteAndDay(dto.bodySiteId, dto.epochDay) == null) {
                bodyMeasurementDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        bodyMeasurementDao.deleteAll()
    }
}
