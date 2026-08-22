package com.example.mytracker.nutrition.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.core.datastore.NutrientGoal
import com.example.mytracker.core.datastore.UserPreferencesRepository
import com.example.mytracker.core.util.DateUtils
import com.example.mytracker.fluid.FluidEntry
import com.example.mytracker.fluid.FluidQuickAdd
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.fluid.FluidType
import com.example.mytracker.nutrition.NutritionTotals
import com.example.mytracker.nutrition.food.Tag
import com.example.mytracker.nutrition.food.TagRepository
import com.example.mytracker.nutrition.recipe.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiaryDayUiState(
    val epochDay: Long,
    val entriesByMeal: Map<MealType, List<DiaryEntry>>,
    val totals: NutritionTotals = NutritionTotals.ZERO,
    /** Only the nutrients with a goal set — the macros without one get no target in their bar. */
    val nutrientGoals: Map<Nutrient, NutrientGoal> = emptyMap(),
    /**
     * The day's drinks, shown here as one bar rather than sending the user to Flüssigkeiten to see
     * whether they are on track. [fluidTypes] comes along because the bar's segments are coloured by
     * the type's position in the library, so the same drink keeps its colour on both screens.
     */
    val fluidEntries: List<FluidEntry> = emptyList(),
    val fluidTypes: List<FluidType> = emptyList(),
    val fluidGoalMl: Double = 2000.0,
    /**
     * The tags behind each logged entry, keyed by what the entry points at. A [DiaryEntry] snapshots
     * only `sourceType`/`sourceId`, so the link has to be re-made here rather than read off the row;
     * a [DiarySourceType.QUICK] entry has no source and is simply absent from the map.
     */
    val tagsBySource: Map<Pair<DiarySourceType, String>, List<Tag>> = emptyMap(),
    /** The full library order, which decides each tag's palette slot — see `Tag.displayColor`. */
    val tagOrder: List<String> = emptyList(),
) {
    val totalKcal: Double get() = totals.kcal

    /** Falls back to a plain 2000 kcal ceiling when the user has never set a calorie goal. */
    val calorieGoal: NutrientGoal get() = nutrientGoals[Nutrient.KCAL] ?: NutrientGoal(max = 2000.0)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val fluidRepository: FluidRepository,
    private val mealClipboard: MealClipboard,
    tagRepository: TagRepository,
    recipeRepository: RecipeRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val _selectedEpochDay = MutableStateFlow(DateUtils.todayEpochDay())
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay.asStateFlow()

    /**
     * Tags for both kinds of source in one flow. Rezepte carry no tags of their own — theirs are
     * derived from their ingredients, which [RecipeRepository.observeAllWithNutrition] has already
     * done. Bundled here rather than passed to the day's `combine` separately, which would push it
     * past the typed overloads.
     */
    private val tagContext: Flow<Pair<Map<Pair<DiarySourceType, String>, List<Tag>>, List<String>>> =
        combine(
            tagRepository.observeAllTags(),
            tagRepository.observeTagsByFoodId(),
            recipeRepository.observeAllWithNutrition(),
        ) { allTags, tagsByFood, recipes ->
            val bySource = buildMap<Pair<DiarySourceType, String>, List<Tag>> {
                tagsByFood.forEach { (foodId, tags) -> put(DiarySourceType.FOOD to foodId, tags) }
                recipes.forEach { recipe ->
                    if (recipe.tags.isNotEmpty()) put(DiarySourceType.RECIPE to recipe.recipe.id, recipe.tags)
                }
            }
            bySource to allTags.map { it.id }
        }

    /** The day's drinks as one flow, so the state below stays inside `combine`'s typed overloads. */
    private val fluidDay: (Long) -> Flow<Pair<List<FluidEntry>, List<FluidType>>> = { epochDay ->
        combine(fluidRepository.observeForDay(epochDay), fluidRepository.observeTypes()) { entries, types ->
            entries to types
        }
    }

    val uiState: StateFlow<DiaryDayUiState> = _selectedEpochDay
        .flatMapLatest { epochDay ->
            combine(
                diaryRepository.observeForDay(epochDay),
                diaryRepository.observeDayNutritionTotals(epochDay),
                userPreferencesRepository.userPreferences,
                fluidDay(epochDay),
                tagContext,
            ) { entries, totals, prefs, fluid, (tagsBySource, tagOrder) ->
                val (fluidEntries, fluidTypes) = fluid
                DiaryDayUiState(
                    epochDay = epochDay,
                    entriesByMeal = entries.groupBy { it.mealType },
                    totals = totals,
                    nutrientGoals = prefs.nutrientGoals,
                    fluidEntries = fluidEntries,
                    fluidTypes = fluidTypes,
                    fluidGoalMl = prefs.dailyWaterGoalMl,
                    tagsBySource = tagsBySource,
                    tagOrder = tagOrder,
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DiaryDayUiState(_selectedEpochDay.value, emptyMap()),
        )

    /** The Schnellauswahl buttons under the fluid bar; empty until the user configures some. */
    val fluidQuickAdds: StateFlow<List<FluidQuickAdd>> = fluidRepository.observeQuickAdds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun goToPreviousDay() {
        _selectedEpochDay.value -= 1
        clearUndo()
    }

    fun goToNextDay() {
        _selectedEpochDay.value += 1
        clearUndo()
    }

    fun goToToday() {
        _selectedEpochDay.value = DateUtils.todayEpochDay()
        clearUndo()
    }

    private fun clearUndo() {
        _undoableDelete.value = null
        _undoableFluidAdd.value = null
    }

    /**
     * The last drink logged from the Schnellauswahl, kept only so the button next to it can take it
     * back. Like [undoableDelete] it is dropped when the day changes — undoing onto a day you are no
     * longer looking at would be the worse surprise.
     */
    private val _undoableFluidAdd = MutableStateFlow<FluidEntry?>(null)
    val undoableFluidAdd: StateFlow<FluidEntry?> = _undoableFluidAdd.asStateFlow()

    fun quickAddFluid(quickAdd: FluidQuickAdd) {
        val epochDay = _selectedEpochDay.value
        viewModelScope.launch {
            _undoableFluidAdd.value = fluidRepository.logQuickAdd(epochDay, quickAdd)
        }
    }

    fun undoFluidAdd() {
        val entry = _undoableFluidAdd.value ?: return
        _undoableFluidAdd.value = null
        viewModelScope.launch { fluidRepository.delete(entry) }
    }

    /**
     * The last deleted entry, kept only so it can be put back. Cleared on undo and whenever the day
     * changes: an undo button that reinstates something onto a day you're no longer looking at would
     * be a worse surprise than losing the undo.
     */
    private val _undoableDelete = MutableStateFlow<DiaryEntrySnapshot?>(null)
    val undoableDelete: StateFlow<DiaryEntrySnapshot?> = _undoableDelete.asStateFlow()

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch {
            // Read the per-day recipe copy before deleting, since the delete cascades it away.
            val dayIngredients = diaryRepository.getRecipeIngredientDrafts(entry.id)
            diaryRepository.delete(entry)
            _undoableDelete.value = DiaryEntrySnapshot(entry, dayIngredients)
        }
    }

    fun undoDelete() {
        val deleted = _undoableDelete.value ?: return
        _undoableDelete.value = null
        viewModelScope.launch { diaryRepository.restore(deleted.entry, deleted.dayIngredients) }
    }

    fun dismissUndo() {
        _undoableDelete.value = null
    }

    /**
     * The Tageszeit on the clipboard, if any. Survives switching days and leaving the Tagebuch —
     * copying is only useful because the paste happens somewhere else (see [MealClipboard]).
     */
    val copiedMeal: StateFlow<CopiedMeal?> = mealClipboard.copied

    /** Long press on a Tageszeit: takes everything logged under it on the selected day. */
    fun copyMeal(mealType: MealType) {
        val epochDay = _selectedEpochDay.value
        viewModelScope.launch {
            val snapshots = diaryRepository.getMealSnapshots(epochDay, mealType)
            if (snapshots.isNotEmpty()) {
                mealClipboard.put(CopiedMeal(epochDay = epochDay, mealType = mealType, entries = snapshots))
            }
        }
    }

    /** Writes the clipboard into [mealType] of the day on screen. The copy stays, so it can go to several days. */
    fun pasteInto(mealType: MealType) {
        val copied = mealClipboard.copied.value ?: return
        val epochDay = _selectedEpochDay.value
        viewModelScope.launch {
            diaryRepository.copyEntriesTo(copied.entries, epochDay, mealType)
        }
    }

    fun clearClipboard() {
        mealClipboard.clear()
    }
}
