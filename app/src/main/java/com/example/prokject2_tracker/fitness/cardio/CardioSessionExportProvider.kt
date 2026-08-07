package com.example.prokject2_tracker.fitness.cardio

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
data class CardioSessionDto(
    val id: String,
    val epochDay: Long,
    val createdAtEpochMillis: Long,
    val activityTypeId: String,
    val activityTypeName: String,
    val durationMinutes: Double,
    val distanceKm: Double? = null,
    val caloriesBurned: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val note: String? = null,
)

private fun CardioSession.toDto() = CardioSessionDto(
    id = id,
    epochDay = epochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    activityTypeId = activityTypeId,
    activityTypeName = activityTypeName,
    durationMinutes = durationMinutes,
    distanceKm = distanceKm,
    caloriesBurned = caloriesBurned,
    avgHeartRateBpm = avgHeartRateBpm,
    note = note,
)

private fun CardioSessionDto.toEntity() = CardioSession(
    id = id,
    epochDay = epochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    activityTypeId = activityTypeId,
    activityTypeName = activityTypeName,
    durationMinutes = durationMinutes,
    distanceKm = distanceKm,
    caloriesBurned = caloriesBurned,
    avgHeartRateBpm = avgHeartRateBpm,
    note = note,
)

/**
 * The Kardio-Einheiten, notes included. Like the Flüssigkeiten they carry the activity's name beside
 * its id and the table has no foreign keys, so a session restores readable even when the
 * Kardio-Arten stayed behind.
 */
class CardioSessionExportProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : BackupExportProvider {
    override val key = "cardioSessions"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(cardioDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<CardioSessionDto>>(json)
        dtos.forEach { dto ->
            if (cardioDao.getById(dto.id) == null) {
                cardioDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        cardioDao.deleteAll()
    }
}
