package com.example.prokject2_tracker.bloodpressure

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
data class BloodPressureEntryDto(
    val id: String,
    val epochDay: Long,
    val timeOfDay: BloodPressureTimeOfDay,
    val systolic: Double,
    val diastolic: Double,
    val comment: String? = null,
    val createdAtEpochMillis: Long,
)

private fun BloodPressureEntry.toDto() = BloodPressureEntryDto(
    id = id,
    epochDay = epochDay,
    timeOfDay = timeOfDay,
    systolic = systolic,
    diastolic = diastolic,
    comment = comment,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun BloodPressureEntryDto.toEntity() = BloodPressureEntry(
    id = id,
    epochDay = epochDay,
    timeOfDay = timeOfDay,
    systolic = systolic,
    diastolic = diastolic,
    comment = comment,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * The Blutdruckwerte, comments included — those are often the only record of why a reading was
 * unusual. Matched on the (Tag, Tageszeit) slot, which is unique, rather than on the id.
 */
class BloodPressureExportProvider @Inject constructor(
    private val bloodPressureDao: BloodPressureDao,
) : BackupExportProvider {
    override val key = "bloodPressureEntries"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(bloodPressureDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<BloodPressureEntryDto>>(json)
        dtos.forEach { dto ->
            if (bloodPressureDao.getForDayAndTime(dto.epochDay, dto.timeOfDay) == null) {
                bloodPressureDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        bloodPressureDao.deleteAll()
    }
}
