package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.fluid.FluidEntry
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.fluid.FluidType
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

data class DiaryDayUiState(
    val epochDay: Long,
    val entriesByMeal: Map<MealType, List<DiaryEntry>>,
    val totalKcal: Double,
    val calorieGoalKcal: Double,
    val fluidEntries: List<FluidEntry> = emptyList(),
    val fluidTotalMl: Double = 0.0,
    val fluidGoalMl: Double = 2000.0,
)

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
                diaryRepository.observeDayTotalKcal(epochDay),
                userPreferencesRepository.userPreferences,
                fluidRepository.observeForDay(epochDay),
                fluidRepository.observeDayTotalMl(epochDay),
            ) { entries, total, prefs, fluidEntries, fluidTotal ->
                DiaryDayUiState(
                    epochDay = epochDay,
                    entriesByMeal = entries.groupBy { it.mealType },
                    totalKcal = total,
                    calorieGoalKcal = prefs.dailyCalorieGoalKcal,
                    fluidEntries = fluidEntries,
                    fluidTotalMl = fluidTotal,
                    fluidGoalMl = prefs.dailyWaterGoalMl,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DiaryDayUiState(_selectedEpochDay.value, emptyMap(), 0.0, 2000.0),
        )

    fun goToPreviousDay() {
        _selectedEpochDay.value -= 1
    }

    fun goToNextDay() {
        _selectedEpochDay.value += 1
    }

    fun goToToday() {
        _selectedEpochDay.value = DateUtils.todayEpochDay()
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch { diaryRepository.delete(entry) }
    }

    fun quickAddFluid(type: FluidType, amountMl: Double) {
        viewModelScope.launch { fluidRepository.logFluid(_selectedEpochDay.value, type, amountMl) }
    }

    fun deleteFluidEntry(entry: FluidEntry) {
        viewModelScope.launch { fluidRepository.delete(entry) }
    }
}
