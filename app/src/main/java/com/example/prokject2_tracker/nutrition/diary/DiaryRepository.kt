package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.nutrition.NutritionMath
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodDao
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
) {
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>> = diaryDao.observeForDay(epochDay)

    fun observeDayTotalKcal(epochDay: Long): Flow<Double> = diaryDao.observeDayTotalKcal(epochDay)

    fun observeDailyKcalTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyKcalTotal>> =
        diaryDao.observeDailyKcalTotals(startInclusive, endInclusive)

    suspend fun logFood(epochDay: Long, foodId: String, amountBaseUnits: Double, mealType: MealType) {
        diaryDao.upsert(buildFoodEntry(IdGenerator.newId(), epochDay, foodId, amountBaseUnits, mealType))
    }

    suspend fun logRecipe(epochDay: Long, recipeId: String, servingsConsumed: Double, mealType: MealType) {
        diaryDao.upsert(buildRecipeEntry(IdGenerator.newId(), epochDay, recipeId, servingsConsumed, mealType))
    }

    /** Re-derives the nutrition snapshot from the source's *current* state; does not touch other rows. */
    suspend fun updateEntry(entry: DiaryEntry, newQuantity: Double, newMealType: MealType) {
        val updated = when (entry.sourceType) {
            DiarySourceType.FOOD -> buildFoodEntry(entry.id, entry.epochDay, entry.sourceId, newQuantity, newMealType)
            DiarySourceType.RECIPE -> buildRecipeEntry(entry.id, entry.epochDay, entry.sourceId, newQuantity, newMealType)
        }
        diaryDao.upsert(updated)
    }

    suspend fun delete(entry: DiaryEntry) {
        diaryDao.delete(entry)
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
