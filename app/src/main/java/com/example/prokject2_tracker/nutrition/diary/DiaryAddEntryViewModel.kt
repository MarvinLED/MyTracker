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
import com.example.prokject2_tracker.nutrition.food.Tag
import com.example.prokject2_tracker.nutrition.food.TagRepository
import com.example.prokject2_tracker.nutrition.food.amountInBaseUnits
import com.example.prokject2_tracker.nutrition.food.defaultAmountText
import com.example.prokject2_tracker.nutrition.recipe.RecipeRepository
import com.example.prokject2_tracker.nutrition.recipe.RecipeWithNutrition
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val foodRepository: FoodRepository,
    recipeRepository: RecipeRepository,
    private val tagRepository: TagRepository,
) : ViewModel() {
    private val route: DiaryAddEntryRoute = savedStateHandle.toRoute()
    val epochDay: Long = route.epochDay

    private val _mode = MutableStateFlow(DiaryPickerMode.ALL)
    val mode: StateFlow<DiaryPickerMode> = _mode.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(DiaryPickerSort.LAST_EATEN)
    val sort: StateFlow<DiaryPickerSort> = _sort.asStateFlow()

    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId.asStateFlow()

    private val _expandedTagIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedTagIds: StateFlow<Set<String>> = _expandedTagIds.asStateFlow()

    private val _mealType = MutableStateFlow(route.mealType)
    val mealType: StateFlow<MealType> = _mealType.asStateFlow()

    val allTags: StateFlow<List<Tag>> = tagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val foodResults: StateFlow<List<FoodItem>> = _query
        .flatMapLatest { q -> if (q.isBlank()) foodRepository.observeAll() else foodRepository.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recipeResults: StateFlow<List<RecipeWithNutrition>> = _query
        .flatMapLatest { q ->
            recipeRepository.observeAllWithNutrition().map { list ->
                if (q.isBlank()) list else list.filter { it.recipe.name.contains(q, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tagsByFoodId: StateFlow<Map<String, List<Tag>>> = tagRepository.observeTagsByFoodId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val lastLogged: StateFlow<Map<Pair<DiarySourceType, String>, LastLoggedSource>> =
        diaryRepository.observeLastLoggedPerSource()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val unifiedItems: StateFlow<List<DiaryPickerItem>> =
        combine(foodResults, recipeResults, tagsByFoodId) { foods, recipes, tagsByFood ->
            val foodItems = foods.map { food ->
                DiaryPickerItem.Food(food, tagsByFood[food.id].orEmpty())
            }
            val recipeItems = recipes.map { recipe ->
                DiaryPickerItem.Recipe(recipe)
            }
            foodItems + recipeItems
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filteredItems: StateFlow<List<DiaryPickerItem>> =
        combine(unifiedItems, _mode, _selectedTagId, _expandedTagIds) { items, mode, tagId, expanded ->
            items.filteredForPicker(mode, tagId, expanded)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pickerItems: StateFlow<List<DiaryPickerItem>> =
        combine(filteredItems, _sort, _mealType, lastLogged) { items, sort, mealType, lastLog ->
            items.sortedForPicker(sort, mealType, lastLog)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedItem = MutableStateFlow<DiaryPickerItem?>(null)
    val expandedItem: StateFlow<DiaryPickerItem?> = _expandedItem.asStateFlow()

    private val _amountText = MutableStateFlow("")
    val amountText: StateFlow<String> = _amountText.asStateFlow()

    /** The expanded food's named units, empty for recipes and Schnelleinträge. */
    val foodUnits: StateFlow<List<FoodUnit>> = _expandedItem
        .flatMapLatest { item ->
            (item as? DiaryPickerItem.Food)?.let { foodRepository.observeUnits(it.food.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** null = the amount is being typed in the food's base unit (g/ml). */
    private val _selectedUnitId = MutableStateFlow<String?>(null)
    val selectedUnitId: StateFlow<String?> = _selectedUnitId.asStateFlow()

    private val selectedUnit: FoodUnit?
        get() = foodUnits.value.firstOrNull { it.id == _selectedUnitId.value }

    private val _quick = MutableStateFlow(QuickEntryState())
    val quick: StateFlow<QuickEntryState> = _quick.asStateFlow()

    private val _addedConfirmation = MutableSharedFlow<String>()
    val addedConfirmation = _addedConfirmation.asSharedFlow()

    fun onModeChange(mode: DiaryPickerMode) {
        _mode.value = mode
        _query.value = ""
        _selectedTagId.value = null
        _expandedItem.value = null
        _amountText.value = ""
        _selectedUnitId.value = null
    }

    fun onQueryChange(value: String) { _query.value = value }

    fun onSortChange(sort: DiaryPickerSort) { _sort.value = sort }

    fun onTagSelected(tagId: String?) {
        _selectedTagId.value = tagId
        if (tagId != null) {
            viewModelScope.launch {
                val expanded = tagRepository.expandTagsWithParents(setOf(tagId))
                _expandedTagIds.value = expanded
            }
        } else {
            _expandedTagIds.value = emptySet()
        }
    }

    fun onRowTapped(item: DiaryPickerItem) {
        if (_expandedItem.value?.id == item.id && _expandedItem.value?.sourceType == item.sourceType) {
            _expandedItem.value = null
            _amountText.value = ""
            _selectedUnitId.value = null
        } else {
            _expandedItem.value = item
            when (item) {
                is DiaryPickerItem.Food -> {
                    viewModelScope.launch {
                        val lastLogged = diaryRepository.getLastLoggedAmount(DiarySourceType.FOOD, item.food.id)
                        if (lastLogged != null) {
                            _amountText.value = lastLogged.unitCount?.let { it.formatDecimal(1) } ?: lastLogged.quantity.formatDecimal(1)
                            _selectedUnitId.value = if (lastLogged.unitName != null) {
                                val units = foodRepository.getUnits(item.food.id)
                                units.firstOrNull { it.name == lastLogged.unitName }?.id
                            } else {
                                null
                            }
                        } else {
                            _amountText.value = defaultAmountText(null)
                            _selectedUnitId.value = null
                        }
                    }
                }
                is DiaryPickerItem.Recipe -> {
                    _amountText.value = "1"
                    _selectedUnitId.value = null
                }
            }
        }
    }

    fun selectUnit(unitId: String?) {
        if (unitId == _selectedUnitId.value) return
        _selectedUnitId.value = unitId
        _amountText.value = defaultAmountText(selectedUnit)
    }

    fun adjustAmount(delta: Double) {
        val current = _amountText.value.toLocaleDoubleOrNull() ?: 0.0
        _amountText.value = (current + delta).coerceAtLeast(0.0).formatDecimal(3)
    }

    fun onAmountChange(value: String) { _amountText.value = value }
    fun onMealTypeChange(value: MealType) { _mealType.value = value }

    fun onQuickNameChange(value: String) { _quick.value = _quick.value.copy(name = value) }
    fun onQuickKcalChange(value: String) { _quick.value = _quick.value.copy(kcal = value) }
    fun onQuickProteinChange(value: String) { _quick.value = _quick.value.copy(protein = value) }
    fun onQuickCarbsChange(value: String) { _quick.value = _quick.value.copy(carbs = value) }
    fun onQuickFatChange(value: String) { _quick.value = _quick.value.copy(fat = value) }

    fun confirmAdd() {
        val item = _expandedItem.value ?: return
        val typed = _amountText.value.toLocaleDoubleOrNull() ?: return
        val unit = selectedUnit

        when (item) {
            is DiaryPickerItem.Food -> {
                val amount = amountInBaseUnits(_amountText.value, unit) ?: return
                viewModelScope.launch {
                    diaryRepository.logFood(
                        epochDay = epochDay,
                        foodId = item.food.id,
                        amountBaseUnits = amount,
                        mealType = _mealType.value,
                        unitName = unit?.name,
                        unitCount = unit?.let { typed },
                    )
                    _addedConfirmation.emit(item.food.name)
                    resetExpansionState()
                }
            }
            is DiaryPickerItem.Recipe -> {
                viewModelScope.launch {
                    diaryRepository.logRecipe(epochDay, item.recipe.recipe.id, typed, _mealType.value)
                    _addedConfirmation.emit(item.recipe.recipe.name)
                    resetExpansionState()
                }
            }
        }
    }

    fun confirmQuick() {
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
            _addedConfirmation.emit(q.name.ifBlank { "Schnelleintrag" })
            _quick.value = QuickEntryState()
        }
    }

    private fun resetExpansionState() {
        _expandedItem.value = null
        _amountText.value = ""
        _selectedUnitId.value = null
    }
}
