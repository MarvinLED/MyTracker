package com.example.prokject2_tracker.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitUiState(
    val habits: List<Habit> = emptyList(),
    val checkedInHabitIds: Set<String> = emptySet(),
    val valuesByHabitId: Map<String, Double> = emptyMap(),
    val streaksByHabitId: Map<String, Int> = emptyMap(),
    val goalsByHabitId: Map<String, List<HabitGoal>> = emptyMap(),
    val progressByGoalId: Map<String, Double> = emptyMap(),
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {
    private val today = DateUtils.todayEpochDay()

    val uiState: StateFlow<HabitUiState> = combine(
        habitRepository.observeActive(),
        habitRepository.observeCheckInsForDay(today),
        habitRepository.observeGoalsByHabitId(),
    ) { habits, checkIns, goalsByHabitId ->
        val habitsById = habits.associateBy { it.id }
        val streaksByHabitId = habits.associate { it.id to habitRepository.getCurrentStreak(it, today) }
        val progressByGoalId = goalsByHabitId.values.flatten().associate { goal ->
            val habit = habitsById.getValue(goal.habitId)
            goal.id to habitRepository.getPeriodProgress(habit, goal, today)
        }
        HabitUiState(
            habits = habits,
            checkedInHabitIds = checkIns.map { it.habitId }.toSet(),
            valuesByHabitId = checkIns.mapNotNull { checkIn -> checkIn.value?.let { checkIn.habitId to it } }.toMap(),
            streaksByHabitId = streaksByHabitId,
            goalsByHabitId = goalsByHabitId,
            progressByGoalId = progressByGoalId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitUiState())

    fun toggleCheckedIn(habit: Habit) {
        val isChecked = habit.id in uiState.value.checkedInHabitIds
        viewModelScope.launch { habitRepository.setCheckedIn(habit.id, today, checked = !isChecked) }
    }

    fun logValue(habit: Habit, value: Double?) {
        viewModelScope.launch { habitRepository.logValue(habit.id, today, value) }
    }

    fun setGoal(habit: Habit, period: GoalPeriod, target: Double?) {
        viewModelScope.launch { habitRepository.setGoal(habit, period, target) }
    }

    fun addHabit(name: String, type: HabitType) {
        viewModelScope.launch { habitRepository.createHabit(name, type) }
    }

    fun renameHabit(habit: Habit, name: String) {
        viewModelScope.launch { habitRepository.renameHabit(habit, name) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.deleteHabit(habit) }
    }
}
