package com.example.mytracker.nutrition.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.nutrition.diary.DiaryPickerItem
import com.example.mytracker.nutrition.diary.DiaryPickerMode
import com.example.mytracker.nutrition.diary.DiaryPickerSort
import com.example.mytracker.nutrition.diary.DiaryRepository
import com.example.mytracker.nutrition.diary.DiarySourceType
import com.example.mytracker.nutrition.diary.LastLoggedSource
import com.example.mytracker.nutrition.diary.MealType
import com.example.mytracker.nutrition.diary.defaultMealType
import com.example.mytracker.nutrition.diary.filteredForPicker
import com.example.mytracker.nutrition.diary.next
import com.example.mytracker.nutrition.diary.sortedForPicker
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.FoodRepository
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagImplication
import com.example.mytracker.nutrition.food.TagRepository
import com.example.mytracker.nutrition.recipe.Recipe
import com.example.mytracker.nutrition.recipe.RecipeRepository
import com.example.mytracker.nutrition.recipe.RecipeWithNutrition
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The one list behind the Bibliothek's Lebensmittel and Rezepte tabs.
 *
 * Search, sort and tag filter live here rather than in the two tabs, for two reasons: they are the
 * same choice in both, so switching tabs must not silently change what is filtered out; and the
 * sorting needs the diary's last-logged and log-count maps, which are one query each no matter how
 * many tabs read them.
 *
 * The ordering itself is not written here — [filteredForPicker] and [sortedForPicker] are pure
 * functions with their own tests, and this class only feeds them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val recipeRepository: RecipeRepository,
    tagRepository: TagRepository,
    diaryRepository: DiaryRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _sort = MutableStateFlow(DiaryPickerSort.LAST_EATEN)
    val sort: StateFlow<DiaryPickerSort> = _sort.asStateFlow()

    private val _selectedTagId = MutableStateFlow<String?>(null)
    val selectedTagId: StateFlow<String?> = _selectedTagId.asStateFlow()

    /**
     * The meal the Zuletzt sort groups by: what was last eaten at *this* meal floats to the top.
     * Follows the clock until the screen says otherwise — it is handed the meal of the "+" the user
     * came through.
     */
    private val _mealType = MutableStateFlow(defaultMealType(LocalTime.now()))

    val allTags: StateFlow<List<Tag>> = tagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Widens the tag filter: picking "vegetarisch" also keeps the vegan-only items. */
    private val tagImplications: StateFlow<List<TagImplication>> = tagRepository.observeAllImplications()
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

    /** How often each source has been logged, for the Am-meisten sort. */
    private val logCounts: StateFlow<Map<Pair<DiarySourceType, String>, Int>> =
        diaryRepository.observeLogCountPerSource()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val unifiedItems: StateFlow<List<DiaryPickerItem>> =
        combine(foodResults, recipeResults, tagsByFoodId) { foods, recipes, tagsByFood ->
            foods.map { DiaryPickerItem.Food(it, tagsByFood[it.id].orEmpty()) } +
                recipes.map { DiaryPickerItem.Recipe(it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filteredItems: StateFlow<List<DiaryPickerItem>> =
        combine(unifiedItems, _selectedTagId, tagImplications) { items, tagId, implications ->
            items.filteredForPicker(DiaryPickerMode.ALL, tagId, implications)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Everything in order, both kinds together. The two tabs then take their half out of it — split
     * *after* sorting, so both are in the one order the sort button names.
     */
    private val sortedItems: StateFlow<List<DiaryPickerItem>> =
        combine(filteredItems, _sort, _mealType, lastLogged, logCounts) { items, sort, meal, lastLog, counts ->
            items.sortedForPicker(sort, meal, lastLog, counts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodItems: StateFlow<List<DiaryPickerItem.Food>> = sortedItems
        .map { items -> items.filterIsInstance<DiaryPickerItem.Food>() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipeItems: StateFlow<List<DiaryPickerItem.Recipe>> = sortedItems
        .map { items -> items.filterIsInstance<DiaryPickerItem.Recipe>() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(value: String) { _query.value = value }

    fun onSortChange(value: DiaryPickerSort) { _sort.value = value }

    /** Short press on the sort button. */
    fun cycleSort() { _sort.value = _sort.value.next() }

    fun onTagSelected(tagId: String?) { _selectedTagId.value = tagId }

    /** Short press on the tag button: Alle → each tag in turn → Alle. */
    fun cycleTag() { _selectedTagId.value = nextTagId(_selectedTagId.value, allTags.value) }

    fun setMealContext(mealType: MealType) { _mealType.value = mealType }

    fun deleteFoodIfUnused(food: FoodItem, onBlocked: () -> Unit) {
        viewModelScope.launch {
            if (foodRepository.canDelete(food.id)) foodRepository.delete(food) else onBlocked()
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch { recipeRepository.delete(recipe) }
    }
}
