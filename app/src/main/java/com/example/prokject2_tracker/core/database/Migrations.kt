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
