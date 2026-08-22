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
data class BodySiteDto(
    val id: String,
    val name: String,
    val measuringHint: String? = null,
    val sortOrder: Int,
    val createdAtEpochMillis: Long,
)

private fun BodySite.toDto() = BodySiteDto(
    id = id,
    name = name,
    measuringHint = measuringHint,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun BodySiteDto.toEntity() = BodySite(
    id = id,
    name = name,
    measuringHint = measuringHint,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * Exports/imports the Körperstellen definitions — including their measuring hints, which are the
 * part that is actually laborious to re-type. The measurements themselves are tracked data and
 * travel in [BackupScope.DAILY_ENTRIES] instead, see [BodyMeasurementExportProvider].
 */
class BodySiteLibraryExportProvider @Inject constructor(
    private val bodySiteDao: BodySiteDao,
) : BackupExportProvider {
    override val key = "bodySites"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(bodySiteDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<BodySiteDto>>(json)
        dtos.forEach { dto ->
            if (bodySiteDao.getById(dto.id) == null) {
                bodySiteDao.upsert(dto.toEntity())
            }
        }
    }

    /** The measurements cascade with the Körperstellen. */
    override suspend fun clear() {
        bodySiteDao.deleteAll()
    }
}
