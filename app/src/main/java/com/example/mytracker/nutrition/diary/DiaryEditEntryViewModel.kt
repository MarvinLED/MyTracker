package com.example.mytracker.nutrition.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mytracker.core.util.formatDecimal
import com.example.mytracker.core.util.toLocaleDoubleOrNull
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.nutrition.food.BaseUnit
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.FoodRepository
import com.example.mytracker.nutrition.food.FoodUnit
import com.example.mytracker.nutrition.food.amountInBaseUnits
import com.example.mytracker.nutrition.food.defaultAmountText
import com.example.mytracker.nutrition.food.fluidMlOf
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
    /** The number as typed — base units when [selectedUnitId] is null, otherwise a count of that unit. */
    val amountText: String,
    val units: List<FoodUnit> = emptyList(),
    val selectedUnitId: String? = null,
    val fluidTypeId: String? = null,
    val fluidMlPer100: Double? = null,
    /** Non-null for a food without a weight; see [FoodItem.portionUnitName]. */
    val portionUnitName: String? = null,
) {
    val selectedUnit: FoodUnit?
        get() = units.firstOrNull { it.id == selectedUnitId }

    val amountBaseUnits: Double?
        get() = amountInBaseUnits(amountText, selectedUnit, portionUnitName)

    val fluidMl: Double
        get() = fluidMlOf(fluidTypeId, fluidMlPer100, amountBaseUnits ?: 0.0)
}

data class DiaryEditEntryState(
    val entry: DiaryEntry? = null,
    val quantityText: String = "",
    /** The Lebensmittel behind a FOOD entry, null for recipes, Schnelleinträge and deleted foods. */
    val sourceFood: FoodItem? = null,
    /** The source food's named units — empty for recipe and Schnelleintrag entries. */
    val entryUnits: List<FoodUnit> = emptyList(),
    /** null = [quantityText] is in the entry's own [DiaryEntry.quantityUnit]. */
    val selectedUnitId: String? = null,
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

    val selectedUnit: FoodUnit?
        get() = entryUnits.firstOrNull { it.id == selectedUnitId }

    /** The entry's amount in base units (or servings for a recipe), whichever way it was typed. */
    val quantityBaseUnits: Double?
        get() = amountInBaseUnits(quantityText, selectedUnit, sourceFood?.portionUnitName)

    val isValid: Boolean
        get() = entry != null &&
            (!isQuantityEditable || quantityBaseUnits?.let { it > 0.0 } == true) &&
            ingredients.all { it.amountBaseUnits?.let { amount -> amount > 0.0 } == true }
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
            // Only a Lebensmittel entry has units to offer; a recipe's amount is servings.
            val sourceFood = if (entry.sourceType == DiarySourceType.FOOD) {
                foodRepository.getById(entry.sourceId)
            } else {
                null
            }
            val entryUnits = sourceFood?.let { foodRepository.getUnits(it.id) }.orEmpty()
            // The unit was snapshotted by name — match it back so the screen reopens in the mode the
            // entry was logged in. A renamed or deleted unit falls back to base units.
            val entryUnit = entry.unitName?.let { name -> entryUnits.firstOrNull { it.name == name } }
            // For a recipe entry the day's list starts as whatever the entry already follows: its own
            // copy if it has one, otherwise the library recipe's ingredients as they are right now.
            _state.value = DiaryEditEntryState(
                entry = entry,
                quantityText = if (entryUnit != null && entry.unitCount != null) {
                    entry.unitCount.formatDecimal(3)
                } else {
                    entry.quantity.formatDecimal(3)
                },
                sourceFood = sourceFood,
                entryUnits = entryUnits,
                selectedUnitId = entryUnit?.id
                    ?: sourceFood?.portionUnitName?.let { name -> entryUnits.firstOrNull { it.name == name }?.id },
                mealType = entry.mealType,
                ingredients = diaryRepository.getRecipeIngredientsInEffect(entry).map { item ->
                    val units = foodRepository.getUnits(item.food.id)
                    val unit = item.unitName?.let { name -> units.firstOrNull { it.name == name } }
                    item.food.toDayIngredientRow(
                        amountBaseUnits = item.amountBaseUnits,
                        units = units,
                        unit = unit,
                        unitCount = item.unitCount,
                    )
                },
                hadDayIngredients = diaryRepository.hasRecipeDayIngredients(entry.id),
            )
        }
    }

    fun onQuantityChange(value: String) { _state.value = _state.value.copy(quantityText = value) }
    fun onMealTypeChange(value: MealType) { _state.value = _state.value.copy(mealType = value) }
    fun onPickerQueryChange(value: String) { _pickerQuery.value = value }

    /**
     * Switches the entry's amount between base units (null) and one of the food's named units,
     * prefilling the usual amount for the new mode. Re-tapping the selected chip does nothing.
     */
    fun onSelectUnit(unitId: String?) {
        val current = _state.value
        if (unitId == current.selectedUnitId) return
        val to = current.entryUnits.firstOrNull { it.id == unitId }
        _state.value = current.copy(
            selectedUnitId = unitId,
            quantityText = defaultAmountText(to),
        )
    }

    fun onIngredientAmountChange(foodId: String, amountText: String) {
        updateIngredients(
            _state.value.ingredients.map { if (it.foodId == foodId) it.copy(amountText = amountText) else it },
        )
    }

    fun onSelectIngredientUnit(foodId: String, unitId: String?) {
        updateIngredients(
            _state.value.ingredients.map { row ->
                if (row.foodId != foodId || unitId == row.selectedUnitId) {
                    row
                } else {
                    val to = row.units.firstOrNull { it.id == unitId }
                    row.copy(
                        selectedUnitId = unitId,
                        amountText = defaultAmountText(to),
                    )
                }
            },
        )
    }

    fun addIngredient(food: FoodItem) {
        if (_state.value.ingredients.any { it.foodId == food.id }) return
        updateIngredients(_state.value.ingredients + food.toDayIngredientRow(amountBaseUnits = null))
        // The units follow a moment later and only add chips below the amount field.
        viewModelScope.launch {
            val units = foodRepository.getUnits(food.id)
            if (units.isNotEmpty()) {
                val portionUnitId = food.portionUnitName
                    ?.let { name -> units.firstOrNull { it.name == name }?.id }
                updateIngredients(
                    _state.value.ingredients.map {
                        if (it.foodId == food.id) {
                            it.copy(units = units, selectedUnitId = it.selectedUnitId ?: portionUnitId)
                        } else {
                            it
                        }
                    },
                )
            }
        }
    }

    fun removeIngredient(foodId: String) {
        updateIngredients(_state.value.ingredients.filterNot { it.foodId == foodId })
    }

    /** Gives the entry the library recipe back, dropping the day's own version of it. */
    fun resetIngredientsToRecipe() {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            val quantity = _state.value.quantityBaseUnits ?: entry.quantity
            diaryRepository.resetRecipeEntryToLibrary(entry, quantity, _state.value.mealType)
            _state.value = _state.value.copy(isSaved = true)
        }
    }

    fun save() {
        val s = _state.value
        val entry = s.entry ?: return
        if (!s.isValid) return
        val quantity = if (s.isQuantityEditable) {
            s.quantityBaseUnits ?: return
        } else {
            entry.quantity
        }
        val unit = s.selectedUnit
        val unitCount = unit?.let { s.quantityText.toLocaleDoubleOrNull() }
        viewModelScope.launch {
            // Only write a per-day copy when the user actually changed the recipe (or the entry
            // already had one) — otherwise the entry keeps following the library recipe.
            if (s.isRecipe && (s.hasIngredientChanges || s.hadDayIngredients)) {
                diaryRepository.updateRecipeEntryIngredients(
                    entry = entry,
                    newQuantity = quantity,
                    newMealType = s.mealType,
                    ingredients = s.ingredients.map { row ->
                        DiaryRecipeIngredientDraft(
                            foodId = row.foodId,
                            amountBaseUnits = row.amountBaseUnits ?: 0.0,
                            unitName = row.selectedUnit?.name,
                            unitCount = row.selectedUnit?.let { row.amountText.toLocaleDoubleOrNull() },
                        )
                    },
                )
            } else {
                diaryRepository.updateEntry(entry, quantity, s.mealType, unit?.name, unitCount)
            }
            _state.value = _state.value.copy(isSaved = true)
        }
    }

    private fun updateIngredients(rows: List<DayIngredientRow>) {
        _state.value = _state.value.copy(ingredients = rows, hasIngredientChanges = true)
    }
}

private fun FoodItem.toDayIngredientRow(
    amountBaseUnits: Double?,
    units: List<FoodUnit> = emptyList(),
    unit: FoodUnit? = null,
    unitCount: Double? = null,
) = DayIngredientRow(
    foodId = id,
    foodName = name,
    baseUnit = baseUnit,
    amountText = if (unit != null && unitCount != null) {
        unitCount.formatDecimal(3)
    } else {
        amountBaseUnits?.formatDecimal(3).orEmpty()
    },
    units = units,
    // A food without a weight has one unit and no chips to pick it with, so it is picked here.
    selectedUnitId = unit?.id ?: portionUnitName?.let { name -> units.firstOrNull { it.name == name }?.id },
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
    portionUnitName = portionUnitName,
)
