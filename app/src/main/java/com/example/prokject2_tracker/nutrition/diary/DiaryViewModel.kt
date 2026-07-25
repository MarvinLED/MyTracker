package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.nutrition.NutritionTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A deleted entry held for one undo, together with the per-day recipe copy that went with it. */
data class DeletedEntry(
    val entry: DiaryEntry,
    val dayIngredients: List<DiaryRecipeIngredientDraft>,
)

data class DiaryDayUiState(
    val epochDay: Long,
    val entriesByMeal: Map<MealType, List<DiaryEntry>>,
    val totals: NutritionTotals = NutritionTotals.ZERO,
    /** Only the nutrients with a goal set — those are the ones the day gets a bar for. */
    val nutrientGoals: Map<Nutrient, NutrientGoal> = emptyMap(),
) {
    val totalKcal: Double get() = totals.kcal
    val calorieGoalKcal: Double get() = nutrientGoals[Nutrient.KCAL]?.value ?: 2000.0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _selectedEpochDay = MutableStateFlow(DateUtils.todayEpochDay())
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay.asStateFlow()

    val uiState: StateFlow<DiaryDayUiState> = _selectedEpochDay
        .flatMapLatest { epochDay ->
            combine(
                diaryRepository.observeForDay(epochDay),
                diaryRepository.observeDayNutritionTotals(epochDay),
                userPreferencesRepository.userPreferences,
            ) { entries, totals, prefs ->
                DiaryDayUiState(
                    epochDay = epochDay,
                    entriesByMeal = entries.groupBy { it.mealType },
                    totals = totals,
                    nutrientGoals = prefs.nutrientGoals,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DiaryDayUiState(_selectedEpochDay.value, emptyMap()),
        )

    fun goToPreviousDay() {
        _selectedEpochDay.value -= 1
        _undoableDelete.value = null
    }

    fun goToNextDay() {
        _selectedEpochDay.value += 1
        _undoableDelete.value = null
    }

    fun goToToday() {
        _selectedEpochDay.value = DateUtils.todayEpochDay()
        _undoableDelete.value = null
    }

    /**
     * The last deleted entry, kept only so it can be put back. Cleared on undo and whenever the day
     * changes: an undo button that reinstates something onto a day you're no longer looking at would
     * be a worse surprise than losing the undo.
     */
    private val _undoableDelete = MutableStateFlow<DeletedEntry?>(null)
    val undoableDelete: StateFlow<DeletedEntry?> = _undoableDelete.asStateFlow()

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            // Read the per-day recipe copy before deleting, since the delete cascades it away.
            val dayIngredients = diaryRepository.getRecipeIngredientDrafts(entry.id)
            diaryRepository.delete(entry)
            _undoableDelete.value = DeletedEntry(entry, dayIngredients)
        }
    }

    fun undoDelete() {
        val deleted = _undoableDelete.value ?: return
        _undoableDelete.value = null
        viewModelScope.launch { diaryRepository.restore(deleted.entry, deleted.dayIngredients) }
    }

    fun dismissUndo() {
        _undoableDelete.value = null
    }
}
