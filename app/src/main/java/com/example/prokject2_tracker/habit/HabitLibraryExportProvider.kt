package com.example.prokject2_tracker.habit

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class HabitDto(
    val id: String,
    val name: String,
    val archived: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

private fun Habit.toDto() = HabitDto(
    id = id,
    name = name,
    archived = archived,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun HabitDto.toEntity() = Habit(
    id = id,
    name = name,
    archived = archived,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

/** Exports/imports habit *definitions* only — check-ins are tracked data and never included. */
class HabitLibraryExportProvider @Inject constructor(
    private val habitDao: HabitDao,
) : LibraryExportProvider {
    override val key = "habits"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(habitDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<HabitDto>>(json)
        dtos.forEach { dto ->
            val existing = habitDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                habitDao.upsert(dto.toEntity())
            }
        }
    }
}
