package com.example.prokject2_tracker.sleep

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
data class SleepTagDto(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long,
)

private fun SleepTag.toDto() = SleepTagDto(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun SleepTagDto.toEntity() = SleepTag(
    id = id,
    name = name,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * The Schlaf-Tags library. Nights are tracked data and stay out of the backup, like every other
 * log — this exports only the labels they are attached to.
 */
class SleepTagLibraryExportProvider @Inject constructor(
    private val sleepTagDao: SleepTagDao,
) : BackupExportProvider {
    override val key = "sleepTags"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(sleepTagDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<SleepTagDto>>(json)
        dtos.forEach { dto ->
            // Name-matched rather than id-matched as well: two devices that both created "heiß"
            // locally would otherwise end up with a duplicate that no screen can tell apart.
            val existing = sleepTagDao.getByName(dto.name)
            if (existing == null) {
                sleepTagDao.upsert(dto.toEntity())
            }
        }
    }

    /** The per-night links cascade with the Tags. */
    override suspend fun clear() {
        sleepTagDao.deleteAll()
    }
}
