package com.example.mytracker.task

/**
 * Where a task stands on a given day. Computed in one place because two screens ask the same
 * question — the Aufgaben list and the Tagesziele — and "fällig" has to mean the same on both.
 */
data class TaskStatus(
    val task: Task,
    /** The day this was worked out for; everything below is relative to it. */
    val today: Long,
    /** The oldest due day still owed, at or before [today]. Null when the task is caught up. */
    val openDueDay: Long?,
    /** When it next comes round, for a task with nothing owed. Null when it never will again. */
    val nextDueDay: Long?,
    /** True when a due day was ticked off *today*, whichever occurrence it settled. */
    val completedToday: Boolean,
) {
    val isOpen: Boolean get() = openDueDay != null

    /** How many days late the open occurrence is — 0 when it is today's, null when nothing is owed. */
    val overdueDays: Long? get() = openDueDay?.let { due -> (today - due).coerceAtLeast(0) }
}

/**
 * Every active task against [today]. A task counts as done only when the tick happened today —
 * yesterday's finished chore is not part of today's picture, but one ticked off this morning stays
 * visible so the Tagesziele do not lose a row the moment it is completed.
 */
fun taskStatuses(
    tasks: List<Task>,
    completions: List<TaskCompletion>,
    today: Long,
): List<TaskStatus> {
    val doneDaysByTask = completions.groupBy({ it.taskId }, { it.dueEpochDay })
        .mapValues { (_, days) -> days.toSet() }
    val completedTodayTaskIds = completions.filter { it.completedEpochDay == today }.map { it.taskId }.toSet()

    return tasks.map { task ->
        val done = doneDaysByTask[task.id].orEmpty()
        val openDueDay = task.oldestOpenOccurrence(today) { it in done }
        TaskStatus(
            task = task,
            today = today,
            openDueDay = openDueDay,
            // Only worth working out when nothing is owed — otherwise the open one is the answer.
            nextDueDay = if (openDueDay == null) task.nextDueOnOrAfter(today + 1) else null,
            completedToday = task.id in completedTodayTaskIds,
        )
    }
}

/**
 * What today's list is: everything owed, plus what was ticked off today so it can be seen (and
 * undone). Oldest debt first — the rest sorts by name to stay put between recompositions.
 */
fun List<TaskStatus>.dueToday(): List<TaskStatus> =
    filter { it.isOpen || it.completedToday }
        .sortedWith(compareBy({ it.openDueDay ?: Long.MAX_VALUE }, { it.task.name.lowercase() }))
