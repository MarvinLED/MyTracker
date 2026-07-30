package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.FoodRepository
import com.example.prokject2_tracker.nutrition.food.FoodUnit
import com.example.prokject2_tracker.nutrition.food.amountInBaseUnits
import com.example.prokject2_tracker.nutrition.food.defaultAmountText
import com.example.prokject2_tracker.nutrition.recipe.RecipeRepository
import com.example.prokject2_tracker.nutrition.recipe.RecipeWithNutrition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryAddEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val diaryRepository: DiaryRepository,
    foodRepository: FoodRepository,
    recipeRepository: RecipeRepository,
) : ViewModel() {
    private val route: DiaryAddEntryRoute = savedStateHandle.toRoute()
    val epochDay: Long = route.epochDay

    private val _sourceType = MutableStateFlow(DiarySourceType.FOOD)
    val sourceType: StateFlow<DiarySourceType> = _sourceType.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Nothing is suggested until something is typed: an unfiltered list of the whole library is a
    // wall of text that buries the fields below it, and it isn't an answer to anything the user asked.
    val foodResults: StateFlow<List<FoodItem>> = _query
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else foodRepository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipeResults: StateFlow<List<RecipeWithNutrition>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(emptyList())
            } else {
                recipeRepository.observeAllWithNutrition().map { list ->
                    list.filter { it.recipe.name.contains(q, ignoreCase = true) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFood = MutableStateFlow<FoodItem?>(null)
    val selectedFood: StateFlow<FoodItem?> = _selectedFood.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<RecipeWithNutrition?>(null)
    val selectedRecipe: StateFlow<RecipeWithNutrition?> = _selectedRecipe.asStateFlow()

    private val _amountText = MutableStateFlow("")
    val amountText: StateFlow<String> = _amountText.asStateFlow()

    /** The selected food's named units, empty for recipes and Schnelleinträge. */
    val foodUnits: StateFlow<List<FoodUnit>> = _selectedFood
        .flatMapLatest { food -> food?.let { foodRepository.observeUnits(it.id) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** null = the amount is being typed in the food's base unit (g/ml). */
    private val _selectedUnitId = MutableStateFlow<String?>(null)
    val selectedUnitId: StateFlow<String?> = _selectedUnitId.asStateFlow()

    private val selectedUnit: FoodUnit?
        get() = foodUnits.value.firstOrNull { it.id == _selectedUnitId.value }

    private val _quick = MutableStateFlow(QuickEntryState())
    val quick: StateFlow<QuickEntryState> = _quick.asStateFlow()

    private val _mealType = MutableStateFlow(MealType.BREAKFAST)
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    val isValid: StateFlow<Boolean> =
        combine(_sourceType, _selectedFood, _selectedRecipe, _amountText, _quick) { type, food, recipe, amount, quick ->
            if (type == DiarySourceType.QUICK) {
                quick.isValid
            } else {
                (food != null || recipe != null) && amount.toLocaleDoubleOrNull()?.let { it > 0.0 } == true
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun selectSourceType(type: DiarySourceType) {
        _sourceType.value = type
        _selectedFood.value = null
        _selectedRecipe.value = null
        _amountText.value = ""
        _selectedUnitId.value = null
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun selectFood(food: FoodItem) {
        _selectedFood.value = food
        // 100 g/ml is the unit the food's values are given in and by far the most common thing to
        // log; a named unit is one tap away from there.
        _amountText.value = defaultAmountText(null)
        _selectedUnitId.value = null
    }

    /**
     * Switches between the base unit (null) and one of the food's named units, prefilling the usual
     * amount for the new mode. Re-tapping the selected chip does nothing — the chips fire on every
     * tap, and resetting a just-typed amount for a mode that didn't change would be a trap.
     */
    fun selectUnit(unitId: String?) {
        if (unitId == _selectedUnitId.value) return
        _selectedUnitId.value = unitId
        _amountText.value = defaultAmountText(selectedUnit)
    }

    /** Steps the amount by [delta], clamped at 0 — the ± buttons next to the Menge field. */
    fun adjustAmount(delta: Double) {
        val current = _amountText.value.toLocaleDoubleOrNull() ?: 0.0
        _amountText.value = (current + delta).coerceAtLeast(0.0).formatDecimal(3)
    }

    fun selectRecipe(recipe: RecipeWithNutrition) {
        _selectedRecipe.value = recipe
        _amountText.value = "1"
        _selectedUnitId.value = null
    }

    fun clearSelection() {
        _selectedFood.value = null
        _selectedRecipe.value = null
        _amountText.value = ""
        _selectedUnitId.value = null
    }

    fun onAmountChange(value: String) { _amountText.value = value }
    fun onMealTypeChange(value: MealType) { _mealType.value = value }

    fun onQuickNameChange(value: String) { _quick.value = _quick.value.copy(name = value) }
    fun onQuickKcalChange(value: String) { _quick.value = _quick.value.copy(kcal = value) }
    fun onQuickProteinChange(value: String) { _quick.value = _quick.value.copy(protein = value) }
    fun onQuickCarbsChange(value: String) { _quick.value = _quick.value.copy(carbs = value) }
    fun onQuickFatChange(value: String) { _quick.value = _quick.value.copy(fat = value) }

    fun save() {
        if (!isValid.value) return
        if (_sourceType.value == DiarySourceType.QUICK) {
            saveQuick()
            return
        }
        val typed = _amountText.value.toLocaleDoubleOrNull() ?: return
        val unit = selectedUnit
        val amount = amountInBaseUnits(_amountText.value, unit) ?: return
        val food = _selectedFood.value
        val recipe = _selectedRecipe.value
        viewModelScope.launch {
            when {
                food != null -> diaryRepository.logFood(
                    epochDay = epochDay,
                    foodId = food.id,
                    amountBaseUnits = amount,
                    mealType = _mealType.value,
                    unitName = unit?.name,
                    unitCount = unit?.let { typed },
                )
                recipe != null -> diaryRepository.logRecipe(epochDay, recipe.recipe.id, typed, _mealType.value)
                else -> return@launch
            }
            _isSaved.value = true
        }
    }

    private fun saveQuick() {
        val q = _quick.value
        val kcal = q.kcal.toLocaleDoubleOrNull() ?: return
        viewModelScope.launch {
            diaryRepository.logQuick(
                epochDay = epochDay,
                name = q.name.ifBlank { "Schnelleintrag" },
                kcal = kcal,
                protein = q.protein.toOptionalNutrient(),
                carbs = q.carbs.toOptionalNutrient(),
                fat = q.fat.toOptionalNutrient(),
                mealType = _mealType.value,
            )
            _isSaved.value = true
        }
    }
}
