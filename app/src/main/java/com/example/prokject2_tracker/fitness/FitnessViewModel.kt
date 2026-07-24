package com.example.prokject2_tracker.fitness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.fitness.cardio.CardioRepository
import com.example.prokject2_tracker.fitness.cardio.CardioSession
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseRepository
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fitness.strength.StrengthLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FitnessUiState(
    val rows: List<TrainingListRow> = emptyList(),
    val daysSinceLastCardio: Long? = null,
    val daysSinceLastStrength: Long? = null,
    val goals: List<FitnessGoal> = emptyList(),
    val progressByGoalId: Map<String, Double> = emptyMap(),
)

@HiltViewModel
class FitnessViewModel @Inject constructor(
    private val cardioRepository: CardioRepository,
    private val strengthLogRepository: StrengthLogRepository,
    private val strengthExerciseRepository: StrengthExerciseRepository,
    private val fitnessGoalRepository: FitnessGoalRepository,
) : ViewModel() {
    val uiState: StateFlow<FitnessUiState> = combine(
        cardioRepository.observeAll(),
        strengthLogRepository.observeAll(),
        strengthLogRepository.observeAllSets(),
        fitnessGoalRepository.observeAll(),
    ) { cardioSessions, strengthEntries, allSets, goals ->
        val today = DateUtils.todayEpochDay()
        val setsByEntryId = allSets.groupBy { it.logEntryId }
        val rows: List<TrainingListRow> = cardioSessions.map { TrainingListRow.Cardio(it) } +
            strengthEntries.map { entry -> TrainingListRow.Strength(entry, setsByEntryId[entry.id].orEmpty()) }
        val sortedRows = rows.sortedWith(
            compareByDescending<TrainingListRow> { it.epochDay }.thenByDescending { it.createdAt },
        )
        val daysSinceLastCardio = cardioSessions.maxOfOrNull { it.epochDay }
            ?.let { DateUtils.daysBetweenEpochDays(it, today) }
        val daysSinceLastStrength = strengthEntries.maxOfOrNull { it.epochDay }
            ?.let { DateUtils.daysBetweenEpochDays(it, today) }
        val progressByGoalId = goals.associate { goal ->
            goal.id to fitnessGoalRepository.getPeriodProgress(goal, today)
        }
        FitnessUiState(
            rows = sortedRows,
            daysSinceLastCardio = daysSinceLastCardio,
            daysSinceLastStrength = daysSinceLastStrength,
            goals = goals,
            progressByGoalId = progressByGoalId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FitnessUiState())

    init {
        viewModelScope.launch {
            cardioRepository.ensureDefaultActivityTypesSeeded()
            strengthExerciseRepository.ensureDefaultMuscleGroupsSeeded()
        }
    }

    fun deleteCardio(session: CardioSession) {
        viewModelScope.launch { cardioRepository.delete(session) }
    }

    fun deleteStrength(entry: StrengthLogEntry) {
        viewModelScope.launch { strengthLogRepository.delete(entry) }
    }
}
