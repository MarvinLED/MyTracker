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
}
