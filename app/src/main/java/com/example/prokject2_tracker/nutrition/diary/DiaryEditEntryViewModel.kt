package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.core.util.formatDecimal
import com.example.prokject2_tracker.core.util.toLocaleDoubleOrNull
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.FoodRepository
import com.example.prokject2_tracker.nutrition.food.fluidMlOf
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One ingredient of the day's version of a recipe, as edited on screen. */
data class DayIngredientRow(
    val foodId: String,
    val foodName: String,
    val baseUnit: BaseUnit,
    val amountText: String,
    val fluidTypeId: String? = null,
    val fluidMlPer100: Double? = null,
) {
    val fluidMl: Double
        get() = fluidMlOf(fluidTypeId, fluidMlPer100, amountText.toLocaleDoubleOrNull() ?: 0.0)
}

data class DiaryEditEntryState(
    val entry: DiaryEntry? = null,
    val quantityText: String = "",
    val mealType: MealType = MealType.BREAKFAST,
    /** The recipe's ingredients for this day — empty for food and Schnelleintrag entries. */
    val ingredients: List<DayIngredientRow> = emptyList(),
    /** True once the day's list differs from what the entry started with, i.e. there is something to save. */
    val hasIngredientChanges: Boolean = false,
    /** True when the entry already carried its own per-day copy when the screen opened. */
    val hadDayIngredients: Boolean = false,
    val isSaved: Boolean = false,
) {
    val isRecipe: Boolean get() = entry?.sourceType == DiarySourceType.RECIPE

    /** A Schnelleintrag's amount is meaningless ("1 Schnelleintrag"), so it isn't editable. */
    val isQuantityEditable: Boolean get() = entry != null && entry.sourceType != DiarySourceType.QUICK

    val isValid: Boolean
        get() = entry != null &&
            (!isQuantityEditable || quantityText.toLocaleDoubleOrNull()?.let { it > 0.0 } == true) &&
            ingredients.all { it.amountText.toLocaleDoubleOrNull()?.let { amount -> amount > 0.0 } == true }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryEditEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val diaryRepository: DiaryRepository,
    private val foodRepository: FoodRepository,
    fluidRepository: FluidRepository,
) : ViewModel() {
    private val route: DiaryEditEntryRoute = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(DiaryEditEntryState())
    val state: StateFlow<DiaryEditEntryState> = _state.asStateFlow()

    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    val pickerResults: StateFlow<List<FoodItem>> = _pickerQuery
        .flatMapLatest { q -> if (q.isBlank()) foodRepository.observeAll() else foodRepository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fluidTypeNames: StateFlow<Map<String, String>> = fluidRepository.observeTypes()
        .map { types -> types.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            val entry = diaryRepository.getEntry(route.entryId) ?: return@launch
            // For a recipe entry the day's list starts as whatever the entry already follows: its own
            // copy if it has one, otherwise the library recipe's ingredients as they are right now.
            _state.value = DiaryEditEntryState(
                entry = entry,
                quantityText = entry.quantity.formatDecimal(3),
                mealType = entry.mealType,
                ingredients = diaryRepository.getRecipeIngredientsInEffect(entry).map {
                    it.food.toDayIngredientRow(it.amountBaseUnits)
                },
                hadDayIngredients = diaryRepository.hasRecipeDayIngredients(entry.id),
            )
        }
    }

    fun onQuantityChange(value: String) { _state.value = _state.value.copy(quantityText = value) }
    fun onMealTypeChange(value: MealType) { _state.value = _state.value.copy(mealType = value) }
    fun onPickerQueryChange(value: String) { _pickerQuery.value = value }

    fun onIngredientAmountChange(foodId: String, amountText: String) {
        updateIngredients(
            _state.value.ingredients.map { if (it.foodId == foodId) it.copy(amountText = amountText) else it },
        )
    }

    fun addIngredient(food: FoodItem) {
        if (_state.value.ingredients.any { it.foodId == food.id }) return
        updateIngredients(_state.value.ingredients + food.toDayIngredientRow(amountBaseUnits = null))
    }

    fun removeIngredient(foodId: String) {
        updateIngredients(_state.value.ingredients.filterNot { it.foodId == foodId })
    }

    /** Gives the entry the library recipe back, dropping the day's own version of it. */
    fun resetIngredientsToRecipe() {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            val quantity = _state.value.quantityText.toLocaleDoubleOrNull() ?: entry.quantity
            diaryRepository.resetRecipeEntryToLibrary(entry, quantity, _state.value.mealType)
            _state.value = _state.value.copy(isSaved = true)
        }
    }

    fun save() {
        val s = _state.value
        val entry = s.entry ?: return
        if (!s.isValid) return
        val quantity = if (s.isQuantityEditable) {
            s.quantityText.toLocaleDoubleOrNull() ?: return
        } else {
            entry.quantity
        }
        viewModelScope.launch {
            // Only write a per-day copy when the user actually changed the recipe (or the entry
            // already had one) — otherwise the entry keeps following the library recipe.
            if (s.isRecipe && (s.hasIngredientChanges || s.hadDayIngredients)) {
                diaryRepository.updateRecipeEntryIngredients(
                    entry = entry,
                    newQuantity = quantity,
                    newMealType = s.mealType,
                    ingredients = s.ingredients.map {
                        DiaryRecipeIngredientDraft(
                            foodId = it.foodId,
                            amountBaseUnits = it.amountText.toLocaleDoubleOrNull() ?: 0.0,
                        )
                    },
                )
            } else {
                diaryRepository.updateEntry(entry, quantity, s.mealType)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }

    private fun updateIngredients(rows: List<DayIngredientRow>) {
        _state.value = _state.value.copy(ingredients = rows, hasIngredientChanges = true)
    }
}

private fun FoodItem.toDayIngredientRow(amountBaseUnits: Double?) = DayIngredientRow(
    foodId = id,
    foodName = name,
    baseUnit = baseUnit,
    amountText = amountBaseUnits?.formatDecimal(3).orEmpty(),
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
)
