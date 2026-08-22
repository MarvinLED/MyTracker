package com.example.mytracker.habit

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
data class HabitCheckInDto(
    val id: String,
    val habitId: String,
    val epochDay: Long,
    val createdAtEpochMillis: Long,
    val value: Double? = null,
)

private fun HabitCheckIn.toDto() = HabitCheckInDto(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    value = value,
)

private fun HabitCheckInDto.toEntity() = HabitCheckIn(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    value = value,
)

/**
 * The abgehakten Habits — the streak itself, and the part that cannot be reconstructed from memory.
 * The Habits they belong to are a library ([HabitLibraryExportProvider]).
 *
 * `habit_check_ins` has no foreign key, but a check-in for a habit that isn't there would show up
 * nowhere and quietly pad the Analyse counts, so it is skipped. Matched on the (Habit, Tag) slot,
 * which is unique, rather than on the id.
 */
class HabitCheckInExportProvider @Inject constructor(
    private val habitCheckInDao: HabitCheckInDao,
    private val habitDao: HabitDao,
) : BackupExportProvider {
    override val key = "habitCheckIns"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(habitCheckInDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<HabitCheckInDto>>(json)
        dtos.forEach { dto ->
            if (habitDao.getById(dto.habitId) == null) return@forEach
            if (habitCheckInDao.getForHabitAndDay(dto.habitId, dto.epochDay) == null) {
                habitCheckInDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        habitCheckInDao.deleteAll()
    }
}
