package com.example.prokject2_tracker.sleep

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.prokject2_tracker.core.metrics.MetricPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_entries ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<SleepEntry>>

    @Query("SELECT * FROM sleep_entries WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<SleepEntry?>

    @Query("SELECT * FROM sleep_entries WHERE epochDay = :epochDay")
    suspend fun getForDay(epochDay: Long): SleepEntry?

    /** The night before [epochDay] — what the form prefills from when a new night is logged. */
    @Query("SELECT * FROM sleep_entries WHERE epochDay < :epochDay ORDER BY epochDay DESC LIMIT 1")
    suspend fun getMostRecentBefore(epochDay: Long): SleepEntry?

    /**
     * The last rating actually given before [epochDay] — where the slider starts. Nights without a
     * rating are skipped, so a gap in the log does not reset the slider.
     */
    @Query(
        "SELECT morningFitness FROM sleep_entries " +
            "WHERE epochDay < :epochDay AND morningFitness IS NOT NULL " +
            "ORDER BY epochDay DESC LIMIT 1",
    )
    suspend fun getMostRecentFitnessBefore(epochDay: Long): Int?

    /**
     * Sleep duration per night, counted forwards across midnight in SQL so the Analyse series needs
     * no post-processing. Mirrors `SleepEntry.durationMinutes` — keep the two in step.
     */
    @Query(
        "SELECT epochDay, ((endMinuteOfDay - startMinuteOfDay) % 1440 + 1440) % 1440 AS value " +
            "FROM sleep_entries WHERE epochDay BETWEEN :startInclusive AND :endInclusive ORDER BY epochDay",
    )
    fun observeDailyDurationMinutes(startInclusive: Long, endInclusive: Long): Flow<List<MetricPoint>>

    @Upsert
    suspend fun upsert(entry: SleepEntry)

    @Delete
    suspend fun delete(entry: SleepEntry)

    @Query("SELECT * FROM sleep_entry_tags")
    fun observeAllEntryTags(): Flow<List<SleepEntryTag>>

    @Query("SELECT tagId FROM sleep_entry_tags WHERE sleepEntryId = :sleepEntryId")
    suspend fun getTagIdsForEntry(sleepEntryId: String): List<String>

    @Query("DELETE FROM sleep_entry_tags WHERE sleepEntryId = :sleepEntryId")
    suspend fun deleteTagsForEntry(sleepEntryId: String)

    @Insert
    suspend fun insertEntryTags(rows: List<SleepEntryTag>)

    /** Wholesale-replaces a night's tags (delete-then-insert), like `TagDao.replaceFoodTags`. */
    @Transaction
    suspend fun replaceTagsForEntry(sleepEntryId: String, tagIds: List<String>) {
        deleteTagsForEntry(sleepEntryId)
        if (tagIds.isNotEmpty()) {
            insertEntryTags(tagIds.map { SleepEntryTag(sleepEntryId = sleepEntryId, tagId = it) })
        }
    }
}

@Dao
interface SleepTagDao {
    @Query("SELECT * FROM sleep_tags ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<SleepTag>>

    @Query("SELECT * FROM sleep_tags ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAllOnce(): List<SleepTag>

    @Query("SELECT * FROM sleep_tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): SleepTag?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM sleep_tags")
    suspend fun nextSortOrder(): Int

    @Upsert
    suspend fun upsert(tag: SleepTag)

    @Delete
    suspend fun delete(tag: SleepTag)

    /** How many nights carry this tag — what the delete confirmation says out loud. */
    @Query("SELECT COUNT(*) FROM sleep_entry_tags WHERE tagId = :tagId")
    suspend fun usageCount(tagId: String): Int
}
