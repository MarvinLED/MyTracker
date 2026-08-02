package com.example.prokject2_tracker.sleep

import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SleepRepository @Inject constructor(
    private val sleepDao: SleepDao,
    private val sleepTagDao: SleepTagDao,
) {
    fun observeAll(): Flow<List<SleepEntry>> = sleepDao.observeAll()

    fun observeForDay(epochDay: Long): Flow<SleepEntry?> = sleepDao.observeForDay(epochDay)

    fun observeTags(): Flow<List<SleepTag>> = sleepTagDao.observeAll()

    /** Every night's tags at once, keyed by entry id — one query behind the whole history list. */
    fun observeTagIdsByEntryId(): Flow<Map<String, List<String>>> =
        sleepDao.observeAllEntryTags().map { rows -> rows.groupBy({ it.sleepEntryId }, { it.tagId }) }

    fun observeDailyDurationMinutes(startInclusive: Long, endInclusive: Long): Flow<List<MetricPoint>> =
        sleepDao.observeDailyDurationMinutes(startInclusive, endInclusive)

    suspend fun getForDay(epochDay: Long): SleepEntry? = sleepDao.getForDay(epochDay)

    suspend fun getMostRecentBefore(epochDay: Long): SleepEntry? = sleepDao.getMostRecentBefore(epochDay)

    suspend fun getTagIdsForEntry(sleepEntryId: String): List<String> = sleepDao.getTagIdsForEntry(sleepEntryId)

    /**
     * Writes the night that ended on [epochDay], at the deterministic id for that day — see
     * [SleepEntry]. Keeps [SleepEntry.createdAt] of an existing night, so correcting the times does
     * not make an old night look freshly logged.
     */
    suspend fun logNight(
        epochDay: Long,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        morningFitness: Int?,
        lastMealMinuteOfDay: Int?,
        tagIds: List<String>,
    ) {
        val id = "sleep-$epochDay"
        val createdAt = sleepDao.getForDay(epochDay)?.createdAt ?: Instant.now()
        sleepDao.upsert(
            SleepEntry(
                id = id,
                epochDay = epochDay,
                startMinuteOfDay = startMinuteOfDay,
                endMinuteOfDay = endMinuteOfDay,
                morningFitness = morningFitness?.coerceIn(MIN_MORNING_FITNESS, MAX_MORNING_FITNESS),
                lastMealMinuteOfDay = lastMealMinuteOfDay,
                createdAt = createdAt,
            ),
        )
        sleepDao.replaceTagsForEntry(id, tagIds)
    }

    suspend fun delete(entry: SleepEntry) {
        // The tag links go with it via the join table's CASCADE foreign key.
        sleepDao.delete(entry)
    }

    /**
     * Creates the tag, or returns the existing one when the name is already taken (case-insensitive)
     * — typing "Heiß" a second time attaches the tag that is already there rather than a twin of it.
     */
    suspend fun createTag(name: String): SleepTag? {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return null
        sleepTagDao.getByName(trimmed)?.let { return it }
        val tag = SleepTag(
            id = IdGenerator.newId(),
            name = trimmed,
            sortOrder = sleepTagDao.nextSortOrder(),
            createdAt = Instant.now(),
        )
        sleepTagDao.upsert(tag)
        return tag
    }

    suspend fun renameTag(tag: SleepTag, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        sleepTagDao.upsert(tag.copy(name = trimmed))
    }

    /** How many nights would lose this tag — the manage screen says so before deleting. */
    suspend fun tagUsageCount(tagId: String): Int = sleepTagDao.usageCount(tagId)

    suspend fun deleteTag(tag: SleepTag) {
        sleepTagDao.delete(tag)
    }
}
