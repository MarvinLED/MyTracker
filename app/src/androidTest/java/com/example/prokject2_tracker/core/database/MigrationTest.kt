package com.example.prokject2_tracker.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the real migration chain (see [Migrations]) end-to-end. This is the primary
 * correctness signal for the fix that replaced `fallbackToDestructiveMigration` — a passing
 * fresh-install chain plus one data-preserving restructure check, not a click-through.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To7_seedsSevenDefaultCardioActivityTypes() {
        helper.createDatabase(dbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            dbName,
            7,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )

        db.query("SELECT COUNT(*) FROM cardio_activity_types").use { cursor ->
            cursor.moveToFirst()
            assertEquals(7, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate4To7_preservesLegacyFluidEntryAsFluidTypeName() {
        val v4 = helper.createDatabase(dbName, 4)
        v4.execSQL(
            "INSERT INTO fluid_entries (id, epochDay, createdAt, type, amountMl) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'COFFEE', 125.0)",
        )
        v4.close()

        val db = helper.runMigrationsAndValidate(
            dbName,
            7,
            true,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
        )

        db.query("SELECT fluidTypeName FROM fluid_entries WHERE id = 'entry-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Kaffee", cursor.getString(0))
        }
        db.close()
    }

    @Test
    fun migrate1To8_seedsEightDefaultMuscleGroups() {
        helper.createDatabase(dbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            dbName,
            8,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
        )

        db.query("SELECT COUNT(*) FROM muscle_groups").use { cursor ->
            cursor.moveToFirst()
            assertEquals(8, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate7To8_expandsLegacySetsIntoIndividualStrengthSets() {
        val v7 = helper.createDatabase(dbName, 7)
        v7.execSQL(
            "INSERT INTO strength_exercises (id, name, muscleGroup, createdAt, updatedAt) " +
                "VALUES ('exercise-1', 'Bankdrücken', 'CHEST', 1700000000000, 1700000000000)",
        )
        v7.execSQL(
            "INSERT INTO strength_log_entries (id, epochDay, createdAt, exerciseId, exerciseName, sets, reps, weightKg, note) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'exercise-1', 'Bankdrücken', 3, 10, 40.0, NULL)",
        )
        v7.close()

        val db = helper.runMigrationsAndValidate(dbName, 8, true, MIGRATION_7_8)

        db.query("SELECT COUNT(*) FROM strength_sets WHERE logEntryId = 'entry-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(3, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM strength_sets WHERE logEntryId = 'entry-1' AND muscleGroupId = 'musclegroup-brust'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(3, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate1To9_seedsEightDefaultMuscleGroups() {
        helper.createDatabase(dbName, 1).close()

        val db = helper.runMigrationsAndValidate(
            dbName,
            9,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
        )

        db.query("SELECT COUNT(*) FROM muscle_groups").use { cursor ->
            cursor.moveToFirst()
            assertEquals(8, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate8To9_convertsSingleMuscleGroupToCrossRefRowAndPreservesSets() {
        val v8 = helper.createDatabase(dbName, 8)
        v8.execSQL(
            "INSERT INTO muscle_groups (id, name, sortOrder, createdAt) " +
                "VALUES ('musclegroup-brust', 'Brust', 0, 1700000000000)",
        )
        v8.execSQL(
            "INSERT INTO strength_exercises (id, name, muscleGroupId, muscleGroupName, createdAt, updatedAt) " +
                "VALUES ('exercise-1', 'Bankdrücken', 'musclegroup-brust', 'Brust', 1700000000000, 1700000000000)",
        )
        v8.execSQL(
            "INSERT INTO strength_log_entries (id, epochDay, createdAt, exerciseId, exerciseName, note) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'exercise-1', 'Bankdrücken', NULL)",
        )
        v8.execSQL(
            "INSERT INTO strength_sets (id, logEntryId, epochDay, exerciseId, muscleGroupId, setIndex, reps, weightKg) " +
                "VALUES ('set-1', 'entry-1', 20000, 'exercise-1', 'musclegroup-brust', 0, 10, 40.0)",
        )
        v8.close()

        val db = helper.runMigrationsAndValidate(dbName, 9, true, MIGRATION_8_9)

        db.query(
            "SELECT COUNT(*) FROM strength_exercise_muscle_groups " +
                "WHERE exerciseId = 'exercise-1' AND muscleGroupId = 'musclegroup-brust'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT reps, weightKg FROM strength_sets WHERE id = 'set-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(10, cursor.getInt(0))
            assertEquals(40.0, cursor.getDouble(1), 0.0001)
        }
        db.close()
    }

    @Test
    fun migrate9To10_addsNullableFluidLinkColumnsWithoutTouchingExistingRows() {
        val v9 = helper.createDatabase(dbName, 9)
        v9.execSQL(
            "INSERT INTO food_items (id, name, baseUnit, kcalPer100, proteinPer100, carbsPer100, fatPer100, " +
                "saturatedFatPer100, sugarPer100, fiberPer100, saltPer100, createdAt, updatedAt) " +
                "VALUES ('food-1', 'Milch', 'G', 64.0, 3.4, 4.8, 3.6, 2.3, 4.8, 0.0, 0.1, 1700000000000, 1700000000000)",
        )
        v9.execSQL(
            "INSERT INTO fluid_types (id, name, defaultQuickAddMl, sortOrder, createdAt) " +
                "VALUES ('fluidtype-wasser', 'Wasser', 250.0, 0, 1700000000000)",
        )
        v9.execSQL(
            "INSERT INTO fluid_entries (id, epochDay, createdAt, fluidTypeId, fluidTypeName, amountMl) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'fluidtype-wasser', 'Wasser', 250.0)",
        )
        v9.close()

        val db = helper.runMigrationsAndValidate(dbName, 10, true, MIGRATION_9_10)

        // Existing rows survive, and every new column defaults to "not set" rather than a value.
        db.query("SELECT name, fluidTypeId, fluidMlPer100 FROM food_items WHERE id = 'food-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Milch", cursor.getString(0))
            assertEquals(true, cursor.isNull(1))
            assertEquals(true, cursor.isNull(2))
        }
        db.query("SELECT colorArgb FROM fluid_types WHERE id = 'fluidtype-wasser'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(true, cursor.isNull(0))
        }
        db.query("SELECT amountMl, sourceDiaryEntryId FROM fluid_entries WHERE id = 'entry-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(250.0, cursor.getDouble(0), 0.0001)
            assertEquals(true, cursor.isNull(1))
        }
        db.close()
    }

    @Test
    fun migrate10To11_keepsRecipeDiaryEntriesAndCascadesTheirPerDayIngredients() {
        val v10 = helper.createDatabase(dbName, 10)
        v10.execSQL(
            "INSERT INTO food_items (id, name, baseUnit, kcalPer100, proteinPer100, carbsPer100, fatPer100, " +
                "saturatedFatPer100, sugarPer100, fiberPer100, saltPer100, createdAt, updatedAt) " +
                "VALUES ('food-1', 'Reis', 'G', 350.0, 7.0, 78.0, 0.6, 0.2, 0.1, 1.4, 0.0, 1700000000000, 1700000000000)",
        )
        v10.execSQL(
            "INSERT INTO diary_entries (id, epochDay, createdAt, mealType, sourceType, sourceId, sourceName, " +
                "quantity, quantityUnit, kcal, protein, carbs, fat) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'LUNCH', 'RECIPE', 'recipe-1', 'Reispfanne', " +
                "2.0, 'Portion(en)', 700.0, 14.0, 156.0, 1.2)",
        )
        v10.close()

        val db = helper.runMigrationsAndValidate(dbName, 11, true, MIGRATION_10_11)

        // The existing recipe entry survives with its snapshot, and recipeServings starts unset so it
        // keeps falling back to the library recipe.
        db.query("SELECT sourceName, kcal, recipeServings FROM diary_entries WHERE id = 'entry-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Reispfanne", cursor.getString(0))
            assertEquals(true, cursor.isNull(2))
            assertEquals(700.0, cursor.getDouble(1), 0.0001)
        }

        // A per-day ingredient row belongs to its diary entry: deleting the entry takes it along.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            "INSERT INTO diary_recipe_ingredients (id, diaryEntryId, foodId, amountBaseUnits, sortOrder) " +
                "VALUES ('day-ing-1', 'entry-1', 'food-1', 220.0, 0)",
        )
        db.execSQL("DELETE FROM diary_entries WHERE id = 'entry-1'")
        db.query("SELECT COUNT(*) FROM diary_recipe_ingredients").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate11To12_addsRemainingNutrientsAsZeroWithoutTouchingTheMacros() {
        val v11 = helper.createDatabase(dbName, 11)
        v11.execSQL(
            "INSERT INTO diary_entries (id, epochDay, createdAt, mealType, sourceType, sourceId, sourceName, " +
                "quantity, quantityUnit, kcal, protein, carbs, fat, recipeServings) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'SNACK', 'FOOD', 'food-1', 'Banane', " +
                "120.0, 'g', 107.0, 1.3, 27.0, 0.4, NULL)",
        )
        v11.close()

        val db = helper.runMigrationsAndValidate(dbName, 12, true, MIGRATION_11_12)

        // The macros logged before the extra nutrients existed are untouched, and the new columns
        // read as 0 = "not recorded" rather than being invented from the food's current values.
        db.query(
            "SELECT kcal, protein, saturatedFat, sugar, fiber, salt FROM diary_entries WHERE id = 'entry-1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(107.0, cursor.getDouble(0), 0.0001)
            assertEquals(1.3, cursor.getDouble(1), 0.0001)
            assertEquals(0.0, cursor.getDouble(2), 0.0001)
            assertEquals(0.0, cursor.getDouble(3), 0.0001)
            assertEquals(0.0, cursor.getDouble(4), 0.0001)
            assertEquals(0.0, cursor.getDouble(5), 0.0001)
        }
        db.close()
    }

    @Test
    fun migrate13To14_movesTheSingleServingIntoFoodUnitsAndKeepsTheFood() {
        val v13 = helper.createDatabase(dbName, 13)
        v13.execSQL(
            "INSERT INTO food_items (id, name, brand, baseUnit, kcalPer100, proteinPer100, carbsPer100, fatPer100, " +
                "saturatedFatPer100, sugarPer100, fiberPer100, saltPer100, servingName, servingAmount, " +
                "createdAt, updatedAt) " +
                "VALUES ('food-1', 'Toastbrot', 'Golden', 'G', 250.0, 8.0, 47.0, 3.0, 0.6, 4.0, 3.0, 1.0, " +
                "'Scheibe', 25.0, 1700000000000, 1700000000000)",
        )
        // A food without a serving must not produce a unit row.
        v13.execSQL(
            "INSERT INTO food_items (id, name, baseUnit, kcalPer100, proteinPer100, carbsPer100, fatPer100, " +
                "saturatedFatPer100, sugarPer100, fiberPer100, saltPer100, createdAt, updatedAt) " +
                "VALUES ('food-2', 'Reis', 'G', 350.0, 7.0, 78.0, 0.6, 0.2, 0.1, 1.4, 0.0, 1700000000000, 1700000000000)",
        )
        v13.execSQL(
            "INSERT INTO diary_entries (id, epochDay, createdAt, mealType, sourceType, sourceId, sourceName, " +
                "quantity, quantityUnit, kcal, protein, carbs, fat, saturatedFat, sugar, fiber, salt, recipeServings) " +
                "VALUES ('entry-1', 20000, 1700000000000, 'BREAKFAST', 'FOOD', 'food-1', 'Toastbrot', " +
                "50.0, 'g', 125.0, 4.0, 23.5, 1.5, 0.3, 2.0, 1.5, 0.5, NULL)",
        )
        v13.close()

        val db = helper.runMigrationsAndValidate(dbName, 14, true, MIGRATION_13_14)

        // The serving became a unit...
        db.query("SELECT foodItemId, name, amountBaseUnits FROM food_units").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("food-1", cursor.getString(0))
            assertEquals("Scheibe", cursor.getString(1))
            assertEquals(25.0, cursor.getDouble(2), 0.0001)
        }
        // ...and rebuilding food_items to drop the two columns kept every other value.
        db.query("SELECT name, brand, kcalPer100, saltPer100 FROM food_items WHERE id = 'food-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Toastbrot", cursor.getString(0))
            assertEquals("Golden", cursor.getString(1))
            assertEquals(250.0, cursor.getDouble(2), 0.0001)
            assertEquals(1.0, cursor.getDouble(3), 0.0001)
        }
        db.query("SELECT COUNT(*) FROM food_items").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        // Everything logged so far was typed in grams, which is exactly what a null unit means.
        db.query("SELECT quantity, unitName, unitCount FROM diary_entries WHERE id = 'entry-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(50.0, cursor.getDouble(0), 0.0001)
            assertEquals(true, cursor.isNull(1))
            assertEquals(true, cursor.isNull(2))
        }
        // Deleting a food takes its units with it.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM diary_entries WHERE id = 'entry-1'")
        db.execSQL("DELETE FROM food_items WHERE id = 'food-1'")
        db.query("SELECT COUNT(*) FROM food_units").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate14To15_addsEmptyBodySiteAndMeasurementTables() {
        val v14 = helper.createDatabase(dbName, 14)
        v14.execSQL(
            "INSERT INTO body_weight_entries (id, epochDay, weightKg, createdAt) " +
                "VALUES ('weight-20000', 20000, 78.5, 1700000000000)",
        )
        v14.close()

        val db = helper.runMigrationsAndValidate(dbName, 15, true, MIGRATION_14_15)

        // Körperstellen are user-created, so the library starts empty rather than seeded.
        db.query("SELECT COUNT(*) FROM body_sites").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // The unrelated tracked data is untouched.
        db.query("SELECT weightKg FROM body_weight_entries WHERE id = 'weight-20000'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(78.5, cursor.getDouble(0), 0.0001)
        }

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            "INSERT INTO body_sites (id, name, measuringHint, sortOrder, createdAt) " +
                "VALUES ('site-1', 'Oberarm links', 'angespannt, dickste Stelle', 0, 1700000000000)",
        )
        db.execSQL(
            "INSERT INTO body_measurements (id, bodySiteId, epochDay, valueCm, createdAt) " +
                "VALUES ('measurement-site-1-20000', 'site-1', 20000, 35.5, 1700000000000)",
        )
        // One value per site and day: the same day re-measured replaces its row instead of adding one.
        db.execSQL(
            "INSERT OR REPLACE INTO body_measurements (id, bodySiteId, epochDay, valueCm, createdAt) " +
                "VALUES ('measurement-site-1-20000', 'site-1', 20000, 36.0, 1700000000000)",
        )
        db.query("SELECT COUNT(*), MAX(valueCm) FROM body_measurements").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
            assertEquals(36.0, cursor.getDouble(1), 0.0001)
        }
        // Deleting a site takes its measurements with it.
        db.execSQL("DELETE FROM body_sites WHERE id = 'site-1'")
        db.query("SELECT COUNT(*) FROM body_measurements").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate15To16_addsBloodPressureKeyedByDayAndTimeOfDay() {
        val v15 = helper.createDatabase(dbName, 15)
        v15.execSQL(
            "INSERT INTO body_sites (id, name, measuringHint, sortOrder, createdAt) " +
                "VALUES ('site-1', 'Taille', NULL, 0, 1700000000000)",
        )
        v15.close()

        val db = helper.runMigrationsAndValidate(dbName, 16, true, MIGRATION_15_16)

        db.query("SELECT COUNT(*) FROM blood_pressure_entries").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // The Maße tables from the previous step are untouched.
        db.query("SELECT name FROM body_sites WHERE id = 'site-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Taille", cursor.getString(0))
        }

        db.execSQL(
            "INSERT INTO blood_pressure_entries (id, epochDay, timeOfDay, systolic, diastolic, comment, createdAt) " +
                "VALUES ('bloodpressure-20000-MORNING', 20000, 'MORNING', 128.0, 84.0, 'schlecht geschlafen', 1700000000000)",
        )
        // Morning and evening of the same day coexist...
        db.execSQL(
            "INSERT INTO blood_pressure_entries (id, epochDay, timeOfDay, systolic, diastolic, comment, createdAt) " +
                "VALUES ('bloodpressure-20000-EVENING', 20000, 'EVENING', 134.0, 88.0, NULL, 1700000000000)",
        )
        // ...while re-entering the same half of the same day corrects that row.
        db.execSQL(
            "INSERT OR REPLACE INTO blood_pressure_entries " +
                "(id, epochDay, timeOfDay, systolic, diastolic, comment, createdAt) " +
                "VALUES ('bloodpressure-20000-MORNING', 20000, 'MORNING', 126.0, 82.0, NULL, 1700000000000)",
        )
        db.query("SELECT COUNT(*) FROM blood_pressure_entries").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }
        db.query(
            "SELECT systolic, diastolic, comment FROM blood_pressure_entries " +
                "WHERE id = 'bloodpressure-20000-MORNING'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(126.0, cursor.getDouble(0), 0.0001)
            assertEquals(82.0, cursor.getDouble(1), 0.0001)
            assertEquals(true, cursor.isNull(2))
        }
        db.close()
    }

    @Test
    fun migrate16To17_addsFluidQuickAddsCascadingWithTheirType() {
        val v16 = helper.createDatabase(dbName, 16)
        v16.execSQL(
            "INSERT INTO fluid_types (id, name, defaultQuickAddMl, sortOrder, createdAt) " +
                "VALUES ('type-1', 'Wasser', 250.0, 0, 1700000000000)",
        )
        v16.close()

        val db = helper.runMigrationsAndValidate(dbName, 17, true, MIGRATION_16_17)

        // Nothing is seeded: the Schnellauswahl starts empty and is filled by the user.
        db.query("SELECT COUNT(*) FROM fluid_quick_adds").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.execSQL(
            "INSERT INTO fluid_quick_adds (id, fluidTypeId, symbol, amountMl, sortOrder, createdAt) " +
                "VALUES ('quick-1', 'type-1', 'GLASS', 250.0, 0, 1700000000000)",
        )
        db.execSQL(
            "INSERT INTO fluid_quick_adds (id, fluidTypeId, symbol, amountMl, sortOrder, createdAt) " +
                "VALUES ('quick-2', 'type-1', 'ML_100', 100.0, 1, 1700000000000)",
        )
        db.query("SELECT symbol, amountMl FROM fluid_quick_adds WHERE id = 'quick-2'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("ML_100", cursor.getString(0))
            assertEquals(100.0, cursor.getDouble(1), 0.0001)
        }
        // A button is only ever a shortcut to a drink type, so deleting the type takes it along.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM fluid_types WHERE id = 'type-1'")
        db.query("SELECT COUNT(*) FROM fluid_quick_adds").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }
}
