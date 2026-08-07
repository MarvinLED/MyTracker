package com.example.prokject2_tracker.core.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prokject2_tracker.core.database.AppDatabase
import com.example.prokject2_tracker.fitness.strength.MovementDirection
import com.example.prokject2_tracker.fitness.strength.StrengthExercise
import com.example.prokject2_tracker.fitness.strength.StrengthExerciseLibraryExportProvider
import com.example.prokject2_tracker.fitness.strength.StrengthLogEntry
import com.example.prokject2_tracker.fitness.strength.StrengthLogExportProvider
import com.example.prokject2_tracker.fitness.strength.StrengthSet
import com.example.prokject2_tracker.nutrition.diary.DiaryEntriesExportProvider
import com.example.prokject2_tracker.nutrition.diary.DiaryEntry
import com.example.prokject2_tracker.nutrition.diary.DiaryRecipeIngredient
import com.example.prokject2_tracker.nutrition.diary.DiarySourceType
import com.example.prokject2_tracker.nutrition.diary.MealType
import com.example.prokject2_tracker.nutrition.food.BaseUnit
import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.food.FoodLibraryExportProvider
import com.example.prokject2_tracker.sleep.SleepEntriesExportProvider
import com.example.prokject2_tracker.sleep.SleepEntry
import com.example.prokject2_tracker.sleep.SleepTag
import com.example.prokject2_tracker.sleep.SleepTagLibraryExportProvider
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A backup of the Tägliche Einträge against a real database: the point of this scope is data that
 * cannot be reconstructed, so what matters is that a night, a training session and a logged meal
 * come back whole on a device that starts empty — and that Ersetzen really does leave exactly what
 * the file held, no more.
 */
@RunWith(AndroidJUnit4::class)
class DailyEntriesBackupTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private val createdAt = Instant.ofEpochMilli(1_700_000_000_000)

    @Before
    fun setUp() {
        source = freshDatabase()
        target = freshDatabase()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun aLoggedMealComesBackWithItsRecipeBreakdown() = runBlocking {
        source.foodDao().upsert(food("food-1", "Haferflocken"))
        source.diaryDao().upsertWithRecipeIngredients(
            diaryEntry("diary-1", epochDay = 20_000),
            listOf(ingredient("ing-1", "diary-1", "food-1")),
        )

        restore(source, target, BackupScope.LIBRARY, BackupScope.DAILY_ENTRIES)

        val entries = target.diaryDao().getAllOnce()
        assertEquals(1, entries.size)
        assertEquals(20_000L, entries.single().epochDay)
        assertEquals(350.0, entries.single().kcal, 0.0001)
        assertEquals(listOf("food-1"), target.diaryDao().getAllRecipeIngredientsOnce().map { it.foodId })
    }

    /**
     * The Bibliothek left behind: the meal itself still lands — it carries its own name and
     * nutrition — while the breakdown, which is a foreign key into the foods, is dropped rather
     * than allowed to fail the whole import.
     */
    @Test
    fun aMealLandsEvenWhenItsFoodsStayedBehind() = runBlocking {
        source.foodDao().upsert(food("food-1", "Haferflocken"))
        source.diaryDao().upsertWithRecipeIngredients(
            diaryEntry("diary-1", epochDay = 20_000),
            listOf(ingredient("ing-1", "diary-1", "food-1")),
        )

        restore(source, target, BackupScope.DAILY_ENTRIES)

        assertEquals(1, target.diaryDao().getAllOnce().size)
        assertEquals("Haferflocken-Porridge", target.diaryDao().getAllOnce().single().sourceName)
        assertTrue(target.diaryDao().getAllRecipeIngredientsOnce().isEmpty())
    }

    @Test
    fun aTrainingSessionComesBackWithItsSets() = runBlocking {
        source.strengthExerciseDao().upsert(exercise("ex-1", "Kniebeuge"))
        source.strengthLogDao().upsert(logEntry("log-1", "ex-1", epochDay = 20_001))
        source.strengthSetDao().upsertAll(
            listOf(
                set("set-1", "log-1", "ex-1", epochDay = 20_001, index = 0, reps = 8, weightKg = 60.0),
                set("set-2", "log-1", "ex-1", epochDay = 20_001, index = 1, reps = 6, weightKg = 70.0),
            ),
        )

        restore(source, target, BackupScope.LIBRARY, BackupScope.DAILY_ENTRIES)

        val sets = target.strengthSetDao().getAllOnce().sortedBy { it.setIndex }
        assertEquals(1, target.strengthLogDao().getAllOnce().size)
        assertEquals(2, sets.size)
        assertEquals(8, sets[0].reps)
        assertEquals(70.0, sets[1].weightKg!!, 0.0001)
        // Denormalised onto the sets, and rebuilt from the entry rather than stored twice.
        assertTrue(sets.all { it.epochDay == 20_001L && it.exerciseId == "ex-1" })
    }

    /** Without its Übung a set has nothing to group under, so the session is skipped whole. */
    @Test
    fun aTrainingSessionWithoutItsExerciseIsSkipped() = runBlocking {
        source.strengthExerciseDao().upsert(exercise("ex-1", "Kniebeuge"))
        source.strengthLogDao().upsert(logEntry("log-1", "ex-1", epochDay = 20_001))
        source.strengthSetDao().upsertAll(
            listOf(set("set-1", "log-1", "ex-1", epochDay = 20_001, index = 0, reps = 8, weightKg = 60.0)),
        )

        restore(source, target, BackupScope.DAILY_ENTRIES)

        assertTrue(target.strengthLogDao().getAllOnce().isEmpty())
        assertTrue(target.strengthSetDao().getAllOnce().isEmpty())
    }

    @Test
    fun aNightComesBackWithItsTags() = runBlocking {
        source.sleepTagDao().upsert(SleepTag(id = "tag-1", name = "heiß", sortOrder = 0, createdAt = createdAt))
        source.sleepDao().upsert(night(epochDay = 20_002))
        source.sleepDao().replaceTagsForEntry("sleep-20002", listOf("tag-1"))

        restore(source, target, BackupScope.LIBRARY, BackupScope.DAILY_ENTRIES)

        val nights = target.sleepDao().getAllOnce()
        assertEquals(1, nights.size)
        assertEquals(7, nights.single().morningFitness)
        assertEquals(listOf("tag-1"), target.sleepDao().getTagIdsForEntry("sleep-20002"))
    }

    /** Merging fills gaps: a night already on the device is left exactly as the device has it. */
    @Test
    fun mergingKeepsWhatTheDeviceAlreadyHas() = runBlocking {
        source.sleepDao().upsert(night(epochDay = 20_002, morningFitness = 7))
        target.sleepDao().upsert(night(epochDay = 20_002, morningFitness = 3))

        restore(source, target, BackupScope.DAILY_ENTRIES)

        assertEquals(3, target.sleepDao().getAllOnce().single().morningFitness)
    }

    @Test
    fun replacingLeavesExactlyWhatTheFileHeld() = runBlocking {
        source.sleepDao().upsert(night(epochDay = 20_002, morningFitness = 7))
        target.sleepDao().upsert(night(epochDay = 20_002, morningFitness = 3))
        target.sleepDao().upsert(night(epochDay = 20_003, morningFitness = 5))

        restore(source, target, BackupScope.DAILY_ENTRIES, mode = ImportMode.REPLACE)

        val nights = target.sleepDao().getAllOnce()
        assertEquals(1, nights.size)
        assertEquals(20_002L, nights.single().epochDay)
        assertEquals(7, nights.single().morningFitness)
    }

    /** Ersetzen only empties what was ticked — the other categories are not collateral. */
    @Test
    fun replacingOneCategoryLeavesTheOthersAlone() = runBlocking {
        source.sleepDao().upsert(night(epochDay = 20_002))
        target.foodDao().upsert(food("food-local", "Nur hier"))
        target.sleepDao().upsert(night(epochDay = 20_003))

        restore(source, target, BackupScope.DAILY_ENTRIES, mode = ImportMode.REPLACE)

        assertNotNull(target.foodDao().getById("food-local"))
        assertNull(target.sleepDao().getForDay(20_003))
    }

    /**
     * Exports from [from], imports into [into] — the whole round trip a user makes between two
     * devices, through the same envelope the app writes.
     */
    private suspend fun restore(
        from: AppDatabase,
        into: AppDatabase,
        vararg scopes: BackupScope,
        mode: ImportMode = ImportMode.MERGE,
    ) {
        val selected = scopes.toSet()
        val json = BackupRepository(providersFor(from)).exportToJson(selected)
        BackupRepository(providersFor(into)).importFromJson(json, selected, mode)
    }

    /** The providers this test exercises, wired straight to a database instead of through Hilt. */
    private fun providersFor(db: AppDatabase): Set<BackupExportProvider> = setOf(
        FoodLibraryExportProvider(db.foodDao(), db.tagDao(), db.foodUnitDao()),
        StrengthExerciseLibraryExportProvider(db.strengthExerciseDao()),
        SleepTagLibraryExportProvider(db.sleepTagDao()),
        DiaryEntriesExportProvider(db.diaryDao(), db.foodDao()),
        StrengthLogExportProvider(db.strengthLogDao(), db.strengthSetDao(), db.strengthExerciseDao()),
        SleepEntriesExportProvider(db.sleepDao(), db.sleepTagDao()),
    )

    private fun freshDatabase(): AppDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    private fun food(id: String, name: String) = FoodItem(
        id = id,
        name = name,
        baseUnit = BaseUnit.G,
        kcalPer100 = 370.0,
        proteinPer100 = 13.0,
        carbsPer100 = 59.0,
        fatPer100 = 7.0,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun diaryEntry(id: String, epochDay: Long) = DiaryEntry(
        id = id,
        epochDay = epochDay,
        createdAt = createdAt,
        mealType = MealType.BREAKFAST,
        sourceType = DiarySourceType.RECIPE,
        sourceId = "recipe-1",
        sourceName = "Haferflocken-Porridge",
        quantity = 1.0,
        quantityUnit = "Portion",
        kcal = 350.0,
        protein = 12.0,
        carbs = 55.0,
        fat = 7.0,
    )

    private fun ingredient(id: String, diaryEntryId: String, foodId: String) = DiaryRecipeIngredient(
        id = id,
        diaryEntryId = diaryEntryId,
        foodId = foodId,
        amountBaseUnits = 80.0,
    )

    private fun exercise(id: String, name: String) = StrengthExercise(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = createdAt,
        movementDirection = MovementDirection.PUSH,
    )

    private fun logEntry(id: String, exerciseId: String, epochDay: Long) = StrengthLogEntry(
        id = id,
        epochDay = epochDay,
        createdAt = createdAt,
        exerciseId = exerciseId,
        exerciseName = "Kniebeuge",
    )

    private fun set(
        id: String,
        logEntryId: String,
        exerciseId: String,
        epochDay: Long,
        index: Int,
        reps: Int,
        weightKg: Double,
    ) = StrengthSet(
        id = id,
        logEntryId = logEntryId,
        epochDay = epochDay,
        exerciseId = exerciseId,
        setIndex = index,
        reps = reps,
        weightKg = weightKg,
    )

    private fun night(epochDay: Long, morningFitness: Int = 7) = SleepEntry(
        id = "sleep-$epochDay",
        epochDay = epochDay,
        startMinuteOfDay = 23 * 60 + 10,
        endMinuteOfDay = 6 * 60 + 45,
        morningFitness = morningFitness,
        lastMealMinuteOfDay = 20 * 60 + 30,
        createdAt = createdAt,
    )
}
