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
import kotlinx.coroutines.flow.combine
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

/** What the typed amount comes to, shown in the dialog before it is confirmed. */
data class LoggedNutrition(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/** Blank is allowed (counts as 0); a non-blank value must parse as a number. */
private fun String.isBlankOrValidNumber(): Boolean = isBlank() || toLocaleDoubleOrNull() != null

/** Blank means "not specified" and contributes 0 to the entry. */
private fun String.toOptionalNutrient(): Double = toLocaleDoubleOrNull() ?: 0.0

/** The Schnelleintrag form: only [kcal] is required, every macro stays optional. */
data class QuickEntryState(
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
) {
    val isValid: Boolean
        get() = kcal.toLocaleDoubleOrNull()?.let { it > 0.0 } == true &&
            protein.isBlankOrValidNumber() &&
            carbs.isBlankOrValidNumber() &&
            fat.isBlankOrValidNumber()
}

/**
 * Everything the Bibliothek writes into the Tagebuch: the "Hinzufügen" dialog behind a tapped row,
 * and the Schnelleintrag tab.
 *
 * The day is not chosen here. Reached from the drawer the Bibliothek has no day of its own and
 * writes to today, the way it always has; reached from a meal's "+" in the Tagebuch it is handed
 * that day and meal through [setContext]. Either way the dialog says out loud which day it is about
 * to write to, so an entry never turns up on a day the user did not pick.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryQuickLogViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val _target = MutableStateFlow<QuickLogTarget?>(null)
    val target: StateFlow<QuickLogTarget?> = _target.asStateFlow()

    /** The day handed in by the route; null means "no day of its own", i.e. today. */
    private val _contextDay = MutableStateFlow<Long?>(null)
    val contextDay: StateFlow<Long?> = _contextDay.asStateFlow()

    private var contextMeal: MealType? = null

    private val _mealType = MutableStateFlow(defaultMealType(LocalTime.now()))
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    private val _quickMealType = MutableStateFlow(defaultMealType(LocalTime.now()))
    val quickMealType: StateFlow<MealType> = _quickMealType.asStateFlow()

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

    /**
     * What the entry will come to. Null while the amount is empty or nonsense — an outdated set of
     * numbers standing under a half-typed amount would be worse than none.
     */
    val preview: StateFlow<LoggedNutrition?> =
        combine(_target, _amountText, _selectedUnitId, units) { target, text, unitId, unitList ->
            when (target) {
                null -> null
                is QuickLogTarget.Food -> {
                    val unit = unitList.firstOrNull { it.id == unitId }
                    val baseUnits = amountInBaseUnits(text, unit, target.food.portionUnitName)
                        ?.takeIf { it > 0.0 }
                    baseUnits?.let { amount ->
                        val factor = amount / 100.0
                        LoggedNutrition(
                            kcal = target.food.kcalPer100 * factor,
                            protein = target.food.proteinPer100 * factor,
                            carbs = target.food.carbsPer100 * factor,
                            fat = target.food.fatPer100 * factor,
                        )
                    }
                }
                is QuickLogTarget.Recipe -> {
                    val servings = text.toLocaleDoubleOrNull()?.takeIf { it > 0.0 }
                    servings?.let {
                        val per = target.recipe.perServing
                        LoggedNutrition(
                            kcal = per.kcal * it,
                            protein = per.protein * it,
                            carbs = per.carbs * it,
                            fat = per.fat * it,
                        )
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _quick = MutableStateFlow(QuickEntryState())
    val quick: StateFlow<QuickEntryState> = _quick.asStateFlow()

    private val _logged = MutableSharedFlow<String>()
    val logged = _logged.asSharedFlow()

    /**
     * The day and meal this screen was opened for. Passing null for either keeps the old Bibliothek
     * behaviour: today, and the meal the clock suggests.
     */
    fun setContext(epochDay: Long?, mealType: MealType?) {
        _contextDay.value = epochDay
        contextMeal = mealType
        if (mealType != null && _target.value == null) {
            // Only while no dialog is open: overwriting a meal the user just picked in an open
            // dialog would undo their choice on the next recomposition.
            _mealType.value = mealType
            _quickMealType.value = mealType
        }
    }

    /** The day an entry made now would land on. */
    private fun logDay(): Long = _contextDay.value ?: DateUtils.todayEpochDay()

    private fun startingMeal(): MealType = contextMeal ?: defaultMealType(LocalTime.now())

    /**
     * Prefills with what this food was last logged as, so reaching a food through the Bibliothek and
     * through a meal's "+" starts from the same amount.
     */
    fun startFood(food: FoodItem) {
        _target.value = QuickLogTarget.Food(food)
        _mealType.value = startingMeal()
        _amountText.value = defaultAmountText(null, food.portionUnitName)
        _selectedUnitId.value = null
        viewModelScope.launch {
            val units = foodRepository.getUnits(food.id)
            // A food without a weight offers no chips, so its one portion is selected here — without
            // it the typed count would be read as grams.
            val portionUnitId = food.portionUnitName?.let { name -> units.firstOrNull { it.name == name }?.id }
            if ((_target.value as? QuickLogTarget.Food)?.food?.id != food.id) return@launch
            _selectedUnitId.value = portionUnitId
            val last = diaryRepository.getLastLoggedAmount(DiarySourceType.FOOD, food.id) ?: return@launch
            // Another target by now means the dialog was closed and reopened while this was loading.
            if ((_target.value as? QuickLogTarget.Food)?.food?.id != food.id) return@launch
            _amountText.value = last.unitCount?.formatDecimal(1) ?: last.quantity.formatDecimal(1)
            _selectedUnitId.value = last.unitName?.let { name ->
                units.firstOrNull { it.name == name }?.id
            } ?: portionUnitId
        }
    }

    fun startRecipe(recipe: RecipeWithNutrition) {
        _target.value = QuickLogTarget.Recipe(recipe)
        _mealType.value = startingMeal()
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

    fun onQuickMealTypeChange(value: MealType) { _quickMealType.value = value }

    fun selectUnit(unitId: String?) {
        if (unitId == _selectedUnitId.value) return
        _selectedUnitId.value = unitId
        _amountText.value = defaultAmountText(units.value.firstOrNull { it.id == unitId })
    }

    fun onQuickNameChange(value: String) { _quick.value = _quick.value.copy(name = value) }
    fun onQuickKcalChange(value: String) { _quick.value = _quick.value.copy(kcal = value) }
    fun onQuickProteinChange(value: String) { _quick.value = _quick.value.copy(protein = value) }
    fun onQuickCarbsChange(value: String) { _quick.value = _quick.value.copy(carbs = value) }
    fun onQuickFatChange(value: String) { _quick.value = _quick.value.copy(fat = value) }

    fun confirm() {
        val target = _target.value ?: return
        val typed = _amountText.value.toLocaleDoubleOrNull()?.takeIf { it > 0.0 } ?: return
        val epochDay = logDay()
        val meal = _mealType.value

        when (target) {
            is QuickLogTarget.Food -> {
                val unit = selectedUnit
                val amount = amountInBaseUnits(_amountText.value, unit, target.food.portionUnitName) ?: return
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

    fun confirmQuick() {
        val q = _quick.value
        val kcal = q.kcal.toLocaleDoubleOrNull() ?: return
        val name = q.name.ifBlank { "Schnelleintrag" }
        viewModelScope.launch {
            diaryRepository.logQuick(
                epochDay = logDay(),
                name = name,
                kcal = kcal,
                protein = q.protein.toOptionalNutrient(),
                carbs = q.carbs.toOptionalNutrient(),
                fat = q.fat.toOptionalNutrient(),
                mealType = _quickMealType.value,
            )
            _logged.emit(name)
            _quick.value = QuickEntryState()
        }
    }
}
