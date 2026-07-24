package com.example.prokject2_tracker.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prokject2_tracker.core.datastore.UserPreferencesRepository
import com.example.prokject2_tracker.core.datastore.WeightUnit
import com.example.prokject2_tracker.core.util.DateUtils
import com.example.prokject2_tracker.core.util.lbToKg
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.fluid.FluidType
import com.example.prokject2_tracker.habit.Habit
import com.example.prokject2_tracker.habit.HabitRepository
import com.example.prokject2_tracker.nutrition.diary.DiaryRepository
import com.example.prokject2_tracker.nutrition.diary.MealType
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.FoodRepository
import com.example.prokject2_tracker.weight.BodyWeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OverviewUiState(
    val habits: List<Habit> = emptyList(),
    val checkedInHabitIds: Set<String> = emptySet(),
    val habitValues: Map<String, Double> = emptyMap(),
    val habitStreaks: Map<String, Int> = emptyMap(),
    val fluidTypes: List<FluidType> = emptyList(),
    val fluidTotalMl: Double = 0.0,
    val fluidGoalMl: Double = 2000.0,
    val todayWeightKg: Double? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
)

/**
 * "Home" tab: a condensed, single-scroll composition of Habits/Fluid/Food/Weight actions that
 * already exist as their own feature screens. Owns zero feature-specific logic itself — same
 * spirit as [com.example.prokject2_tracker.analyse.AnalyseViewModel] — it only injects and
 * composes the existing repositories' flows/calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val fluidRepository: FluidRepository,
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val bodyWeightRepository: BodyWeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val today = DateUtils.todayEpochDay()

    private val habitSlice = combine(
        habitRepository.observeActive(),
        habitRepository.observeCheckInsForDay(today),
    ) { habits, checkIns -> habits to checkIns }

    private val fluidSlice = combine(
        fluidRepository.observeTypes(),
        fluidRepository.observeDayTotalMl(today),
    ) { types, totalMl -> types to totalMl }

    val uiState: StateFlow<OverviewUiState> = combine(
        habitSlice,
        fluidSlice,
        bodyWeightRepository.observeForDay(today),
        userPreferencesRepository.userPreferences,
    ) { (habits, checkIns), (fluidTypes, fluidTotalMl), weightEntry, prefs ->
        OverviewUiState(
            habits = habits,
            checkedInHabitIds = checkIns.map { it.habitId }.toSet(),
            habitValues = checkIns.mapNotNull { checkIn -> checkIn.value?.let { checkIn.habitId to it } }.toMap(),
            habitStreaks = habits.associate { it.id to habitRepository.getCurrentStreak(it, today) },
            fluidTypes = fluidTypes,
            fluidTotalMl = fluidTotalMl,
            fluidGoalMl = prefs.dailyWaterGoalMl,
            todayWeightKg = weightEntry?.weightKg,
            weightUnit = prefs.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState())

    private val _foodQuery = MutableStateFlow("")
    val foodQuery: StateFlow<String> = _foodQuery.asStateFlow()

    val foodResults: StateFlow<List<FoodItem>> = _foodQuery
        .flatMapLatest { query -> if (query.isBlank()) flowOf(emptyList()) else foodRepository.search(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFood = MutableStateFlow<FoodItem?>(null)
    val selectedFood: StateFlow<FoodItem?> = _selectedFood.asStateFlow()

    private val _amountText = MutableStateFlow("")
    val amountText: StateFlow<String> = _amountText.asStateFlow()

    private val _mealType = MutableStateFlow(defaultMealTypeForTimeOfDay())
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    fun onFoodQueryChange(value: String) {
        _foodQuery.value = value
    }

    fun onSelectFood(food: FoodItem) {
        _selectedFood.value = food
        _amountText.value = food.servingAmount?.toString() ?: ""
    }

    fun onAmountChange(value: String) {
        _amountText.value = value
    }

    fun onMealTypeChange(value: MealType) {
        _mealType.value = value
    }

    fun confirmLogFood() {
        val food = _selectedFood.value ?: return
        val amount = _amountText.value.toDoubleOrNull() ?: return
        val mealType = _mealType.value
        viewModelScope.launch {
            diaryRepository.logFood(today, food.id, amount, mealType)
            _selectedFood.value = null
            _amountText.value = ""
            _foodQuery.value = ""
        }
    }

    fun onToggleHabit(habitId: String) {
        val checked = habitId in uiState.value.checkedInHabitIds
        viewModelScope.launch { habitRepository.setCheckedIn(habitId, today, checked = !checked) }
    }

    fun onLogHabitValue(habitId: String, value: Double?) {
        viewModelScope.launch { habitRepository.logValue(habitId, today, value) }
    }

    fun onQuickAddFluid(fluidTypeId: String) {
        val type = uiState.value.fluidTypes.firstOrNull { it.id == fluidTypeId } ?: return
        viewModelScope.launch { fluidRepository.logFluid(today, type, type.defaultQuickAddMl) }
    }

    /** [input] is in the user's current preferred unit; converted to kg before storing. */
    fun onSaveWeight(input: String) {
        val displayValue = input.toLocaleDoubleOrNull() ?: return
        val weightKg = when (uiState.value.weightUnit) {
            WeightUnit.KG -> displayValue
            WeightUnit.LB -> displayValue.lbToKg()
        }
        viewModelScope.launch { bodyWeightRepository.logWeight(today, weightKg) }
    }
}

/** Sensible default meal type by time of day; always overridable by the user via chips. */
private fun defaultMealTypeForTimeOfDay(now: LocalTime = LocalTime.now()): MealType = when {
    now.hour < 10 -> MealType.BREAKFAST
    now.hour < 14 -> MealType.LUNCH
    now.hour < 18 -> MealType.SNACK
    else -> MealType.DINNER
}
