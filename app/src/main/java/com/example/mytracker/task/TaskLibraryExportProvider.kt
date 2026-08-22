package com.example.mytracker.task

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
data class TaskDto(
    val id: String,
    val name: String,
    val recurrence: TaskRecurrence,
    val intervalCount: Int = 1,
    val weekdayMask: Int = 0,
    val dayOfMonth: Int = 1,
    val startEpochDay: Long,
    val archived: Boolean = false,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

private fun Task.toDto() = TaskDto(
    id = id,
    name = name,
    recurrence = recurrence,
    intervalCount = intervalCount,
    weekdayMask = weekdayMask,
    dayOfMonth = dayOfMonth,
    startEpochDay = startEpochDay,
    archived = archived,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun TaskDto.toEntity() = Task(
    id = id,
    name = name,
    recurrence = recurrence,
    intervalCount = intervalCount,
    weekdayMask = weekdayMask,
    dayOfMonth = dayOfMonth,
    startEpochDay = startEpochDay,
    archived = archived,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

/**
 * The Aufgaben *definitions* — their rhythm is the laborious part, and the archived ones come too so
 * that a restore doesn't quietly revive them by losing the flag. What was ticked off when is tracked
 * data and travels in [BackupScope.DAILY_ENTRIES], see [TaskCompletionExportProvider].
 */
class TaskLibraryExportProvider @Inject constructor(
    private val taskDao: TaskDao,
) : BackupExportProvider {
    override val key = "tasks"
    override val scope = BackupScope.LIBRARY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(taskDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<TaskDto>>(json)
        dtos.forEach { dto ->
            val existing = taskDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                taskDao.upsert(dto.toEntity())
            }
        }
    }

    /** The completions cascade with the Aufgaben. */
    override suspend fun clear() {
        taskDao.deleteAll()
    }
}
