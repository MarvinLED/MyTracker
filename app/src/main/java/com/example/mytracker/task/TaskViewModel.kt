package com.example.mytracker.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskUiState(
    val today: Long = DateUtils.todayEpochDay(),
    /** Owed now, plus what was ticked off today — the working list at the top of the screen. */
    val due: List<TaskStatus> = emptyList(),
    /** Everything else: nothing owed, waiting for its next turn. */
    val upcoming: List<TaskStatus> = emptyList(),
) {
    val isEmpty: Boolean get() = due.isEmpty() && upcoming.isEmpty()
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    // Fixed at construction like the Habits screen: this is "today", and a screen that silently
    // rolled over at midnight would change what it says under the user's hands.
    private val today = DateUtils.todayEpochDay()

    val uiState: StateFlow<TaskUiState> = combine(
        taskRepository.observeActive(),
        taskRepository.observeCompletions(),
    ) { tasks, completions ->
        val statuses = taskStatuses(tasks, completions, today)
        val due = statuses.dueToday()
        TaskUiState(
            today = today,
            due = due,
            upcoming = (statuses - due.toSet()).sortedWith(
                compareBy({ it.nextDueDay ?: Long.MAX_VALUE }, { it.task.name.lowercase() }),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskUiState())

    /**
     * Ticks the open occurrence off, or takes back today's tick. Which due day that settles is the
     * status's business, not the button's — tapping "erledigt" on a task three days late closes
     * *that* day, so the rhythm picks up from where it actually stood.
     */
    fun toggleCompleted(status: TaskStatus) {
        viewModelScope.launch {
            val openDueDay = status.openDueDay
            if (openDueDay != null) {
                taskRepository.setCompleted(
                    taskId = status.task.id,
                    dueEpochDay = openDueDay,
                    completedEpochDay = today,
                    completed = true,
                )
                return@launch
            }
            // Nothing open, so the tap can only mean "undo what I ticked off today".
            taskRepository.getCompletionsForTask(status.task.id)
                .filter { it.completedEpochDay == today }
                .maxByOrNull { it.dueEpochDay }
                ?.let { completion ->
                    taskRepository.setCompleted(
                        taskId = completion.taskId,
                        dueEpochDay = completion.dueEpochDay,
                        completedEpochDay = today,
                        completed = false,
                    )
                }
        }
    }

    fun addTask(
        name: String,
        recurrence: TaskRecurrence,
        startEpochDay: Long,
        intervalCount: Int,
        weekdayMask: Int,
        dayOfMonth: Int,
    ) {
        viewModelScope.launch {
            taskRepository.createTask(
                name = name,
                recurrence = recurrence,
                startEpochDay = startEpochDay,
                intervalCount = intervalCount,
                weekdayMask = weekdayMask,
                dayOfMonth = dayOfMonth,
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { taskRepository.updateTask(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }
}
