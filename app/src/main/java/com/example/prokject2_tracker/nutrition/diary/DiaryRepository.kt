package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.fluid.FluidContribution
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.nutrition.FoodAmount
import com.example.prokject2_tracker.nutrition.NutritionMath
import com.example.prokject2_tracker.nutrition.NutritionTotals
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodDao
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.fluidMl
import com.example.prokject2_tracker.nutrition.recipe.RecipeDao
import com.example.prokject2_tracker.nutrition.recipe.foodAmounts
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** One ingredient row of a per-day recipe copy as edited in the UI, before ids are assigned. */
data class DiaryRecipeIngredientDraft(
    val foodId: String,
    val amountBaseUnits: Double,
)

/**
 * The ingredient list a recipe entry is computed from, plus the servings it divides by: either the
 * library recipe or the entry's own per-day copy of it. Both paths produce an entry the same way,
 * so the nutrition snapshot and the mirrored fluid can't drift apart between them.
 */
private data class RecipeSource(
    val recipeId: String,
    val recipeName: String,
    val servings: Double,
    val ingredients: List<FoodAmount>,
)

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val foodDao: FoodDao,
    private val recipeDao: RecipeDao,
    private val fluidRepository: FluidRepository,
) {
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>> = diaryDao.observeForDay(epochDay)

    fun observeDayTotalKcal(epochDay: Long): Flow<Double> = diaryDao.observeDayTotalKcal(epochDay)

    fun observeDayNutritionTotals(epochDay: Long): Flow<NutritionTotals> =
        diaryDao.observeDayNutritionTotals(epochDay)

    fun observeDailyKcalTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyKcalTotal>> =
        diaryDao.observeDailyKcalTotals(startInclusive, endInclusive)

    suspend fun getEntry(id: String): DiaryEntry? = diaryDao.getById(id)

    /** True when the user has already given this entry its own version of the recipe. */
    suspend fun hasRecipeDayIngredients(diaryEntryId: String): Boolean =
        diaryDao.getRecipeIngredients(diaryEntryId).isNotEmpty()

    /**
     * The ingredients a recipe entry currently follows — its own per-day copy if it has one, else the
     * library recipe as it stands now. Empty for food and Schnelleintrag entries, and for a recipe
     * that has since been deleted from the library without this entry ever getting its own copy.
     */
    suspend fun getRecipeIngredientsInEffect(entry: DiaryEntry): List<FoodAmount> =
        if (entry.sourceType == DiarySourceType.RECIPE) {
            recipeSourceFor(entry)?.ingredients.orEmpty()
        } else {
            emptyList()
        }

    suspend fun logFood(epochDay: Long, foodId: String, amountBaseUnits: Double, mealType: MealType) {
        val food = requireNotNull(foodDao.getById(foodId)) { "Food $foodId not found" }
        val entry = foodEntry(IdGenerator.newId(), epochDay, Instant.now(), food, amountBaseUnits, mealType)
        diaryDao.upsert(entry)
        syncFluidForFoodEntry(entry, food)
    }

    suspend fun logRecipe(epochDay: Long, recipeId: String, servingsConsumed: Double, mealType: MealType) {
        val source = requireNotNull(libraryRecipeSource(recipeId)) { "Recipe $recipeId not found" }
        val entry = source.toEntry(IdGenerator.newId(), epochDay, Instant.now(), servingsConsumed, mealType)
        diaryDao.upsert(entry)
        syncFluidForRecipeEntry(entry, source)
    }

    /**
     * Logs a one-off Schnelleintrag: no library item behind it, kcal is the only required value and
     * the macros are whatever the user bothered to type — all already totals, never per 100 g.
     */
    suspend fun logQuick(
        epochDay: Long,
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        mealType: MealType,
    ) {
        diaryDao.upsert(
            DiaryEntry(
                id = IdGenerator.newId(),
                epochDay = epochDay,
                createdAt = Instant.now(),
                mealType = mealType,
                sourceType = DiarySourceType.QUICK,
                sourceId = "",
                sourceName = name,
                quantity = 1.0,
                quantityUnit = "Schnelleintrag",
                kcal = kcal,
                protein = protein,
                carbs = carbs,
                fat = fat,
            ),
        )
    }

    /**
     * Re-derives the nutrition snapshot for a changed amount/meal, from the entry's per-day recipe
     * copy if it has one and from the source's *current* state otherwise. Does not touch other rows,
     * and keeps [DiaryEntry.createdAt] so correcting an amount doesn't reshuffle the day's order.
     */
    suspend fun updateEntry(entry: DiaryEntry, newQuantity: Double, newMealType: MealType) {
        when (entry.sourceType) {
            DiarySourceType.FOOD -> {
                val food = foodDao.getById(entry.sourceId)
                if (food == null) {
                    diaryDao.upsert(entry.scaledTo(newQuantity, newMealType))
                } else {
                    val updated = foodEntry(entry.id, entry.epochDay, entry.createdAt, food, newQuantity, newMealType)
                    diaryDao.upsert(updated)
                    syncFluidForFoodEntry(updated, food)
                }
            }
            DiarySourceType.RECIPE -> {
                val source = recipeSourceFor(entry)
                if (source == null) {
                    diaryDao.upsert(entry.scaledTo(newQuantity, newMealType))
                } else {
                    saveRecipeEntry(entry, source, newQuantity, newMealType, asDayIngredients = false)
                }
            }
            // A Schnelleintrag has no source to re-derive from — its snapshot *is* the entry.
            DiarySourceType.QUICK -> diaryDao.upsert(entry.copy(mealType = newMealType))
        }
    }

    /**
     * Records "I made the Rezept differently today": [ingredients] becomes this entry's own copy of
     * the recipe, and its nutrition and mirrored fluid follow that copy from now on. The library
     * recipe and every other day's entry stay untouched.
     */
    suspend fun updateRecipeEntryIngredients(
        entry: DiaryEntry,
        newQuantity: Double,
        newMealType: MealType,
        ingredients: List<DiaryRecipeIngredientDraft>,
    ) {
        val foodsById = foodDao.getByIds(ingredients.map { it.foodId }).associateBy { it.id }
        val source = RecipeSource(
            recipeId = entry.sourceId,
            recipeName = entry.sourceName,
            servings = servingsOf(entry),
            ingredients = ingredients.mapNotNull { draft ->
                foodsById[draft.foodId]?.let { FoodAmount(it, draft.amountBaseUnits) }
            },
        )
        saveRecipeEntry(entry, source, newQuantity, newMealType, asDayIngredients = true)
    }

    /**
     * Drops the per-day copy so the entry follows the library recipe again. Keeps the copy if that
     * recipe no longer exists — there would be nothing left to fall back to.
     */
    suspend fun resetRecipeEntryToLibrary(entry: DiaryEntry, newQuantity: Double, newMealType: MealType) {
        val source = libraryRecipeSource(entry.sourceId)
        if (source == null) {
            updateEntry(entry, newQuantity, newMealType)
            return
        }
        val updated = source.toEntry(entry.id, entry.epochDay, entry.createdAt, newQuantity, newMealType)
        diaryDao.upsertWithRecipeIngredients(updated, emptyList())
        syncFluidForRecipeEntry(updated, source)
    }

    suspend fun delete(entry: DiaryEntry) {
        // The per-day recipe copy goes with it via the entity's CASCADE foreign key.
        diaryDao.delete(entry)
        fluidRepository.deleteForDiaryEntry(entry.id)
    }

    /** The entry's per-day recipe copy in the shape [restore] wants it back in. */
    suspend fun getRecipeIngredientDrafts(diaryEntryId: String): List<DiaryRecipeIngredientDraft> =
        diaryDao.getRecipeIngredients(diaryEntryId).map {
            DiaryRecipeIngredientDraft(foodId = it.food.id, amountBaseUnits = it.ingredient.amountBaseUnits)
        }

    /**
     * Puts a deleted entry back exactly as it was, snapshot and all — an undo, not a re-log, so the
     * nutrition is *not* re-derived from a source that may meanwhile have changed. Its per-day recipe
     * copy and mirrored fluid are rebuilt with it.
     */
    suspend fun restore(entry: DiaryEntry, dayIngredients: List<DiaryRecipeIngredientDraft>) {
        diaryDao.upsertWithRecipeIngredients(
            entry,
            dayIngredients.mapIndexed { index, draft ->
                DiaryRecipeIngredient(
                    id = IdGenerator.newId(),
                    diaryEntryId = entry.id,
                    foodId = draft.foodId,
                    amountBaseUnits = draft.amountBaseUnits,
                    sortOrder = index,
                )
            },
        )
        when (entry.sourceType) {
            DiarySourceType.FOOD -> foodDao.getById(entry.sourceId)?.let { syncFluidForFoodEntry(entry, it) }
            DiarySourceType.RECIPE -> recipeSourceFor(entry)?.let { syncFluidForRecipeEntry(entry, it) }
            DiarySourceType.QUICK -> Unit
        }
    }

    /**
     * Writes [source] as [entry]'s new state. [asDayIngredients] true stores [source]'s ingredients
     * as the entry's own per-day copy of the recipe; false leaves whatever copy (or none) it has.
     */
    private suspend fun saveRecipeEntry(
        entry: DiaryEntry,
        source: RecipeSource,
        newQuantity: Double,
        newMealType: MealType,
        asDayIngredients: Boolean,
    ) {
        val updated = source.toEntry(entry.id, entry.epochDay, entry.createdAt, newQuantity, newMealType)
        if (asDayIngredients) {
            diaryDao.upsertWithRecipeIngredients(
                updated,
                source.ingredients.mapIndexed { index, item ->
                    DiaryRecipeIngredient(
                        id = IdGenerator.newId(),
                        diaryEntryId = updated.id,
                        foodId = item.food.id,
                        amountBaseUnits = item.amountBaseUnits,
                        sortOrder = index,
                    )
                },
            )
        } else {
            diaryDao.upsert(updated)
        }
        syncFluidForRecipeEntry(updated, source)
    }

    /** Null when the recipe has since been deleted from the library. */
    private suspend fun libraryRecipeSource(recipeId: String): RecipeSource? {
        val withIngredients = recipeDao.getWithIngredients(recipeId) ?: return null
        return RecipeSource(
            recipeId = withIngredients.recipe.id,
            recipeName = withIngredients.recipe.name,
            servings = withIngredients.recipe.servings,
            ingredients = withIngredients.ingredients.foodAmounts(),
        )
    }

    /** The entry's own per-day copy of the recipe if it has one, otherwise the library recipe. */
    private suspend fun recipeSourceFor(entry: DiaryEntry): RecipeSource? {
        val dayIngredients = diaryDao.getRecipeIngredients(entry.id)
        if (dayIngredients.isEmpty()) return libraryRecipeSource(entry.sourceId)
        return RecipeSource(
            recipeId = entry.sourceId,
            recipeName = entry.sourceName,
            servings = servingsOf(entry),
            ingredients = dayIngredients.foodAmounts(),
        )
    }

    /**
     * The fallback for an entry whose source is gone from the library (and that has no per-day recipe
     * copy either): the snapshot is the only nutrition left, so re-scale it to the new amount rather
     * than refuse the edit. Any mirrored fluid stays as it was, for the same reason — there is
     * nothing left to re-derive it from.
     */
    private fun DiaryEntry.scaledTo(newQuantity: Double, newMealType: MealType): DiaryEntry {
        val factor = if (quantity > 0.0) newQuantity / quantity else 0.0
        return copy(
            mealType = newMealType,
            quantity = newQuantity,
            kcal = kcal * factor,
            protein = protein * factor,
            carbs = carbs * factor,
            fat = fat * factor,
            saturatedFat = saturatedFat * factor,
            sugar = sugar * factor,
            fiber = fiber * factor,
            salt = salt * factor,
        )
    }

    /**
     * The servings a recipe entry divides its ingredients by. Entries logged before this was
     * snapshotted fall back to the library recipe's current value, and to 1 if the recipe is gone —
     * "the whole pot is one portion" is the only reading left, and it beats dividing by zero.
     */
    private suspend fun servingsOf(entry: DiaryEntry): Double =
        entry.recipeServings ?: recipeDao.getById(entry.sourceId)?.servings ?: 1.0

    private fun RecipeSource.toEntry(
        id: String,
        epochDay: Long,
        createdAt: Instant,
        servingsConsumed: Double,
        mealType: MealType,
    ): DiaryEntry {
        val totals = NutritionMath.perServing(NutritionMath.total(ingredients), servings) * servingsConsumed
        return DiaryEntry(
            id = id,
            epochDay = epochDay,
            createdAt = createdAt,
            mealType = mealType,
            sourceType = DiarySourceType.RECIPE,
            sourceId = recipeId,
            sourceName = recipeName,
            quantity = servingsConsumed,
            quantityUnit = "Portion(en)",
            kcal = totals.kcal,
            protein = totals.protein,
            carbs = totals.carbs,
            fat = totals.fat,
            saturatedFat = totals.saturatedFat,
            sugar = totals.sugar,
            fiber = totals.fiber,
            salt = totals.salt,
            recipeServings = servings,
        )
    }

    private fun foodEntry(
        id: String,
        epochDay: Long,
        createdAt: Instant,
        food: FoodItem,
        amountBaseUnits: Double,
        mealType: MealType,
    ): DiaryEntry {
        val totals = NutritionMath.forFoodAmount(food, amountBaseUnits)
        return DiaryEntry(
            id = id,
            epochDay = epochDay,
            createdAt = createdAt,
            mealType = mealType,
            sourceType = DiarySourceType.FOOD,
            sourceId = food.id,
            sourceName = food.name,
            quantity = amountBaseUnits,
            quantityUnit = if (food.baseUnit == BaseUnit.G) "g" else "ml",
            kcal = totals.kcal,
            protein = totals.protein,
            carbs = totals.carbs,
            fat = totals.fat,
            saturatedFat = totals.saturatedFat,
            sugar = totals.sugar,
            fiber = totals.fiber,
            salt = totals.salt,
        )
    }

    /**
     * Mirrors the fluid a drink-like Lebensmittel contributes into the Flüssigkeiten log. Foods
     * without a [FoodItem.fluidTypeId] clear any previously mirrored row instead, so unlinking a
     * food and re-saving an entry doesn't leave a stale drink behind.
     */
    private suspend fun syncFluidForFoodEntry(entry: DiaryEntry, food: FoodItem) {
        fluidRepository.syncFromDiaryEntry(
            diaryEntryId = entry.id,
            epochDay = entry.epochDay,
            contributions = listOfNotNull(
                food.fluidTypeId?.let { FluidContribution(it, food.fluidMl(entry.quantity)) },
            ),
        )
    }

    /**
     * Same mirroring for a Rezept, but summed over its drink-linked ingredients and scaled from the
     * whole recipe down to the portions actually eaten.
     */
    private suspend fun syncFluidForRecipeEntry(entry: DiaryEntry, source: RecipeSource) {
        val portionFactor = if (source.servings > 0.0) entry.quantity / source.servings else 0.0
        fluidRepository.syncFromDiaryEntry(
            diaryEntryId = entry.id,
            epochDay = entry.epochDay,
            contributions = source.ingredients.mapNotNull { item ->
                item.food.fluidTypeId?.let {
                    FluidContribution(it, item.food.fluidMl(item.amountBaseUnits) * portionFactor)
                }
            },
        )
    }
}
