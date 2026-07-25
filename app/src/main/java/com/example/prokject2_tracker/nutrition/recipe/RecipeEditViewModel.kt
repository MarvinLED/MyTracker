package com.example.prokject2_tracker.nutrition.recipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
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

data class IngredientRow(
    val foodId: String,
    val foodName: String,
    val baseUnit: BaseUnit,
    val amountText: String,
    /**
     * The ingredient's link into the Getränkearten library, copied from the Lebensmittel so the row
     * can show the fluid it brings along while the amount is still being typed.
     */
    val fluidTypeId: String? = null,
    val fluidMlPer100: Double? = null,
) {
    val fluidMl: Double
        get() = fluidMlOf(fluidTypeId, fluidMlPer100, amountText.toDoubleOrNull() ?: 0.0)
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
            servings.toDoubleOrNull()?.let { it > 0.0 } == true &&
            ingredients.isNotEmpty() &&
            ingredients.all { it.amountText.toDoubleOrNull() != null }
}

private fun FoodItem.toIngredientRow(amountText: String) = IngredientRow(
    foodId = id,
    foodName = name,
    baseUnit = baseUnit,
    amountText = amountText,
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
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
                        ingredients = recipeWithNutrition.ingredients.map {
                            it.food.toIngredientRow(amountText = it.ingredient.amountBaseUnits.toString())
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
    }

    fun updateIngredientAmount(foodId: String, amountText: String) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.map {
                if (it.foodId == foodId) it.copy(amountText = amountText) else it
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
                servings = s.servings.toDouble(),
                instructions = s.instructions.ifBlank { null },
                ingredientDrafts = s.ingredients.map {
                    RecipeIngredientDraft(foodId = it.foodId, amountBaseUnits = it.amountText.toDouble())
                },
            )
            _state.value = _state.value.copy(isSaved = true)
        }
    }
}
