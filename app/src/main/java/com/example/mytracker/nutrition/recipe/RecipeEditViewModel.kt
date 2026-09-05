package com.example.mytracker.nutrition.recipe

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

data class IngredientRow(
    val foodId: String,
    val foodName: String,
    val baseUnit: BaseUnit,
    /** The number as typed — grams when [selectedUnitId] is null, otherwise a count of that unit. */
    val amountText: String,
    /** The food's named units, so the row can offer "2 × Scheibe" instead of grams. */
    val units: List<FoodUnit> = emptyList(),
    val selectedUnitId: String? = null,
    /**
     * The ingredient's link into the Getränkearten library, copied from the Lebensmittel so the row
     * can show the fluid it brings along while the amount is still being typed.
     */
    val fluidTypeId: String? = null,
    val fluidMlPer100: Double? = null,
    /** Non-null for an ingredient without a weight; see [FoodItem.portionUnitName]. */
    val portionUnitName: String? = null,
) {
    val selectedUnit: FoodUnit?
        get() = units.firstOrNull { it.id == selectedUnitId }

    /** What the row actually contributes, whichever way it was typed. Null while it isn't a number. */
    val amountBaseUnits: Double?
        get() = amountInBaseUnits(amountText, selectedUnit, portionUnitName)

    val fluidMl: Double
        get() = fluidMlOf(fluidTypeId, fluidMlPer100, amountBaseUnits ?: 0.0)
}

data class RecipeEditState(
    val id: String? = null,
    val name: String = "",
    val servings: String = "1",
    val instructions: String = "",
    val ingredients: List<IngredientRow> = emptyList(),
    val isSaved: Boolean = false,
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
            servings.toLocaleDoubleOrNull()?.let { it > 0.0 } == true &&
            ingredients.isNotEmpty() &&
            ingredients.all { it.amountBaseUnits != null }
}

private fun FoodItem.toIngredientRow(
    amountText: String,
    units: List<FoodUnit> = emptyList(),
    selectedUnitId: String? = null,
) = IngredientRow(
    foodId = id,
    foodName = name,
    baseUnit = baseUnit,
    amountText = amountText,
    units = units,
    // A food without a weight has one unit and no chips to pick it with, so it is picked here.
    selectedUnitId = selectedUnitId
        ?: portionUnitName?.let { name -> units.firstOrNull { it.name == name }?.id },
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
    portionUnitName = portionUnitName,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val foodRepository: FoodRepository,
    fluidRepository: FluidRepository,
) : ViewModel() {
    private val route: RecipeEditRoute = savedStateHandle.toRoute()
    private var existing: Recipe? = null

    private val _state = MutableStateFlow(RecipeEditState(id = route.recipeId))
    val state: StateFlow<RecipeEditState> = _state.asStateFlow()

    private val _pickerQuery = MutableStateFlow("")
    val pickerQuery: StateFlow<String> = _pickerQuery.asStateFlow()

    val pickerResults: StateFlow<List<FoodItem>> = _pickerQuery
        .flatMapLatest { q -> if (q.isBlank()) foodRepository.observeAll() else foodRepository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Names for the ingredients' [IngredientRow.fluidTypeId]s, so rows can label the fluid they add. */
    val fluidTypeNames: StateFlow<Map<String, String>> = fluidRepository.observeTypes()
        .map { types -> types.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        val recipeId = route.recipeId
        if (recipeId != null) {
            viewModelScope.launch {
                recipeRepository.getWithNutrition(recipeId)?.let { recipeWithNutrition ->
                    existing = recipeWithNutrition.recipe
                    _state.value = RecipeEditState(
                        id = recipeWithNutrition.recipe.id,
                        name = recipeWithNutrition.recipe.name,
                        servings = recipeWithNutrition.recipe.servings.toString(),
                        instructions = recipeWithNutrition.recipe.instructions.orEmpty(),
                        ingredients = recipeWithNutrition.ingredients.map { withFood ->
                            val units = foodRepository.getUnits(withFood.food.id)
                            // The unit was snapshotted by name; match it back to a live one so the
                            // row reopens in the mode it was saved in. A since-renamed or deleted
                            // unit falls back to plain base units — the amount is the same either way.
                            val unit = withFood.ingredient.unitName?.let { name ->
                                units.firstOrNull { it.name == name }
                            }
                            withFood.food.toIngredientRow(
                                amountText = if (unit != null && withFood.ingredient.unitCount != null) {
                                    withFood.ingredient.unitCount.formatDecimal(3)
                                } else {
                                    withFood.ingredient.amountBaseUnits.formatDecimal(3)
                                },
                                units = units,
                                selectedUnitId = unit?.id,
                            )
                        },
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onServingsChange(value: String) { _state.value = _state.value.copy(servings = value) }
    fun onInstructionsChange(value: String) { _state.value = _state.value.copy(instructions = value) }
    fun onPickerQueryChange(value: String) { _pickerQuery.value = value }

    fun addIngredient(food: FoodItem) {
        if (_state.value.ingredients.any { it.foodId == food.id }) return
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients + food.toIngredientRow(amountText = ""),
        )
        // The row is added immediately (the screen focuses its amount field right away); the food's
        // units arrive a moment later and only add chips below the field.
        viewModelScope.launch {
            val units = foodRepository.getUnits(food.id)
            if (units.isNotEmpty()) {
                val portionUnitId = food.portionUnitName
                    ?.let { name -> units.firstOrNull { it.name == name }?.id }
                updateIngredient(food.id) {
                    it.copy(units = units, selectedUnitId = it.selectedUnitId ?: portionUnitId)
                }
            }
        }
    }

    fun updateIngredientAmount(foodId: String, amountText: String) {
        updateIngredient(foodId) { it.copy(amountText = amountText) }
    }

    /**
     * Switches a row between base units (null) and one of the food's named units, prefilling the
     * usual amount for the new mode. Re-tapping the selected chip does nothing.
     */
    fun selectIngredientUnit(foodId: String, unitId: String?) {
        updateIngredient(foodId) { row ->
            if (unitId == row.selectedUnitId) {
                row
            } else {
                row.copy(
                    selectedUnitId = unitId,
                    amountText = defaultAmountText(row.units.firstOrNull { it.id == unitId }),
                )
            }
        }
    }

    private fun updateIngredient(foodId: String, transform: (IngredientRow) -> IngredientRow) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.map {
                if (it.foodId == foodId) transform(it) else it
            },
        )
    }

    fun removeIngredient(foodId: String) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.filterNot { it.foodId == foodId },
        )
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            recipeRepository.saveRecipe(
                existing = existing,
                name = s.name,
                servings = s.servings.toLocaleDoubleOrNull() ?: return@launch,
                instructions = s.instructions.ifBlank { null },
                ingredientDrafts = s.ingredients.map { row ->
                    RecipeIngredientDraft(
                        foodId = row.foodId,
                        amountBaseUnits = row.amountBaseUnits ?: 0.0,
                        unitName = row.selectedUnit?.name,
                        unitCount = row.selectedUnit?.let { row.amountText.toLocaleDoubleOrNull() },
                    )
                },
            )
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
