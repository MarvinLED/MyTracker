package com.example.prokject2_tracker.nutrition.diary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prokject2_tracker.core.database.AppDatabase
import com.example.prokject2_tracker.fluid.FluidRepository
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodItem
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for "last logged per source" query: correctly surfacing the most recent diary entry per
 * food/recipe, grouped by the observation that Schnelleinträge (QUICK) have no meaningful sourceId
 * and should be excluded.
 */
@RunWith(AndroidJUnit4::class)
class DiaryLastLoggedTest {
    private lateinit var db: AppDatabase
    private lateinit var diaryRepository: DiaryRepository
    private lateinit var fluidRepository: FluidRepository

    private val epochDay1 = 20_000L
    private val epochDay2 = 20_001L

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        fluidRepository =
            FluidRepository(db.fluidDao(), db.fluidTypeDao(), db.fluidUnitDao(), db.fluidQuickAddDao())
        diaryRepository = DiaryRepository(db.diaryDao(), db.foodDao(), db.recipeDao(), fluidRepository)

        val now = Instant.now()
        db.foodDao().upsert(
            FoodItem(
                id = "food-1",
                name = "Banana",
                baseUnit = BaseUnit.G,
                kcalPer100 = 89.0,
                proteinPer100 = 1.1,
                carbsPer100 = 23.0,
                fatPer100 = 0.3,
                createdAt = now,
                updatedAt = now,
            )
        )
        db.foodDao().upsert(
            FoodItem(
                id = "food-2",
                name = "Apple",
                baseUnit = BaseUnit.G,
                kcalPer100 = 52.0,
                proteinPer100 = 0.3,
                carbsPer100 = 14.0,
                fatPer100 = 0.2,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun lastLoggedPerSource_surfacesMostRecentEntry() = runBlocking {
        val now = Instant.now()
        diaryRepository.logFood(epochDay1, "food-1", 100.0, MealType.BREAKFAST)
        diaryRepository.logFood(epochDay2, "food-1", 150.0, MealType.LUNCH)

        val lastLogged = diaryRepository.observeLastLoggedPerSource().first()

        val key = DiarySourceType.FOOD to "food-1"
        val lastLog = lastLogged[key]
        assertEquals(epochDay2, lastLog?.epochDay)
        assertEquals(MealType.LUNCH, lastLog?.mealType)
    }

    @Test
    fun lastLoggedPerSource_collapsesMultipleEntriesSameDayByCreatedAt() = runBlocking {
        val now1 = Instant.now()
        val now2 = now1.plusSeconds(60)

        diaryRepository.logFood(epochDay1, "food-1", 100.0, MealType.BREAKFAST)
        // Manually create a second entry on the same day with a different meal type
        db.diaryDao().upsert(
            DiaryEntry(
                id = "entry-2",
                epochDay = epochDay1,
                createdAt = now2,
                mealType = MealType.LUNCH,
                sourceType = DiarySourceType.FOOD,
                sourceId = "food-1",
                sourceName = "Banana",
                quantity = 150.0,
                quantityUnit = "g",
                kcal = 150.0,
                protein = 1.65,
                carbs = 34.5,
                fat = 0.45,
            )
        )

        val lastLogged = diaryRepository.observeLastLoggedPerSource().first()
        val key = DiarySourceType.FOOD to "food-1"
        val lastLog = lastLogged[key]

        // Should return the one with the latest createdAt
        assertEquals(epochDay1, lastLog?.epochDay)
        assertEquals(MealType.LUNCH, lastLog?.mealType)
        assertEquals(now2, lastLog?.createdAt)
    }

    @Test
    fun lastLoggedPerSource_excludesQuickEntries() = runBlocking {
        diaryRepository.logFood(epochDay1, "food-1", 100.0, MealType.BREAKFAST)
        diaryRepository.logQuick(epochDay1, "Snack", 200.0, 5.0, 10.0, 8.0, MealType.SNACK)

        val lastLogged = diaryRepository.observeLastLoggedPerSource().first()

        // Should have food-1 but not the quick entry (QUICK has sourceId = "")
        assertEquals(1, lastLogged.size)
        assertNull(lastLogged[DiarySourceType.QUICK to ""])
    }

    @Test
    fun lastLoggedPerSource_multipleSourcesReturnedSeparately() = runBlocking {
        diaryRepository.logFood(epochDay1, "food-1", 100.0, MealType.BREAKFAST)
        diaryRepository.logFood(epochDay1, "food-2", 150.0, MealType.LUNCH)

        val lastLogged = diaryRepository.observeLastLoggedPerSource().first()

        assertEquals(2, lastLogged.size)
        assertEquals("food-1", lastLogged[DiarySourceType.FOOD to "food-1"]?.sourceId)
        assertEquals("food-2", lastLogged[DiarySourceType.FOOD to "food-2"]?.sourceId)
    }

    @Test
    fun lastLoggedAmount_retrievesQuantityAndUnit() = runBlocking {
        diaryRepository.logFood(epochDay1, "food-1", 100.0, MealType.BREAKFAST, unitName = "Stück", unitCount = 2.0)
        diaryRepository.logFood(epochDay2, "food-1", 150.0, MealType.LUNCH)

        val lastLogged = diaryRepository.getLastLoggedAmount(DiarySourceType.FOOD, "food-1")

        // Most recent entry is from epochDay2 (LUNCH, no custom unit)
        assertEquals(150.0, lastLogged?.quantity)
        assertEquals(null, lastLogged?.unitName)
        assertEquals(null, lastLogged?.unitCount)
    }

    @Test
    fun lastLoggedAmount_returnsNullForUnloggedFood() = runBlocking {
        val lastLogged = diaryRepository.getLastLoggedAmount(DiarySourceType.FOOD, "food-nonexistent")

        assertEquals(null, lastLogged)
    }

    @Test
    fun lastLoggedAmount_prefersCustomUnit() = runBlocking {
        diaryRepository.logFood(epochDay1, "food-1", 100.0, MealType.BREAKFAST, unitName = "Scheibe", unitCount = 2.5)

        val lastLogged = diaryRepository.getLastLoggedAmount(DiarySourceType.FOOD, "food-1")

        assertEquals(100.0, lastLogged?.quantity)
        assertEquals("Scheibe", lastLogged?.unitName)
        assertEquals(2.5, lastLogged?.unitCount)
    }
}
