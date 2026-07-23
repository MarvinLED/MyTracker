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
)

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
) : ViewModel() {
    private val today = DateUtils.todayEpochDay()

    val uiState: StateFlow<HabitUiState> = combine(
        habitRepository.observeActive(),
        habitRepository.observeCheckInsForDay(today),
    ) { habits, checkIns ->
        HabitUiState(habits = habits, checkedInHabitIds = checkIns.map { it.habitId }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitUiState())

    fun toggleCheckedIn(habit: Habit) {
        val isChecked = habit.id in uiState.value.checkedInHabitIds
        viewModelScope.launch { habitRepository.setCheckedIn(habit.id, today, checked = !isChecked) }
    }

    fun addHabit(name: String) {
        viewModelScope.launch { habitRepository.createHabit(name) }
    }

    fun renameHabit(habit: Habit, name: String) {
        viewModelScope.launch { habitRepository.renameHabit(habit, name) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { habitRepository.deleteHabit(habit) }
    }
}
