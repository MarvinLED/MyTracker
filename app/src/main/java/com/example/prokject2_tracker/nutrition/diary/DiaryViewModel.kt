package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.core.datastore.NutrientGoal
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.fluid.FluidEntry
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.fluid.FluidType
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
    /** Only the nutrients with a goal set — the macros without one get no target in their bar. */
    val nutrientGoals: Map<Nutrient, NutrientGoal> = emptyMap(),
    /**
     * The day's drinks, shown here as one bar rather than sending the user to Flüssigkeiten to see
     * whether they are on track. [fluidTypes] comes along because the bar's segments are coloured by
     * the type's position in the library, so the same drink keeps its colour on both screens.
     */
    val fluidEntries: List<FluidEntry> = emptyList(),
    val fluidTypes: List<FluidType> = emptyList(),
    val fluidGoalMl: Double = 2000.0,
) {
    val totalKcal: Double get() = totals.kcal
    val calorieGoalKcal: Double get() = nutrientGoals[Nutrient.KCAL]?.value ?: 2000.0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val fluidRepository: FluidRepository,
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
                fluidRepository.observeForDay(epochDay),
                fluidRepository.observeTypes(),
            ) { entries, totals, prefs, fluidEntries, fluidTypes ->
                DiaryDayUiState(
                    epochDay = epochDay,
                    entriesByMeal = entries.groupBy { it.mealType },
                    totals = totals,
                    nutrientGoals = prefs.nutrientGoals,
                    fluidEntries = fluidEntries,
                    fluidTypes = fluidTypes,
                    fluidGoalMl = prefs.dailyWaterGoalMl,
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
