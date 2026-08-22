package com.example.mytracker.task

import com.example.mytracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val taskCompletionDao: TaskCompletionDao,
) {
    fun observeActive(): Flow<List<Task>> = taskDao.observeActive()

    fun observeCompletions(): Flow<List<TaskCompletion>> = taskCompletionDao.observeAll()

    suspend fun createTask(
        name: String,
        recurrence: TaskRecurrence,
        startEpochDay: Long,
        intervalCount: Int = 1,
        weekdayMask: Int = 0,
        dayOfMonth: Int = 1,
    ) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val now = Instant.now()
        taskDao.upsert(
            Task(
                id = IdGenerator.newId(),
                name = trimmed,
                recurrence = recurrence,
                intervalCount = intervalCount.coerceAtLeast(1),
                weekdayMask = weekdayMask,
                dayOfMonth = dayOfMonth.coerceIn(1, 31),
                startEpochDay = startEpochDay,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /**
     * Rewrites the rule. The completions stay: they are keyed by due date, so the ones that still
     * fall on a due day of the new rhythm keep counting and the rest simply stop being asked about.
     */
    suspend fun updateTask(task: Task) {
        val trimmed = task.name.trim()
        if (trimmed.isBlank()) return
        taskDao.upsert(
            task.copy(
                name = trimmed,
                intervalCount = task.intervalCount.coerceAtLeast(1),
                dayOfMonth = task.dayOfMonth.coerceIn(1, 31),
                updatedAt = Instant.now(),
            ),
        )
    }

    suspend fun deleteTask(task: Task) {
        // The completions go with it via the CASCADE foreign key on task_completions.
        taskDao.delete(task)
    }

    /**
     * Ticks [dueEpochDay] off, or takes the tick back. [completedEpochDay] is when it was actually
     * done — normally today, and separate from the due day so working through a backlog still
     * counts as activity for the day it happened on.
     */
    suspend fun setCompleted(
        taskId: String,
        dueEpochDay: Long,
        completedEpochDay: Long,
        completed: Boolean,
    ) {
        if (completed) {
            // Deterministic id keyed on (taskId, dueEpochDay), so a repeated tick upserts in place
            // rather than violating the unique index.
            taskCompletionDao.upsert(
                TaskCompletion(
                    id = "$taskId-$dueEpochDay",
                    taskId = taskId,
                    dueEpochDay = dueEpochDay,
                    completedEpochDay = completedEpochDay,
                    createdAt = Instant.now(),
                ),
            )
        } else {
            taskCompletionDao.deleteForTaskAndDueDay(taskId, dueEpochDay)
        }
    }

    suspend fun getCompletionsForTask(taskId: String): List<TaskCompletion> =
        taskCompletionDao.getForTask(taskId)
}
