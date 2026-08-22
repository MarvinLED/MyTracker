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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Copying a whole Tageszeit and pasting it onto another day or meal, against a real database.
 *
 * The fixture is one drink-linked Lebensmittel (Milch) and one plain one, so the mirrored
 * Flüssigkeiten log is exercised along with the entries themselves.
 */
@RunWith(AndroidJUnit4::class)
class MealCopyPasteTest {
    private lateinit var db: AppDatabase
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var fluidRepository: FluidRepository
    private lateinit var recipeRepository: RecipeRepository

    private val sourceDay = 20_000L
    private val targetDay = 20_007L

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
            FluidType(id = "fluidtype-milch", name = "Milch", defaultQuickAddMl = 200.0, sortOrder = 0, createdAt = now),
        )
        db.foodDao().upsert(food("food-milch", "Milch", kcalPer100 = 64.0, fluidTypeId = "fluidtype-milch"))
        db.foodDao().upsert(food("food-hafer", "Haferflocken", kcalPer100 = 370.0, fluidTypeId = null))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun pastingPutsTheWholeMealOnAnotherDayAndLeavesTheOriginalAlone() = runBlocking {
        diaryRepository.logFood(sourceDay, "food-milch", amountBaseUnits = 200.0, mealType = MealType.BREAKFAST)
        diaryRepository.logFood(sourceDay, "food-hafer", amountBaseUnits = 60.0, mealType = MealType.BREAKFAST)
        // Another meal of the same day stays out of it.
        diaryRepository.logFood(sourceDay, "food-hafer", amountBaseUnits = 30.0, mealType = MealType.SNACK)

        val copied = diaryRepository.getMealSnapshots(sourceDay, MealType.BREAKFAST)
        assertEquals(listOf("Milch", "Haferflocken"), copied.map { it.entry.sourceName })

        diaryRepository.copyEntriesTo(copied, targetDay, MealType.DINNER)

        val pasted = diaryRepository.observeForDay(targetDay).first()
        assertEquals(listOf("Milch", "Haferflocken"), pasted.map { it.sourceName })
        assertTrue(pasted.all { it.mealType == MealType.DINNER })
        assertEquals(200.0, pasted.first { it.sourceName == "Milch" }.quantity, 0.001)
        assertEquals(copied.first().entry.kcal, pasted.first { it.sourceName == "Milch" }.kcal, 0.001)
        // New rows, not moved ones.
        assertEquals(3, diaryRepository.observeForDay(sourceDay).first().size)
        copied.forEach { snapshot ->
            assertTrue(pasted.none { it.id == snapshot.entry.id })
        }
    }

    @Test
    fun pastingMirrorsTheDrinkOntoTheTargetDay() = runBlocking {
        diaryRepository.logFood(sourceDay, "food-milch", amountBaseUnits = 200.0, mealType = MealType.BREAKFAST)

        diaryRepository.copyEntriesTo(
            diaryRepository.getMealSnapshots(sourceDay, MealType.BREAKFAST),
            targetDay,
            MealType.LUNCH,
        )

        val fluids = fluidRepository.observeForDay(targetDay).first()
        assertEquals(200.0, fluids.single().amountMl, 0.001)
        assertEquals("Milch", fluids.single().fluidTypeName)
        // The mirrored row belongs to the *pasted* entry, so deleting that one takes it along.
        assertEquals(diaryRepository.observeForDay(targetDay).first().single().id, fluids.single().sourceDiaryEntryId)
        // And the source day keeps its own.
        assertEquals(200.0, fluidRepository.observeForDay(sourceDay).first().single().amountMl, 0.001)
    }

    @Test
    fun pastingIntoAnotherMealOfTheSameDayKeepsBoth() = runBlocking {
        diaryRepository.logFood(sourceDay, "food-hafer", amountBaseUnits = 60.0, mealType = MealType.BREAKFAST)

        diaryRepository.copyEntriesTo(
            diaryRepository.getMealSnapshots(sourceDay, MealType.BREAKFAST),
            sourceDay,
            MealType.DINNER,
        )

        val entries = diaryRepository.observeForDay(sourceDay).first()
        assertEquals(2, entries.size)
        assertEquals(setOf(MealType.BREAKFAST, MealType.DINNER), entries.map { it.mealType }.toSet())
        assertNotEquals(entries[0].id, entries[1].id)
    }

    /** A recipe entry cooked differently that day has to paste as *that* version, not the library one. */
    @Test
    fun pastingCarriesAModifiedRecipesOwnIngredients() = runBlocking {
        val now = Instant.now()
        db.recipeDao().replaceRecipeWithIngredients(
            Recipe(id = "recipe-1", name = "Porridge", servings = 2.0, instructions = null, createdAt = now, updatedAt = now),
            listOf(
                RecipeIngredient(id = "ing-1", recipeId = "recipe-1", foodId = "food-milch", amountBaseUnits = 500.0, sortOrder = 0),
                RecipeIngredient(id = "ing-2", recipeId = "recipe-1", foodId = "food-hafer", amountBaseUnits = 100.0, sortOrder = 1),
            ),
        )
        diaryRepository.logRecipe(sourceDay, "recipe-1", servingsConsumed = 1.0, mealType = MealType.BREAKFAST)
        val logged = diaryRepository.observeForDay(sourceDay).first().single()
        // That morning it was made with 800 ml Milch and no Haferflocken.
        diaryRepository.updateRecipeEntryIngredients(
            entry = logged,
            newQuantity = 1.0,
            newMealType = MealType.BREAKFAST,
            ingredients = listOf(DiaryRecipeIngredientDraft(foodId = "food-milch", amountBaseUnits = 800.0)),
        )
        val modified = diaryRepository.observeForDay(sourceDay).first().single()

        diaryRepository.copyEntriesTo(
            diaryRepository.getMealSnapshots(sourceDay, MealType.BREAKFAST),
            targetDay,
            MealType.BREAKFAST,
        )

        val pasted = diaryRepository.observeForDay(targetDay).first().single()
        assertEquals(modified.kcal, pasted.kcal, 0.001)
        assertTrue(diaryRepository.hasRecipeDayIngredients(pasted.id))
        assertEquals(
            listOf("food-milch"),
            diaryRepository.getRecipeIngredientDrafts(pasted.id).map { it.foodId },
        )
        // 800 ml over 2 portions, one portion eaten.
        assertEquals(400.0, fluidRepository.observeForDay(targetDay).first().single().amountMl, 0.001)
    }

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
