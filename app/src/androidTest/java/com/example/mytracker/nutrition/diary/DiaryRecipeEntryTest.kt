package com.example.mytracker.nutrition.diary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mytracker.core.database.AppDatabase
import com.example.mytracker.fluid.FluidRepository
import com.example.mytracker.fluid.FluidType
import com.example.mytracker.nutrition.food.BaseUnit
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.TagRepository
import com.example.mytracker.nutrition.recipe.Recipe
import com.example.mytracker.nutrition.recipe.RecipeIngredient
import com.example.mytracker.nutrition.recipe.RecipeRepository
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Editing Tagebuch entries against a real database: what a logged Rezept contributes to the
 * Flüssigkeiten log, what changing that recipe for a single day does to the entry, and what happens
 * once the library item an entry came from is gone.
 *
 * The fixture is one recipe of 2 portions = 500 ml Milch (a fluid-linked food) + 200 g Reis.
 */
@RunWith(AndroidJUnit4::class)
class DiaryRecipeEntryTest {
    private lateinit var db: AppDatabase
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var fluidRepository: FluidRepository
    private lateinit var recipeRepository: RecipeRepository

    private val epochDay = 20_000L
    private val milchTypeId = "fluidtype-milch"

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        fluidRepository =
            FluidRepository(db.fluidDao(), db.fluidTypeDao(), db.fluidUnitDao(), db.fluidQuickAddDao())
        diaryRepository = DiaryRepository(db.diaryDao(), db.foodDao(), db.recipeDao(), fluidRepository)
        recipeRepository = RecipeRepository(db.recipeDao(), TagRepository(db.tagDao()), db.fluidTypeDao())

        val now = Instant.now()
        db.fluidTypeDao().upsert(
            FluidType(id = milchTypeId, name = "Milch", defaultQuickAddMl = 200.0, sortOrder = 0, createdAt = now),
        )
        db.foodDao().upsert(food(id = "food-milch", name = "Milch", kcalPer100 = 64.0, fluidTypeId = milchTypeId))
        db.foodDao().upsert(food(id = "food-reis", name = "Reis", kcalPer100 = 350.0, fluidTypeId = null))
        db.recipeDao().replaceRecipeWithIngredients(
            Recipe(id = "recipe-1", name = "Milchreis", servings = 2.0, instructions = null, createdAt = now, updatedAt = now),
            listOf(
                RecipeIngredient(id = "ing-1", recipeId = "recipe-1", foodId = "food-milch", amountBaseUnits = 500.0, sortOrder = 0),
                RecipeIngredient(id = "ing-2", recipeId = "recipe-1", foodId = "food-reis", amountBaseUnits = 200.0, sortOrder = 1),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun recipeListsTheFluidItsIngredientsContain() = runBlocking {
        val recipe = requireNotNull(recipeRepository.getWithNutrition("recipe-1"))

        assertEquals(1, recipe.fluids.size)
        assertEquals("Milch", recipe.fluids.first().name)
        assertEquals(500.0, recipe.fluids.first().totalMl, 0.001)
    }

    @Test
    fun loggingOnePortionMirrorsThatPortionsFluid() = runBlocking {
        diaryRepository.logRecipe(epochDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.LUNCH)

        // One of two portions of a recipe holding 500 ml Milch: 250 ml.
        val fluidEntries = fluidRepository.observeForDay(epochDay).first()
        assertEquals(1, fluidEntries.size)
        assertEquals(250.0, fluidEntries.first().amountMl, 0.001)
        assertEquals("Milch", fluidEntries.first().fluidTypeName)
        // Marked as derived, so the Flüssigkeiten tab shows it as read-only.
        assertEquals(loggedEntry().id, fluidEntries.first().sourceDiaryEntryId)
    }

    @Test
    fun changingTheAmountRescalesNutritionAndMirroredFluid() = runBlocking {
        diaryRepository.logRecipe(epochDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.LUNCH)
        val entry = loggedEntry()

        diaryRepository.updateEntry(entry, newQuantity = 2.0, newMealType = MealType.DINNER)

        val updated = loggedEntry()
        assertEquals(2.0, updated.quantity, 0.001)
        assertEquals(MealType.DINNER, updated.mealType)
        assertEquals(entry.kcal * 2.0, updated.kcal, 0.001)
        // Editing keeps the entry in place in the day's order instead of moving it to the end.
        assertEquals(entry.createdAt, updated.createdAt)
        assertEquals(500.0, fluidRepository.observeForDay(epochDay).first().single().amountMl, 0.001)
    }

    @Test
    fun changingTheRecipeForOneDayLeavesTheLibraryRecipeAlone() = runBlocking {
        diaryRepository.logRecipe(epochDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.LUNCH)
        val entry = loggedEntry()

        // That day it was made with 800 ml Milch instead of 500, and without the Reis.
        diaryRepository.updateRecipeEntryIngredients(
            entry = entry,
            newQuantity = 1.0,
            newMealType = entry.mealType,
            ingredients = listOf(DiaryRecipeIngredientDraft(foodId = "food-milch", amountBaseUnits = 800.0)),
        )

        val updated = loggedEntry()
        assertEquals(800.0 / 100.0 * 64.0 / 2.0, updated.kcal, 0.001)
        assertEquals(400.0, fluidRepository.observeForDay(epochDay).first().single().amountMl, 0.001)
        assertTrue(diaryRepository.hasRecipeDayIngredients(updated.id))

        // The library recipe and its own fluid listing are untouched.
        val recipe = requireNotNull(recipeRepository.getWithNutrition("recipe-1"))
        assertEquals(2, recipe.ingredients.size)
        assertEquals(500.0, recipe.fluids.single().totalMl, 0.001)
    }

    @Test
    fun resettingDropsThePerDayCopyAndFollowsTheRecipeAgain() = runBlocking {
        diaryRepository.logRecipe(epochDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.LUNCH)
        val logged = loggedEntry()
        diaryRepository.updateRecipeEntryIngredients(
            entry = logged,
            newQuantity = 1.0,
            newMealType = logged.mealType,
            ingredients = listOf(DiaryRecipeIngredientDraft(foodId = "food-milch", amountBaseUnits = 800.0)),
        )

        diaryRepository.resetRecipeEntryToLibrary(loggedEntry(), newQuantity = 1.0, newMealType = MealType.LUNCH)

        assertEquals(false, diaryRepository.hasRecipeDayIngredients(logged.id))
        assertEquals(logged.kcal, loggedEntry().kcal, 0.001)
        assertEquals(250.0, fluidRepository.observeForDay(epochDay).first().single().amountMl, 0.001)
    }

    @Test
    fun deletingTheEntryRemovesItsPerDayCopyAndMirroredFluid() = runBlocking {
        diaryRepository.logRecipe(epochDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.LUNCH)
        val entry = loggedEntry()
        diaryRepository.updateRecipeEntryIngredients(
            entry = entry,
            newQuantity = 1.0,
            newMealType = entry.mealType,
            ingredients = listOf(DiaryRecipeIngredientDraft(foodId = "food-milch", amountBaseUnits = 800.0)),
        )

        diaryRepository.delete(loggedEntry())

        assertEquals(emptyList<DiaryEntry>(), diaryRepository.observeForDay(epochDay).first())
        assertEquals(false, diaryRepository.hasRecipeDayIngredients(entry.id))
        assertEquals(emptyList<Any>(), fluidRepository.observeForDay(epochDay).first())
    }

    /**
     * Nothing stops a Rezept from being deleted while old entries still reference it, and editing
     * such an entry must not blow up — the stored snapshot is simply re-scaled.
     */
    @Test
    fun editingStillWorksAfterTheRecipeWasDeletedFromTheLibrary() = runBlocking {
        diaryRepository.logRecipe(epochDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.LUNCH)
        val entry = loggedEntry()
        recipeRepository.delete(requireNotNull(recipeRepository.getRecipeOnly("recipe-1")))

        diaryRepository.updateEntry(entry, newQuantity = 2.0, newMealType = MealType.DINNER)

        val updated = loggedEntry()
        assertEquals(2.0, updated.quantity, 0.001)
        assertEquals(entry.kcal * 2.0, updated.kcal, 0.001)
    }

    /**
     * Same for a Lebensmittel entry whose food was deleted from the Bibliothek. Only a food that no
     * recipe uses can actually be deleted (a foreign key blocks the rest), so this uses its own.
     */
    @Test
    fun editingStillWorksAfterTheFoodWasDeletedFromTheLibrary() = runBlocking {
        val standalone = food(id = "food-solo", name = "Banane", kcalPer100 = 89.0, fluidTypeId = null)
        db.foodDao().upsert(standalone)
        diaryRepository.logFood(epochDay, standalone.id, amountBaseUnits = 200.0, mealType = MealType.BREAKFAST)
        val entry = loggedEntry()
        db.foodDao().delete(standalone)

        diaryRepository.updateEntry(entry, newQuantity = 400.0, newMealType = MealType.SNACK)

        val updated = loggedEntry()
        assertEquals(400.0, updated.quantity, 0.001)
        assertEquals(entry.kcal * 2.0, updated.kcal, 0.001)
    }

    private suspend fun loggedEntry(): DiaryEntry = diaryRepository.observeForDay(epochDay).first().single()

    private fun food(id: String, name: String, kcalPer100: Double, fluidTypeId: String?): FoodItem {
        val now = Instant.now()
        return FoodItem(
            id = id,
            name = name,
            baseUnit = BaseUnit.G,
            kcalPer100 = kcalPer100,
            proteinPer100 = 0.0,
            carbsPer100 = 0.0,
            fatPer100 = 0.0,
            fluidTypeId = fluidTypeId,
            fluidMlPer100 = if (fluidTypeId == null) null else 100.0,
            createdAt = now,
            updatedAt = now,
        )
    }
}
