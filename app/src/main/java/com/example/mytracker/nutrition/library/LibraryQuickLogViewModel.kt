package com.example.mytracker.nutrition.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.nutrition.diary.DiaryRepository
import com.example.mytracker.nutrition.diary.DiarySourceType
import com.example.mytracker.nutrition.diary.MealType
import com.example.mytracker.nutrition.diary.defaultMealType
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.FoodRepository
import com.example.mytracker.nutrition.food.FoodUnit
import com.example.mytracker.nutrition.food.amountInBaseUnits
import com.example.mytracker.nutrition.food.defaultAmountText
import com.example.mytracker.nutrition.recipe.RecipeWithNutrition
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the Bibliothek is about to log; null while no dialog is open. */
sealed interface QuickLogTarget {
    val name: String

    data class Food(val food: FoodItem) : QuickLogTarget {
        override val name: String get() = food.name
    }

    data class Recipe(val recipe: RecipeWithNutrition) : QuickLogTarget {
        override val name: String get() = recipe.recipe.name
    }
}

/**
 * Logging a Lebensmittel or Rezept straight from the Bibliothek, without the detour through the
 * Tagebuch's own picker. Always writes to *today*: the Bibliothek has no day of its own, and a day
 * picker here would be a second, worse version of what the Tagebuch already does. The meal follows
 * the clock the way the Tagebuch's add button does and stays changeable in the dialog.
 *
 * Lives in the library package rather than in the two list ViewModels, so both tabs share one dialog
 * and one snackbar and `food`/`recipe` keep knowing nothing about the Tagebuch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryQuickLogViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val _target = MutableStateFlow<QuickLogTarget?>(null)
    val target: StateFlow<QuickLogTarget?> = _target.asStateFlow()

    private val _mealType = MutableStateFlow(defaultMealType(LocalTime.now()))
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    private val _amountText = MutableStateFlow("")
    val amountText: StateFlow<String> = _amountText.asStateFlow()

    /** The target food's named units; empty for a Rezept, whose amount is always in Portionen. */
    val units: StateFlow<List<FoodUnit>> = _target
        .flatMapLatest { target ->
            (target as? QuickLogTarget.Food)?.let { foodRepository.observeUnits(it.food.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** null = the amount is being typed in the food's base unit (g/ml). */
    private val _selectedUnitId = MutableStateFlow<String?>(null)
    val selectedUnitId: StateFlow<String?> = _selectedUnitId.asStateFlow()

    private val selectedUnit: FoodUnit?
        get() = units.value.firstOrNull { it.id == _selectedUnitId.value }

    val canConfirm: StateFlow<Boolean> = _amountText
        .map { (it.toLocaleDoubleOrNull() ?: 0.0) > 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _logged = MutableSharedFlow<String>()
    val logged = _logged.asSharedFlow()

    /**
     * Prefills with what this food was last logged as, exactly like the Tagebuch's picker does, so
     * both ways of logging the same item start from the same amount.
     */
    fun startFood(food: FoodItem) {
        _target.value = QuickLogTarget.Food(food)
        _mealType.value = defaultMealType(LocalTime.now())
        _amountText.value = defaultAmountText(null)
        _selectedUnitId.value = null
        viewModelScope.launch {
            val last = diaryRepository.getLastLoggedAmount(DiarySourceType.FOOD, food.id) ?: return@launch
            // Another target by now means the dialog was closed and reopened while this was loading.
            if ((_target.value as? QuickLogTarget.Food)?.food?.id != food.id) return@launch
            _amountText.value = last.unitCount?.formatDecimal(1) ?: last.quantity.formatDecimal(1)
            _selectedUnitId.value = last.unitName?.let { name ->
                foodRepository.getUnits(food.id).firstOrNull { it.name == name }?.id
            }
        }
    }

    fun startRecipe(recipe: RecipeWithNutrition) {
        _target.value = QuickLogTarget.Recipe(recipe)
        _mealType.value = defaultMealType(LocalTime.now())
        _amountText.value = "1"
        _selectedUnitId.value = null
    }

    fun dismiss() {
        _target.value = null
        _amountText.value = ""
        _selectedUnitId.value = null
    }

    fun onAmountChange(value: String) { _amountText.value = value }

    fun onMealTypeChange(value: MealType) { _mealType.value = value }

    fun selectUnit(unitId: String?) {
        if (unitId == _selectedUnitId.value) return
        _selectedUnitId.value = unitId
        _amountText.value = defaultAmountText(units.value.firstOrNull { it.id == unitId })
    }

    fun confirm() {
        val target = _target.value ?: return
        val typed = _amountText.value.toLocaleDoubleOrNull()?.takeIf { it > 0.0 } ?: return
        val epochDay = DateUtils.todayEpochDay()
        val meal = _mealType.value

        when (target) {
            is QuickLogTarget.Food -> {
                val unit = selectedUnit
                val amount = amountInBaseUnits(_amountText.value, unit) ?: return
                viewModelScope.launch {
                    diaryRepository.logFood(
                        epochDay = epochDay,
                        foodId = target.food.id,
                        amountBaseUnits = amount,
                        mealType = meal,
                        unitName = unit?.name,
                        unitCount = unit?.let { typed },
                    )
                    _logged.emit(target.name)
                    dismiss()
                }
            }
            is QuickLogTarget.Recipe -> {
                viewModelScope.launch {
                    diaryRepository.logRecipe(epochDay, target.recipe.recipe.id, typed, meal)
                    _logged.emit(target.name)
                    dismiss()
                }
            }
        }
    }
}
