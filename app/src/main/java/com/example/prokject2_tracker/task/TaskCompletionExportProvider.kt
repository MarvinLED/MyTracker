package com.example.prokject2_tracker.task

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
data class TaskCompletionDto(
    val id: String,
    val taskId: String,
    val dueEpochDay: Long,
    val completedEpochDay: Long,
    val createdAtEpochMillis: Long,
)

private fun TaskCompletion.toDto() = TaskCompletionDto(
    id = id,
    taskId = taskId,
    dueEpochDay = dueEpochDay,
    completedEpochDay = completedEpochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
)

private fun TaskCompletionDto.toEntity() = TaskCompletion(
    id = id,
    taskId = taskId,
    dueEpochDay = dueEpochDay,
    completedEpochDay = completedEpochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
)

/**
 * When each Aufgabe was ticked off. `taskId` is a real foreign key, so a completion whose Aufgabe
 * didn't come along is skipped rather than allowed to fail the import. Matched on the (Aufgabe,
 * Fälligkeitstag) slot, which is unique, rather than on the id.
 */
class TaskCompletionExportProvider @Inject constructor(
    private val taskCompletionDao: TaskCompletionDao,
    private val taskDao: TaskDao,
) : BackupExportProvider {
    override val key = "taskCompletions"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(taskCompletionDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<TaskCompletionDto>>(json)
        dtos.forEach { dto ->
            if (taskDao.getById(dto.taskId) == null) return@forEach
            if (taskCompletionDao.getForTaskAndDueDay(dto.taskId, dto.dueEpochDay) == null) {
                taskCompletionDao.upsert(dto.toEntity())
            }
        }
    }

    override suspend fun clear() {
        taskCompletionDao.deleteAll()
    }
}
