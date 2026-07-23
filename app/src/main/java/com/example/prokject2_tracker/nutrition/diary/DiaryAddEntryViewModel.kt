package com.example.prokject2_tracker.nutrition.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.FoodRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    val foodResults: StateFlow<List<FoodItem>> = _query
        .flatMapLatest { q -> if (q.isBlank()) foodRepository.observeAll() else foodRepository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipeResults: StateFlow<List<RecipeWithNutrition>> = _query
        .flatMapLatest { q ->
            recipeRepository.observeAllWithNutrition().map { list ->
                if (q.isBlank()) list else list.filter { it.recipe.name.contains(q, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFood = MutableStateFlow<FoodItem?>(null)
    val selectedFood: StateFlow<FoodItem?> = _selectedFood.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<RecipeWithNutrition?>(null)
    val selectedRecipe: StateFlow<RecipeWithNutrition?> = _selectedRecipe.asStateFlow()

    private val _amountText = MutableStateFlow("")
    val amountText: StateFlow<String> = _amountText.asStateFlow()

    private val _mealType = MutableStateFlow(MealType.BREAKFAST)
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    val isValid: StateFlow<Boolean> =
        combine(_selectedFood, _selectedRecipe, _amountText) { food, recipe, amount ->
            (food != null || recipe != null) && amount.toDoubleOrNull()?.let { it > 0.0 } == true
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun selectSourceType(type: DiarySourceType) {
        _sourceType.value = type
        _selectedFood.value = null
        _selectedRecipe.value = null
        _amountText.value = ""
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun selectFood(food: FoodItem) {
        _selectedFood.value = food
        _amountText.value = food.servingAmount?.toString() ?: ""
    }

    fun selectRecipe(recipe: RecipeWithNutrition) {
        _selectedRecipe.value = recipe
        _amountText.value = "1"
    }

    fun onAmountChange(value: String) { _amountText.value = value }
    fun onMealTypeChange(value: MealType) { _mealType.value = value }

    fun save() {
        val amount = _amountText.value.toDoubleOrNull() ?: return
        val food = _selectedFood.value
        val recipe = _selectedRecipe.value
        viewModelScope.launch {
            when {
                food != null -> diaryRepository.logFood(epochDay, food.id, amount, _mealType.value)
                recipe != null -> diaryRepository.logRecipe(epochDay, recipe.recipe.id, amount, _mealType.value)
                else -> return@launch
            }
            _isSaved.value = true
        }
    }
}
