package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.nutrition.NutritionMath
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodDao
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.recipe.RecipeDao
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DiaryRepository @Inject constructor(
    private val diaryDao: DiaryDao,
    private val foodDao: FoodDao,
    private val recipeDao: RecipeDao,
    private val fluidRepository: FluidRepository,
) {
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>> = diaryDao.observeForDay(epochDay)

    fun observeDayTotalKcal(epochDay: Long): Flow<Double> = diaryDao.observeDayTotalKcal(epochDay)

    fun observeDailyKcalTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyKcalTotal>> =
        diaryDao.observeDailyKcalTotals(startInclusive, endInclusive)

    suspend fun logFood(epochDay: Long, foodId: String, amountBaseUnits: Double, mealType: MealType) {
        val entry = buildFoodEntry(IdGenerator.newId(), epochDay, foodId, amountBaseUnits, mealType)
        diaryDao.upsert(entry)
        syncFluidForFoodEntry(entry, foodDao.getById(foodId))
    }

    suspend fun logRecipe(epochDay: Long, recipeId: String, servingsConsumed: Double, mealType: MealType) {
        diaryDao.upsert(buildRecipeEntry(IdGenerator.newId(), epochDay, recipeId, servingsConsumed, mealType))
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

    /** Re-derives the nutrition snapshot from the source's *current* state; does not touch other rows. */
    suspend fun updateEntry(entry: DiaryEntry, newQuantity: Double, newMealType: MealType) {
        val updated = when (entry.sourceType) {
            DiarySourceType.FOOD -> buildFoodEntry(entry.id, entry.epochDay, entry.sourceId, newQuantity, newMealType)
            DiarySourceType.RECIPE -> buildRecipeEntry(entry.id, entry.epochDay, entry.sourceId, newQuantity, newMealType)
            // A Schnelleintrag has no source to re-derive from — its snapshot *is* the entry.
            DiarySourceType.QUICK -> entry.copy(mealType = newMealType)
        }
        diaryDao.upsert(updated)
        if (updated.sourceType == DiarySourceType.FOOD) {
            syncFluidForFoodEntry(updated, foodDao.getById(updated.sourceId))
        }
    }

    suspend fun delete(entry: DiaryEntry) {
        diaryDao.delete(entry)
        fluidRepository.deleteForDiaryEntry(entry.id)
    }

    /**
     * Mirrors the fluid a drink-like Lebensmittel contributes into the Flüssigkeiten log. Foods
     * without a [FoodItem.fluidTypeId] clear any previously mirrored row instead, so unlinking a
     * food and re-saving an entry doesn't leave a stale drink behind.
     */
    private suspend fun syncFluidForFoodEntry(entry: DiaryEntry, food: FoodItem?) {
        val typeId = food?.fluidTypeId
        val mlPer100 = food?.fluidMlPer100 ?: 100.0
        fluidRepository.syncFromDiaryEntry(
            diaryEntryId = entry.id,
            epochDay = entry.epochDay,
            typeId = typeId,
            amountMl = entry.quantity * mlPer100 / 100.0,
        )
    }

    private suspend fun buildFoodEntry(
        id: String,
        epochDay: Long,
        foodId: String,
        amountBaseUnits: Double,
        mealType: MealType,
    ): DiaryEntry {
        val food = requireNotNull(foodDao.getById(foodId)) { "Food $foodId not found" }
        val totals = NutritionMath.forFoodAmount(food, amountBaseUnits)
        return DiaryEntry(
            id = id,
            epochDay = epochDay,
            createdAt = Instant.now(),
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
        )
    }

    private suspend fun buildRecipeEntry(
        id: String,
        epochDay: Long,
        recipeId: String,
        servingsConsumed: Double,
        mealType: MealType,
    ): DiaryEntry {
        val recipeWithIngredients = requireNotNull(recipeDao.getWithIngredients(recipeId)) {
            "Recipe $recipeId not found"
        }
        val perServing = NutritionMath.perServing(
            NutritionMath.total(recipeWithIngredients.ingredients),
            recipeWithIngredients.recipe.servings,
        )
        val totals = perServing * servingsConsumed
        return DiaryEntry(
            id = id,
            epochDay = epochDay,
            createdAt = Instant.now(),
            mealType = mealType,
            sourceType = DiarySourceType.RECIPE,
            sourceId = recipeWithIngredients.recipe.id,
            sourceName = recipeWithIngredients.recipe.name,
            quantity = servingsConsumed,
            quantityUnit = "Portion(en)",
            kcal = totals.kcal,
            protein = totals.protein,
            carbs = totals.carbs,
            fat = totals.fat,
        )
    }
}
