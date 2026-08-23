package com.example.mytracker.bloodpressure

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupScope
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Defaults on every new field, so a backup written before the pulse and the second measurement
 * existed still reads back as what it was: one measurement, no pulse.
 */
@Serializable
data class BloodPressureEntryDto(
    val id: String,
    val epochDay: Long,
    val timeOfDay: BloodPressureTimeOfDay,
    val systolic: Double,
    val diastolic: Double,
    val pulse: Double? = null,
    val systolic2: Double? = null,
    val diastolic2: Double? = null,
    val pulse2: Double? = null,
    val comment: String? = null,
    val createdAtEpochMillis: Long,
)

private fun BloodPressureEntry.toDto() = BloodPressureEntryDto(
    id = id,
    epochDay = epochDay,
    timeOfDay = timeOfDay,
    // The raw measurements travel, not the mean: the mean is derived on the way back out, and a
    // backup that carried it would be the one place the two could drift apart.
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    systolic2 = systolic2,
    diastolic2 = diastolic2,
    pulse2 = pulse2,
    comment = comment,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun BloodPressureEntryDto.toEntity() = BloodPressureEntry(
    id = id,
    epochDay = epochDay,
    timeOfDay = timeOfDay,
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse,
    systolic2 = systolic2,
    diastolic2 = diastolic2,
    pulse2 = pulse2,
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
