package com.example.prokject2_tracker.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Real Room migrations, one object per version step. Historically this app relied on
 * `fallbackToDestructiveMigration`, which silently wipes a real user's data on every schema
 * change — these replace that with data-preserving upgrades. Table/column shapes are taken
 * verbatim from the committed schema snapshots in `app/schemas/`, substituting the literal
 * table name for Room's `${TABLE_NAME}` export placeholder.
 */
object MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The committed v1 snapshot was overwritten by a later local build before its version was
        // bumped in code, so it can't be trusted as an exact historical shape. Re-assert the full
        // v1/v2 shape (identical on disk) with IF NOT EXISTS so this is a safe no-op if a given
        // install's true v1 already matches — Room only validates the resulting schema after a
        // migration runs, never the starting one, so this defensiveness carries no downside.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_items` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`baseUnit` TEXT NOT NULL, `kcalPer100` REAL NOT NULL, `proteinPer100` REAL NOT NULL, " +
                "`carbsPer100` REAL NOT NULL, `fatPer100` REAL NOT NULL, `servingName` TEXT, `servingAmount` REAL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_name` ON `food_items` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipes` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`servings` REAL NOT NULL, `instructions` TEXT, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recipe_ingredients` (`id` TEXT NOT NULL, `recipeId` TEXT NOT NULL, " +
                "`foodId` TEXT NOT NULL, `amountBaseUnits` REAL NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_foodId` ON `recipe_ingredients` (`foodId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_entries` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `mealType` TEXT NOT NULL, `sourceType` TEXT NOT NULL, " +
                "`sourceId` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `quantity` REAL NOT NULL, " +
                "`quantityUnit` TEXT NOT NULL, `kcal` REAL NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, " +
                "`fat` REAL NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_epochDay` ON `diary_entries` (`epochDay`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diary_entries_sourceType_sourceId` ON `diary_entries` (`sourceType`, `sourceId`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fluid_entries` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `amountMl` REAL NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fluid_entries_epochDay` ON `fluid_entries` (`epochDay`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cardio_sessions` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `activityType` TEXT NOT NULL, `durationMinutes` REAL NOT NULL, " +
                "`distanceKm` REAL, `caloriesBurned` REAL NOT NULL, `note` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cardio_sessions_epochDay` ON `cardio_sessions` (`epochDay`)")
    }
}

object MIGRATION_2_3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `strength_exercises` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`muscleGroup` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_exercises_name` ON `strength_exercises` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `strength_log_entries` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `exerciseName` TEXT NOT NULL, " +
                "`sets` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `note` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_log_entries_epochDay` ON `strength_log_entries` (`epochDay`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_strength_log_entries_exerciseId` ON `strength_log_entries` (`exerciseId`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`archived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_name` ON `habits` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit_check_ins` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_check_ins_epochDay` ON `habit_check_ins` (`epochDay`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_check_ins_habitId_epochDay` ON `habit_check_ins` (`habitId`, `epochDay`)",
        )
    }
}

object MIGRATION_3_4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Room requires a DEFAULT for NOT NULL columns added via ALTER TABLE.
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `saturatedFatPer100` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `sugarPer100` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `fiberPer100` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `saltPer100` REAL NOT NULL DEFAULT 0")
    }
}

object MIGRATION_4_5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fluid_types` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`defaultQuickAddMl` REAL NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fluid_types_name` ON `fluid_types` (`name`)")

        // Seed the 6 fluid types that map 1:1 onto the legacy WATER/COFFEE/TEA/JUICE/SODA/OTHER
        // enum values (verified against FluidRepository.DEFAULT_FLUID_TYPES — the 7th "Milch"
        // entry there was added later purely at the seed-data level and has no legacy counterpart).
        val now = System.currentTimeMillis()
        val seedTypes = listOf(
            Triple("fluidtype-wasser", "Wasser", 250.0),
            Triple("fluidtype-kaffee", "Kaffee", 125.0),
            Triple("fluidtype-tee", "Tee", 200.0),
            Triple("fluidtype-saft", "Saft", 200.0),
            Triple("fluidtype-limonade", "Limonade", 330.0),
            Triple("fluidtype-sonstiges", "Sonstiges", 200.0),
        )
        seedTypes.forEachIndexed { index, (id, name, defaultMl) ->
            db.execSQL(
                "INSERT INTO `fluid_types` (`id`, `name`, `defaultQuickAddMl`, `sortOrder`, `createdAt`) " +
                    "VALUES ('$id', '$name', $defaultMl, $index, $now)",
            )
        }

        // fluid_entries.type (legacy enum) -> fluidTypeId/fluidTypeName snapshot. SQLite can't drop
        // a column or add a NOT NULL column referencing derived data in place, so create-copy-drop-rename.
        db.execSQL(
            "CREATE TABLE `fluid_entries_new` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `fluidTypeId` TEXT NOT NULL, `fluidTypeName` TEXT NOT NULL, " +
                "`amountMl` REAL NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `fluid_entries_new` (`id`, `epochDay`, `createdAt`, `fluidTypeId`, `fluidTypeName`, `amountMl`) " +
                "SELECT `id`, `epochDay`, `createdAt`, " +
                "CASE `type` " +
                "WHEN 'WATER' THEN 'fluidtype-wasser' " +
                "WHEN 'COFFEE' THEN 'fluidtype-kaffee' " +
                "WHEN 'TEA' THEN 'fluidtype-tee' " +
                "WHEN 'JUICE' THEN 'fluidtype-saft' " +
                "WHEN 'SODA' THEN 'fluidtype-limonade' " +
                "ELSE 'fluidtype-sonstiges' END, " +
                "CASE `type` " +
                "WHEN 'WATER' THEN 'Wasser' " +
                "WHEN 'COFFEE' THEN 'Kaffee' " +
                "WHEN 'TEA' THEN 'Tee' " +
                "WHEN 'JUICE' THEN 'Saft' " +
                "WHEN 'SODA' THEN 'Limonade' " +
                "ELSE 'Sonstiges' END, " +
                "`amountMl` FROM `fluid_entries`",
        )
        db.execSQL("DROP TABLE `fluid_entries`")
        db.execSQL("ALTER TABLE `fluid_entries_new` RENAME TO `fluid_entries`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fluid_entries_epochDay` ON `fluid_entries` (`epochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fluid_entries_fluidTypeId` ON `fluid_entries` (`fluidTypeId`)")
    }
}

object MIGRATION_5_6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `fluid_entries` ADD COLUMN `fluidUnitId` TEXT")
        db.execSQL("ALTER TABLE `fluid_entries` ADD COLUMN `fluidUnitName` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fluid_entries_fluidUnitId` ON `fluid_entries` (`fluidUnitId`)")

        db.execSQL("ALTER TABLE `fluid_types` ADD COLUMN `dailyGoalMinMl` REAL")
        db.execSQL("ALTER TABLE `fluid_types` ADD COLUMN `dailyGoalMaxMl` REAL")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fluid_units` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`amountMl` REAL NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fluid_units_name` ON `fluid_units` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_item_tags` (`foodItemId` TEXT NOT NULL, `tagId` TEXT NOT NULL, " +
                "PRIMARY KEY(`foodItemId`, `tagId`), " +
                "FOREIGN KEY(`foodItemId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_item_tags_foodItemId` ON `food_item_tags` (`foodItemId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_item_tags_tagId` ON `food_item_tags` (`tagId`)")
    }
}

object MIGRATION_6_7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // food_items: brand (nullable, no default needed)
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `brand` TEXT")

        // habits: type (NOT NULL, defaults existing rows to YES_NO — the only type that existed before)
        db.execSQL("ALTER TABLE `habits` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'YES_NO'")

        // habit_check_ins: value (nullable)
        db.execSQL("ALTER TABLE `habit_check_ins` ADD COLUMN `value` REAL")

        // habit_goals: new table, FK CASCADE to habits
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit_goals` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, " +
                "`period` TEXT NOT NULL, `targetValue` REAL NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_goals_habitId` ON `habit_goals` (`habitId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_goals_habitId_period` ON `habit_goals` (`habitId`, `period`)",
        )

        // body_weight_entries: new table
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `body_weight_entries` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`weightKg` REAL NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_body_weight_entries_epochDay` ON `body_weight_entries` (`epochDay`)",
        )

        // cardio_activity_types: new table + 7 seeded defaults. Must match
        // CardioRepository.DEFAULT_CARDIO_ACTIVITY_TYPES exactly (Laufen/Radfahren/Schwimmen/Gehen/
        // Wandern/Rudern/Sonstiges) so ensureDefaultActivityTypesSeeded()'s isNotEmpty() check no-ops
        // afterwards instead of double-seeding.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cardio_activity_types` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cardio_activity_types_name` ON `cardio_activity_types` (`name`)")

        val now = System.currentTimeMillis()
        val activityTypes = listOf(
            "cardiotype-laufen" to "Laufen",
            "cardiotype-radfahren" to "Radfahren",
            "cardiotype-schwimmen" to "Schwimmen",
            "cardiotype-gehen" to "Gehen",
            "cardiotype-wandern" to "Wandern",
            "cardiotype-rudern" to "Rudern",
            "cardiotype-sonstiges" to "Sonstiges",
        )
        activityTypes.forEachIndexed { index, (id, name) ->
            db.execSQL(
                "INSERT INTO `cardio_activity_types` (`id`, `name`, `sortOrder`, `createdAt`) " +
                    "VALUES ('$id', '$name', $index, $now)",
            )
        }

        // cardio_sessions: legacy `activityType` enum -> activityTypeId/activityTypeName snapshot,
        // caloriesBurned NOT NULL -> nullable, + new avgHeartRateBpm. SQLite can't drop a column or
        // add a NOT NULL column referencing derived data in place, so create-copy-drop-rename.
        db.execSQL(
            "CREATE TABLE `cardio_sessions_new` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `activityTypeId` TEXT NOT NULL, `activityTypeName` TEXT NOT NULL, " +
                "`durationMinutes` REAL NOT NULL, `distanceKm` REAL, `caloriesBurned` REAL, " +
                "`avgHeartRateBpm` INTEGER, `note` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `cardio_sessions_new` (`id`, `epochDay`, `createdAt`, `activityTypeId`, `activityTypeName`, " +
                "`durationMinutes`, `distanceKm`, `caloriesBurned`, `avgHeartRateBpm`, `note`) " +
                "SELECT `id`, `epochDay`, `createdAt`, " +
                "CASE `activityType` " +
                "WHEN 'RUNNING' THEN 'cardiotype-laufen' " +
                "WHEN 'CYCLING' THEN 'cardiotype-radfahren' " +
                "WHEN 'SWIMMING' THEN 'cardiotype-schwimmen' " +
                "WHEN 'WALKING' THEN 'cardiotype-gehen' " +
                "WHEN 'HIKING' THEN 'cardiotype-wandern' " +
                "WHEN 'ROWING' THEN 'cardiotype-rudern' " +
                "ELSE 'cardiotype-sonstiges' END, " +
                "CASE `activityType` " +
                "WHEN 'RUNNING' THEN 'Laufen' " +
                "WHEN 'CYCLING' THEN 'Radfahren' " +
                "WHEN 'SWIMMING' THEN 'Schwimmen' " +
                "WHEN 'WALKING' THEN 'Gehen' " +
                "WHEN 'HIKING' THEN 'Wandern' " +
                "WHEN 'ROWING' THEN 'Rudern' " +
                "ELSE 'Sonstiges' END, " +
                "`durationMinutes`, `distanceKm`, `caloriesBurned`, NULL, `note` FROM `cardio_sessions`",
        )
        db.execSQL("DROP TABLE `cardio_sessions`")
        db.execSQL("ALTER TABLE `cardio_sessions_new` RENAME TO `cardio_sessions`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cardio_sessions_epochDay` ON `cardio_sessions` (`epochDay`)")
    }
}

object MIGRATION_7_8 : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // muscle_groups: new table + 8 seeded defaults. Must match
        // StrengthExerciseRepository.DEFAULT_MUSCLE_GROUPS exactly (Brust/Rücken/Beine/Schultern/
        // Arme/Rumpf/Ganzkörper/Sonstiges) so ensureDefaultMuscleGroupsSeeded()'s isNotEmpty() check
        // no-ops afterwards instead of double-seeding.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `muscle_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_muscle_groups_name` ON `muscle_groups` (`name`)")

        val now = System.currentTimeMillis()
        val muscleGroups = listOf(
            "musclegroup-brust" to "Brust",
            "musclegroup-ruecken" to "Rücken",
            "musclegroup-beine" to "Beine",
            "musclegroup-schultern" to "Schultern",
            "musclegroup-arme" to "Arme",
            "musclegroup-rumpf" to "Rumpf",
            "musclegroup-ganzkoerper" to "Ganzkörper",
            "musclegroup-sonstiges" to "Sonstiges",
        )
        muscleGroups.forEachIndexed { index, (id, name) ->
            db.execSQL(
                "INSERT INTO `muscle_groups` (`id`, `name`, `sortOrder`, `createdAt`) " +
                    "VALUES ('$id', '$name', $index, $now)",
            )
        }

        // strength_exercises: legacy `muscleGroup` enum -> muscleGroupId/muscleGroupName snapshot.
        // SQLite can't drop a column or add a NOT NULL column referencing derived data in place,
        // so create-copy-drop-rename (same idiom as cardio_sessions in MIGRATION_6_7).
        db.execSQL(
            "CREATE TABLE `strength_exercises_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`muscleGroupId` TEXT NOT NULL, `muscleGroupName` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `strength_exercises_new` (`id`, `name`, `muscleGroupId`, `muscleGroupName`, " +
                "`createdAt`, `updatedAt`) " +
                "SELECT `id`, `name`, " +
                "CASE `muscleGroup` " +
                "WHEN 'CHEST' THEN 'musclegroup-brust' " +
                "WHEN 'BACK' THEN 'musclegroup-ruecken' " +
                "WHEN 'LEGS' THEN 'musclegroup-beine' " +
                "WHEN 'SHOULDERS' THEN 'musclegroup-schultern' " +
                "WHEN 'ARMS' THEN 'musclegroup-arme' " +
                "WHEN 'CORE' THEN 'musclegroup-rumpf' " +
                "WHEN 'FULL_BODY' THEN 'musclegroup-ganzkoerper' " +
                "ELSE 'musclegroup-sonstiges' END, " +
                "CASE `muscleGroup` " +
                "WHEN 'CHEST' THEN 'Brust' " +
                "WHEN 'BACK' THEN 'Rücken' " +
                "WHEN 'LEGS' THEN 'Beine' " +
                "WHEN 'SHOULDERS' THEN 'Schultern' " +
                "WHEN 'ARMS' THEN 'Arme' " +
                "WHEN 'CORE' THEN 'Rumpf' " +
                "WHEN 'FULL_BODY' THEN 'Ganzkörper' " +
                "ELSE 'Sonstiges' END, " +
                "`createdAt`, `updatedAt` FROM `strength_exercises`",
        )
        db.execSQL("DROP TABLE `strength_exercises`")
        db.execSQL("ALTER TABLE `strength_exercises_new` RENAME TO `strength_exercises`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_exercises_name` ON `strength_exercises` (`name`)")

        // strength_sets: new table. Backfilled below from the still-old-shaped strength_log_entries
        // (sets/reps/weightKg not yet dropped) joined against the now-restructured strength_exercises
        // for muscleGroupId, expanding each entry's `sets` count into that many individual rows with
        // the same reps/weightKg (legacy data has no per-set variation).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `strength_sets` (`id` TEXT NOT NULL, `logEntryId` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `muscleGroupId` TEXT NOT NULL, " +
                "`setIndex` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`logEntryId`) REFERENCES `strength_log_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_sets_logEntryId` ON `strength_sets` (`logEntryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_sets_epochDay` ON `strength_sets` (`epochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_sets_exerciseId` ON `strength_sets` (`exerciseId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_strength_sets_muscleGroupId` ON `strength_sets` (`muscleGroupId`)",
        )

        db.query(
            "SELECT sle.id, sle.epochDay, sle.exerciseId, sle.sets, sle.reps, sle.weightKg, se.muscleGroupId " +
                "FROM strength_log_entries sle JOIN strength_exercises se ON se.id = sle.exerciseId",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val logEntryId = cursor.getString(0)
                val epochDay = cursor.getLong(1)
                val exerciseId = cursor.getString(2)
                val sets = cursor.getInt(3)
                val reps = cursor.getInt(4)
                val weightKg = cursor.getDouble(5)
                val muscleGroupId = cursor.getString(6)
                for (setIndex in 0 until sets) {
                    db.execSQL(
                        "INSERT INTO `strength_sets` (`id`, `logEntryId`, `epochDay`, `exerciseId`, " +
                            "`muscleGroupId`, `setIndex`, `reps`, `weightKg`) VALUES " +
                            "('$logEntryId-set-$setIndex', '$logEntryId', $epochDay, '$exerciseId', " +
                            "'$muscleGroupId', $setIndex, $reps, $weightKg)",
                    )
                }
            }
        }

        // strength_log_entries: drop sets/reps/weightKg, now represented by strength_sets rows.
        db.execSQL(
            "CREATE TABLE `strength_log_entries_new` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `exerciseName` TEXT NOT NULL, " +
                "`note` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `strength_log_entries_new` (`id`, `epochDay`, `createdAt`, `exerciseId`, " +
                "`exerciseName`, `note`) " +
                "SELECT `id`, `epochDay`, `createdAt`, `exerciseId`, `exerciseName`, `note` " +
                "FROM `strength_log_entries`",
        )
        db.execSQL("DROP TABLE `strength_log_entries`")
        db.execSQL("ALTER TABLE `strength_log_entries_new` RENAME TO `strength_log_entries`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_strength_log_entries_epochDay` ON `strength_log_entries` (`epochDay`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_strength_log_entries_exerciseId` ON `strength_log_entries` (`exerciseId`)",
        )

        // fitness_goals: new table, empty (no seed data — user-configured).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fitness_goals` (`id` TEXT NOT NULL, `metric` TEXT NOT NULL, " +
                "`period` TEXT NOT NULL, `muscleGroupId` TEXT, `targetValue` REAL NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}

object MIGRATION_8_9 : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // strength_exercise_muscle_groups: new join table (an exercise can now target several
        // muscle groups instead of exactly one). Created before the backfill below so it can be
        // populated from the still-old-shaped strength_exercises (muscleGroupId not yet dropped).
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `strength_exercise_muscle_groups` (`exerciseId` TEXT NOT NULL, " +
                "`muscleGroupId` TEXT NOT NULL, PRIMARY KEY(`exerciseId`, `muscleGroupId`), " +
                "FOREIGN KEY(`exerciseId`) REFERENCES `strength_exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`muscleGroupId`) REFERENCES `muscle_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_strength_exercise_muscle_groups_exerciseId` " +
                "ON `strength_exercise_muscle_groups` (`exerciseId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_strength_exercise_muscle_groups_muscleGroupId` " +
                "ON `strength_exercise_muscle_groups` (`muscleGroupId`)",
        )
        db.execSQL(
            "INSERT INTO `strength_exercise_muscle_groups` (`exerciseId`, `muscleGroupId`) " +
                "SELECT `id`, `muscleGroupId` FROM `strength_exercises`",
        )

        // strength_exercises: drop muscleGroupId/muscleGroupName, now represented by the join table.
        db.execSQL(
            "CREATE TABLE `strength_exercises_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `strength_exercises_new` (`id`, `name`, `createdAt`, `updatedAt`) " +
                "SELECT `id`, `name`, `createdAt`, `updatedAt` FROM `strength_exercises`",
        )
        db.execSQL("DROP TABLE `strength_exercises`")
        db.execSQL("ALTER TABLE `strength_exercises_new` RENAME TO `strength_exercises`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_exercises_name` ON `strength_exercises` (`name`)")

        // strength_sets: drop muscleGroupId — "sets per muscle group" is now computed by joining
        // exerciseId against strength_exercise_muscle_groups (an exercise's current assignment),
        // since a set's exercise can target more than one group.
        db.execSQL(
            "CREATE TABLE `strength_sets_new` (`id` TEXT NOT NULL, `logEntryId` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL, `setIndex` INTEGER NOT NULL, " +
                "`reps` INTEGER NOT NULL, `weightKg` REAL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`logEntryId`) REFERENCES `strength_log_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO `strength_sets_new` (`id`, `logEntryId`, `epochDay`, `exerciseId`, `setIndex`, " +
                "`reps`, `weightKg`) " +
                "SELECT `id`, `logEntryId`, `epochDay`, `exerciseId`, `setIndex`, `reps`, `weightKg` " +
                "FROM `strength_sets`",
        )
        db.execSQL("DROP TABLE `strength_sets`")
        db.execSQL("ALTER TABLE `strength_sets_new` RENAME TO `strength_sets`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_sets_logEntryId` ON `strength_sets` (`logEntryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_sets_epochDay` ON `strength_sets` (`epochDay`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_sets_exerciseId` ON `strength_sets` (`exerciseId`)")
    }
}

object MIGRATION_9_10 : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // food_items: optional link into the Getränkearten library, so logging a drink-like
        // Lebensmittel to the diary also logs the fluid. Both nullable — existing foods keep
        // "no fluid" semantics without a backfill.
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `fluidTypeId` TEXT")
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `fluidMlPer100` REAL")

        // fluid_types: user-chosen pie-chart colour (packed ARGB); null falls back to a palette slot.
        db.execSQL("ALTER TABLE `fluid_types` ADD COLUMN `colorArgb` INTEGER")

        // fluid_entries: back-reference to the diary entry that produced this row (null for
        // everything logged directly in the Flüssigkeiten tab), so it can follow that entry's
        // edits and deletions.
        db.execSQL("ALTER TABLE `fluid_entries` ADD COLUMN `sourceDiaryEntryId` TEXT")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fluid_entries_sourceDiaryEntryId` " +
                "ON `fluid_entries` (`sourceDiaryEntryId`)",
        )
    }
}

object MIGRATION_10_11 : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // diary_entries: the servings a logged Rezept was divided into, snapshotted alongside the
        // nutrition. Nullable — existing recipe entries fall back to the library recipe's current
        // value, so no backfill is needed (and none would be correct for a since-edited recipe).
        db.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `recipeServings` REAL")

        // diary_recipe_ingredients: new table, empty. Holds a single diary entry's own copy of a
        // recipe's ingredients ("I made it differently that day"); rows only appear once the user
        // actually changes one, so nothing to migrate into it.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_recipe_ingredients` (`id` TEXT NOT NULL, " +
                "`diaryEntryId` TEXT NOT NULL, `foodId` TEXT NOT NULL, `amountBaseUnits` REAL NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`diaryEntryId`) REFERENCES `diary_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diary_recipe_ingredients_diaryEntryId` " +
                "ON `diary_recipe_ingredients` (`diaryEntryId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_diary_recipe_ingredients_foodId` " +
                "ON `diary_recipe_ingredients` (`foodId`)",
        )
    }
}

object MIGRATION_11_12 : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // diary_entries: the four nutrients beyond the macros, so the Tagebuch can show a progress
        // bar for every nutrient a goal can be set for. Existing rows default to 0 rather than being
        // backfilled from their source food: the entry's numbers are a snapshot of the food *as it
        // was logged*, and the food's sugar/salt values may well have been edited since. 0 honestly
        // says "not recorded" instead of inventing history.
        db.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `saturatedFat` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `sugar` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `fiber` REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `salt` REAL NOT NULL DEFAULT 0")
    }
}

object MIGRATION_12_13 : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // strength_exercises: the movement pattern an exercise trains (PUSH/PULL/ISOMETRIC),
        // tagged alongside its muscle groups. Nullable — existing exercises predate the tag and
        // guessing a direction from the name would invent data the user never entered.
        db.execSQL("ALTER TABLE `strength_exercises` ADD COLUMN `movementDirection` TEXT")

        // fitness_goals: the movement direction a STRENGTH_SETS_MOVEMENT_DIRECTION goal targets,
        // mirroring the existing nullable `muscleGroupId` discriminator. Null for every other metric.
        db.execSQL("ALTER TABLE `fitness_goals` ADD COLUMN `movementDirection` TEXT")
    }
}

object MIGRATION_13_14 : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // food_units: a food's named amounts ("Scheibe" = 25 g), replacing the single optional
        // serving that lived on food_items as servingName/servingAmount.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_units` (`id` TEXT NOT NULL, `foodItemId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `amountBaseUnits` REAL NOT NULL, `sortOrder` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`foodItemId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_units_foodItemId` ON `food_units` (`foodItemId`)")

        // Every food that had a serving keeps it as its first unit. The id is derived from the food's
        // so it stays stable if this runs against a database that already imported such a backup.
        db.execSQL(
            "INSERT OR IGNORE INTO `food_units` (`id`, `foodItemId`, `name`, `amountBaseUnits`, `sortOrder`) " +
                "SELECT `id` || '-serving', `id`, `servingName`, `servingAmount`, 0 FROM `food_items` " +
                "WHERE `servingName` IS NOT NULL AND `servingName` != '' " +
                "AND `servingAmount` IS NOT NULL AND `servingAmount` > 0",
        )

        // food_items: drop servingName/servingAmount now that they live in food_units. SQLite can't
        // drop a column in place, so create-copy-drop-rename (as in MIGRATION_6_7/7_8).
        db.execSQL(
            "CREATE TABLE `food_items_new` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `brand` TEXT, " +
                "`baseUnit` TEXT NOT NULL, `kcalPer100` REAL NOT NULL, `proteinPer100` REAL NOT NULL, " +
                "`carbsPer100` REAL NOT NULL, `fatPer100` REAL NOT NULL, `saturatedFatPer100` REAL NOT NULL, " +
                "`sugarPer100` REAL NOT NULL, `fiberPer100` REAL NOT NULL, `saltPer100` REAL NOT NULL, " +
                "`fluidTypeId` TEXT, `fluidMlPer100` REAL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `food_items_new` (`id`, `name`, `brand`, `baseUnit`, `kcalPer100`, `proteinPer100`, " +
                "`carbsPer100`, `fatPer100`, `saturatedFatPer100`, `sugarPer100`, `fiberPer100`, `saltPer100`, " +
                "`fluidTypeId`, `fluidMlPer100`, `createdAt`, `updatedAt`) " +
                "SELECT `id`, `name`, `brand`, `baseUnit`, `kcalPer100`, `proteinPer100`, `carbsPer100`, " +
                "`fatPer100`, `saturatedFatPer100`, `sugarPer100`, `fiberPer100`, `saltPer100`, " +
                "`fluidTypeId`, `fluidMlPer100`, `createdAt`, `updatedAt` FROM `food_items`",
        )
        db.execSQL("DROP TABLE `food_items`")
        db.execSQL("ALTER TABLE `food_items_new` RENAME TO `food_items`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_items_name` ON `food_items` (`name`)")

        // The three places an amount is recorded gain the "entered as 2 × Scheibe" snapshot next to
        // the base-unit amount they already store. Nullable: everything logged so far was typed in
        // grams, and that is exactly what null means.
        listOf("diary_entries", "recipe_ingredients", "diary_recipe_ingredients").forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `unitName` TEXT")
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `unitCount` REAL")
        }
    }
}

object MIGRATION_14_15 : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // body_sites: the Körperstellen library ("Oberarm links") plus the note on how that spot is
        // measured. Deliberately not seeded — unlike muscle groups or cardio types, which spots
        // someone tracks is personal, so the screen starts empty and every row is user-created.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `body_sites` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`measuringHint` TEXT, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_body_sites_name` ON `body_sites` (`name`)")

        // body_measurements: one value per site and day, in cm. The unique (site, day) index backs
        // the deterministic id "measurement-<site>-<day>", so re-measuring a day corrects that row
        // instead of adding a second point.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `body_measurements` (`id` TEXT NOT NULL, `bodySiteId` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `valueCm` REAL NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`bodySiteId`) REFERENCES `body_sites`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_body_measurements_bodySiteId_epochDay` " +
                "ON `body_measurements` (`bodySiteId`, `epochDay`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_body_measurements_epochDay` ON `body_measurements` (`epochDay`)",
        )
    }
}

object MIGRATION_15_16 : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // blood_pressure_entries: systolisch/diastolisch are fixed columns rather than rows of a
        // user-managed library — unlike body_sites, a cuff measures exactly these two values. The
        // unique (day, timeOfDay) index backs the deterministic id, so re-entering this morning's
        // reading corrects it instead of adding a second point.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `blood_pressure_entries` (`id` TEXT NOT NULL, " +
                "`epochDay` INTEGER NOT NULL, `timeOfDay` TEXT NOT NULL, `systolic` REAL NOT NULL, " +
                "`diastolic` REAL NOT NULL, `comment` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_blood_pressure_entries_epochDay_timeOfDay` " +
                "ON `blood_pressure_entries` (`epochDay`, `timeOfDay`)",
        )
    }
}

object MIGRATION_16_17 : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // fluid_quick_adds: the Tagebuch's Schnellauswahl — which drink is offered with which
        // symbol, and how much one tap logs. A row is only ever a shortcut to a fluid_types row, so
        // it cascades with it: a button pointing at a deleted drink type has nothing left to log.
        // Not seeded — which drinks are worth a one-tap button is personal, and an unasked-for row
        // of buttons in the Tagebuch is worse than an empty area that says how to fill it.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fluid_quick_adds` (`id` TEXT NOT NULL, " +
                "`fluidTypeId` TEXT NOT NULL, `symbol` TEXT NOT NULL, `amountMl` REAL NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`fluidTypeId`) REFERENCES `fluid_types`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_fluid_quick_adds_fluidTypeId` " +
                "ON `fluid_quick_adds` (`fluidTypeId`)",
        )
    }
}

object MIGRATION_17_18 : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // food_items: what a Lebensmittel costs. `price` is nullable rather than defaulting to 0,
        // because "kein Preis erfasst" and "kostet nichts" are different statements and every food
        // that exists today is the former. `priceUnitName` says what the price is *for*: NULL means
        // 100 g/ml, otherwise it names one of the food's food_units rows ("Packung"). The name, not
        // the unit id, since food_units rows are recreated with fresh ids on every save.
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `price` REAL")
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `priceUnitName` TEXT")
    }
}

object MIGRATION_18_19 : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Bodyweight becomes a fact of its own instead of being encoded as "no weight". A set now
        // says whether the body was the load, and `weightKg` means the *external* weight either way
        // — which is what makes weighted pull-ups (bodyweight + 10 kg) expressible at all.
        db.execSQL("ALTER TABLE `strength_sets` ADD COLUMN `isBodyweight` INTEGER NOT NULL DEFAULT 0")
        // A NULL weight is exactly what bodyweight used to mean, so every such set carries over.
        db.execSQL("UPDATE `strength_sets` SET `isBodyweight` = 1 WHERE `weightKg` IS NULL")

        // The exercise-level flag decides what logging *starts* in. Inferred from the log rather
        // than left off for everyone: an exercise whose every logged set was bodyweight is a
        // bodyweight exercise, and asking the user to re-tag Klimmzüge they have logged for months
        // would be busywork. Exercises never logged stay off — there is nothing to infer from.
        db.execSQL("ALTER TABLE `strength_exercises` ADD COLUMN `isBodyweight` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE `strength_exercises` SET `isBodyweight` = 1 WHERE `id` IN (" +
                "SELECT `exerciseId` FROM `strength_sets` GROUP BY `exerciseId` " +
                "HAVING COUNT(*) = SUM(CASE WHEN `weightKg` IS NULL THEN 1 ELSE 0 END))",
        )
    }
}

object MIGRATION_19_20 : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // sleep_entries: one night per row, keyed by the *morning* it ended (unique index behind the
        // deterministic id "sleep-<day>", the same idempotent-logging idea as body_weight_entries).
        // Times are minutes since midnight rather than timestamps: a night is "23:10 bis 6:45", and
        // the date it belongs to is already the row's key.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_entries` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, " +
                "`morningFitness` INTEGER, `lastMealMinuteOfDay` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_entries_epochDay` ON `sleep_entries` (`epochDay`)")

        // sleep_tags: the user's own labels ("heiß", "viel geträumt"). Not seeded — which of them
        // are worth a tap is personal, and an unasked-for row of tags is worse than an empty one
        // next to a field that says how to fill it.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_tags_name` ON `sleep_tags` (`name`)")

        // The join table cascades from both sides: a deleted night takes its labels off, and a
        // deleted tag comes off every night without touching the nights themselves.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sleep_entry_tags` (`sleepEntryId` TEXT NOT NULL, `tagId` TEXT NOT NULL, " +
                "PRIMARY KEY(`sleepEntryId`, `tagId`), " +
                "FOREIGN KEY(`sleepEntryId`) REFERENCES `sleep_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tagId`) REFERENCES `sleep_tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_entry_tags_sleepEntryId` ON `sleep_entry_tags` (`sleepEntryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sleep_entry_tags_tagId` ON `sleep_entry_tags` (`tagId`)")
    }
}

object MIGRATION_20_21 : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // tasks: the *rule*, not the occurrences. A recurring task is one row for good — which of
        // its due dates are done lives in task_completions below, so working through a rhythm never
        // rewrites the task itself. `startEpochDay` anchors every rhythm (and is the date itself
        // for a one-off), which is what makes "alle 3 Wochen" a phase rather than a guess.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`recurrence` TEXT NOT NULL, `intervalCount` INTEGER NOT NULL, " +
                "`weekdayMask` INTEGER NOT NULL, `dayOfMonth` INTEGER NOT NULL, " +
                "`startEpochDay` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_name` ON `tasks` (`name`)")

        // task_completions: one ticked-off due date. Keyed by the day it was *due* rather than the
        // day of the tap, so catching up on a backlog settles the occurrence it was meant for;
        // `completedEpochDay` keeps the day it actually happened, which is what lets the Tagesziele
        // still show a task as done today after the tick.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `task_completions` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, " +
                "`dueEpochDay` INTEGER NOT NULL, `completedEpochDay` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_completions_taskId_dueEpochDay` " +
                "ON `task_completions` (`taskId`, `dueEpochDay`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_task_completions_completedEpochDay` " +
                "ON `task_completions` (`completedEpochDay`)",
        )
    }
}

object MIGRATION_21_22 : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Sleep entries can now mark "did not sleep" nights without recording times.
        // Make start/end times nullable and add didNotSleep flag.
        // Can't alter column to nullable directly, so recreate the table.
        db.execSQL(
            "CREATE TABLE `sleep_entries_new` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`startMinuteOfDay` INTEGER, `endMinuteOfDay` INTEGER, " +
                "`morningFitness` INTEGER, `lastMealMinuteOfDay` INTEGER, " +
                "`didNotSleep` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        // Copy existing data, assuming startMinuteOfDay and endMinuteOfDay were never NULL before
        db.execSQL(
            "INSERT INTO `sleep_entries_new` " +
                "SELECT `id`, `epochDay`, `startMinuteOfDay`, `endMinuteOfDay`, " +
                "`morningFitness`, `lastMealMinuteOfDay`, 0, `createdAt` FROM `sleep_entries`",
        )
        db.execSQL("DROP TABLE `sleep_entries`")
        db.execSQL("ALTER TABLE `sleep_entries_new` RENAME TO `sleep_entries`")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sleep_entries_epochDay` ON `sleep_entries` (`epochDay`)")

        // Nap entries: daytime naps (Mittagsschlaf) with similar structure to sleep entries.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `nap_entries` (`id` TEXT NOT NULL, `epochDay` INTEGER NOT NULL, " +
                "`startMinuteOfDay` INTEGER NOT NULL, `endMinuteOfDay` INTEGER NOT NULL, " +
                "`refreshmentFitness` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_nap_entries_epochDay` ON `nap_entries` (`epochDay`)")
    }
}

object MIGRATION_22_23 : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // tag_hierarchy: parent-child relationships between tags (e.g. Vegan → Vegetarisch).
        // Empty table at first; relationships are user-created via the UI.
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tag_hierarchy` (`parentTagId` TEXT NOT NULL, `childTagId` TEXT NOT NULL, " +
                "PRIMARY KEY(`parentTagId`, `childTagId`), " +
                "FOREIGN KEY(`parentTagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`childTagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_hierarchy_parentTagId` ON `tag_hierarchy` (`parentTagId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tag_hierarchy_childTagId` ON `tag_hierarchy` (`childTagId`)")
    }
}
