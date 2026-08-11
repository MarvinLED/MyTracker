package com.example.prokject2_tracker.nutrition.diary

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prokject2_tracker.core.database.AppDatabase
import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.goals.NutrientGoalChange
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
 * The queries behind the Verlauf, against real Room: the ranged all-nutrient roll-up and the goal
 * change log. Both are new SQL, and both are read by a chart where a wrong number looks entirely
 * plausible.
 */
@RunWith(AndroidJUnit4::class)
class DiaryHistoryQueriesTest {
    private lateinit var db: AppDatabase

    private val now = Instant.ofEpochMilli(1_700_000_000_000)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(id: String, epochDay: Long, kcal: Double, sugar: Double, salt: Double) = DiaryEntry(
        id = id,
        epochDay = epochDay,
        createdAt = now,
        mealType = MealType.BREAKFAST,
        sourceType = DiarySourceType.QUICK,
        sourceId = "",
        sourceName = "Schnelleintrag",
        quantity = 1.0,
        quantityUnit = "g",
        kcal = kcal,
        protein = 0.0,
        carbs = 0.0,
        fat = 0.0,
        sugar = sugar,
        salt = salt,
    )

    @Test
    fun dailyNutritionTotals_sumPerDayAndStayInsideTheRange() = runBlocking {
        db.diaryDao().upsert(entry("a", 20_000, kcal = 400.0, sugar = 12.0, salt = 0.5))
        db.diaryDao().upsert(entry("b", 20_000, kcal = 600.0, sugar = 8.0, salt = 1.5))
        db.diaryDao().upsert(entry("c", 20_001, kcal = 900.0, sugar = 20.0, salt = 2.0))
        // Outside the window asked for — it must not leak into the first day's total.
        db.diaryDao().upsert(entry("d", 19_999, kcal = 5000.0, sugar = 99.0, salt = 9.0))

        val rows = db.diaryDao().observeDailyNutritionTotals(20_000, 20_001).first()

        assertEquals(listOf(20_000L, 20_001L), rows.map { it.epochDay })
        assertEquals(1000.0, rows[0].totals.kcal, 0.001)
        assertEquals(20.0, rows[0].totals.sugar, 0.001)
        assertEquals(2.0, rows[0].totals.salt, 0.001)
        assertEquals(900.0, rows[1].totals.kcal, 0.001)
    }

    @Test
    fun dailyNutritionTotals_skipDaysWithNothingLogged() = runBlocking {
        db.diaryDao().upsert(entry("a", 20_000, kcal = 400.0, sugar = 1.0, salt = 0.1))
        db.diaryDao().upsert(entry("b", 20_005, kcal = 500.0, sugar = 2.0, salt = 0.2))

        val rows = db.diaryDao().observeDailyNutritionTotals(20_000, 20_005).first()

        // A gap is a gap: the chart draws no point rather than a zero the user never logged.
        assertEquals(listOf(20_000L, 20_005L), rows.map { it.epochDay })
    }

    @Test
    fun firstLoggedDay_isNullUntilSomethingIsLogged() = runBlocking {
        assertNull(db.diaryDao().observeFirstLoggedDay().first())

        db.diaryDao().upsert(entry("a", 20_010, kcal = 100.0, sugar = 0.0, salt = 0.0))
        db.diaryDao().upsert(entry("b", 19_990, kcal = 100.0, sugar = 0.0, salt = 0.0))

        assertEquals(19_990L, db.diaryDao().observeFirstLoggedDay().first())
    }

    @Test
    fun goalChanges_comeBackOldestFirstAcrossNutrients() = runBlocking {
        val dao = db.nutrientGoalChangeDao()
        dao.insert(NutrientGoalChange("bump", Nutrient.KCAL, 20_000, 2400.0, 2600.0, now))
        dao.insert(NutrientGoalChange("seed", Nutrient.KCAL, 0, 1800.0, null, now))
        dao.insert(NutrientGoalChange("salt", Nutrient.SALT, 20_001, null, 6.0, now))

        assertEquals(listOf("seed", "bump", "salt"), dao.observeAll().first().map { it.id })
        assertEquals(2, dao.countForNutrient(Nutrient.KCAL))
        assertEquals(0, dao.countForNutrient(Nutrient.PROTEIN))
    }

    @Test
    fun goalChanges_roundTripNullBoundsAndTheirEnum() = runBlocking {
        val dao = db.nutrientGoalChangeDao()
        dao.insert(NutrientGoalChange("drop", Nutrient.SUGAR, 20_002, null, null, now))

        val stored = dao.getAllOnce().single()

        assertEquals(Nutrient.SUGAR, stored.nutrient)
        assertNull(stored.minValue)
        assertNull(stored.maxValue)
        assertEquals(now, stored.changedAt)
    }
}
