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
data class SleepEntryDto(
    val id: String,
    val epochDay: Long,
    val startMinuteOfDay: Int? = null,
    val endMinuteOfDay: Int? = null,
    val morningFitness: Int? = null,
    val lastMealMinuteOfDay: Int? = null,
    val didNotSleep: Boolean = false,
    val createdAtEpochMillis: Long,
    /** The night's Tags, by id — dropped on import when the tag itself didn't come along. */
    val tagIds: List<String> = emptyList(),
)

@Serializable
data class NapEntryDto(
    val id: String,
    val epochDay: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val refreshmentFitness: Int? = null,
    val createdAtEpochMillis: Long,
)

@Serializable
data class SleepExportDto(
    val nights: List<SleepEntryDto> = emptyList(),
    val naps: List<NapEntryDto> = emptyList(),
)

private fun SleepEntry.toDto(tagIds: List<String>) = SleepEntryDto(
    id = id,
    epochDay = epochDay,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    morningFitness = morningFitness,
    lastMealMinuteOfDay = lastMealMinuteOfDay,
    didNotSleep = didNotSleep,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    tagIds = tagIds,
)

private fun SleepEntryDto.toEntity() = SleepEntry(
    id = id,
    epochDay = epochDay,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    morningFitness = morningFitness,
    lastMealMinuteOfDay = lastMealMinuteOfDay,
    didNotSleep = didNotSleep,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

private fun NapEntry.toDto() = NapEntryDto(
    id = id,
    epochDay = epochDay,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    refreshmentFitness = refreshmentFitness,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun NapEntryDto.toEntity() = NapEntry(
    id = id,
    epochDay = epochDay,
    startMinuteOfDay = startMinuteOfDay,
    endMinuteOfDay = endMinuteOfDay,
    refreshmentFitness = refreshmentFitness,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * The logged Nächte with their Tags, plus the Mittagsschlaf rows.
 *
 * `nap_entries` has no repository behind it yet — see the TODO in [SleepRepository] — so that list
 * is empty on every device today. It travels regardless, so that naps are not the one thing missing
 * from every backup written before the feature lands.
 *
 * Nights are matched on `epochDay`, which is unique: a night is the day it ended, and a device can
 * only have one record of it. The Schlaf-Tags are a library ([SleepTagLibraryExportProvider]) and
 * carry a real foreign key, so a link to a tag that stayed behind is dropped and the night still
 * lands with the rest of its tags.
 */
class SleepEntriesExportProvider @Inject constructor(
    private val sleepDao: SleepDao,
    private val sleepTagDao: SleepTagDao,
) : BackupExportProvider {
    override val key = "sleepEntries"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val tagIdsByEntry = sleepDao.getAllEntryTagsOnce()
            .groupBy({ it.sleepEntryId }, { it.tagId })
        return json.encodeToJsonElement(
            SleepExportDto(
                nights = sleepDao.getAllOnce().map { it.toDto(tagIdsByEntry[it.id].orEmpty()) },
                naps = sleepDao.getAllNapsOnce().map { it.toDto() },
            ),
        )
    }

    override suspend fun import(json: JsonElement) {
        val dto = this.json.decodeFromJsonElement<SleepExportDto>(json)
        val knownTagIds = sleepTagDao.getAllOnce().map { it.id }.toSet()

        dto.nights.forEach { night ->
            if (sleepDao.getForDay(night.epochDay) != null) return@forEach
            sleepDao.upsert(night.toEntity())
            sleepDao.replaceTagsForEntry(night.id, night.tagIds.filter { it in knownTagIds })
        }
        dto.naps.forEach { nap ->
            if (sleepDao.getNapById(nap.id) == null) {
                sleepDao.upsertNap(nap.toEntity())
            }
        }
    }

    override suspend fun clear() {
        sleepDao.deleteAll()
        sleepDao.deleteAllNaps()
    }
}
