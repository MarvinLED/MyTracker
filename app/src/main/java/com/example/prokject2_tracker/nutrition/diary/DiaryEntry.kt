package com.example.prokject2_tracker.nutrition.diary

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }

/**
 * Diary entries can log a Lebensmittel, a Rezept, or a one-off [QUICK] "Schnelleintrag" that has no
 * library source at all. Extend with new cases as needed.
 */
enum class DiarySourceType { FOOD, RECIPE, QUICK }

/**
 * One logged consumption. Nutrition (kcal/protein/carbs/fat) and [sourceName]/[quantityUnit] are
 * snapshotted at logging time so editing or deleting the source food/recipe later never changes
 * history.
 */
@Entity(tableName = "diary_entries", indices = [Index("epochDay"), Index("sourceType", "sourceId")])
data class DiaryEntry(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val createdAt: Instant,
    val mealType: MealType,
    val sourceType: DiarySourceType,
    /** Empty for [DiarySourceType.QUICK] — a Schnelleintrag references no library item. */
    val sourceId: String,
    val sourceName: String,
    /** FOOD: amount in the source's base unit. RECIPE: number of servings. QUICK: always 1. */
    val quantity: Double,
    val quantityUnit: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)
