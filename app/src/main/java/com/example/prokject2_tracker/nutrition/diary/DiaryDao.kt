package com.example.prokject2_tracker.nutrition.diary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.prokject2_tracker.nutrition.NutritionTotals
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyKcalTotal(val epochDay: Long, val value: Double)
data class DailyProteinTotal(val epochDay: Long, val value: Double)
data class DailyCarbsTotal(val epochDay: Long, val value: Double)
data class DailyFatTotal(val epochDay: Long, val value: Double)

/** One source's most recent logging: which day, and under which MealType — the "Zuletzt gegessen" signal. */
data class LastLoggedSource(
    val sourceType: DiarySourceType,
    val sourceId: String,
    val mealType: MealType,
    val epochDay: Long,
    val createdAt: Instant,
)

/** One source's most recent amount and unit — for pre-filling quantity when adding. */
data class LastLoggedAmount(
    val quantity: Double,
    val quantityUnit: String,
    val unitName: String?,
    val unitCount: Double?,
)

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE epochDay = :epochDay ORDER BY mealType, createdAt")
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>>

    /** The same rows as [observeForDay], for a one-off read — copying a Tageszeit, say. */
    @Query("SELECT * FROM diary_entries WHERE epochDay = :epochDay ORDER BY mealType, createdAt")
    suspend fun getForDay(epochDay: Long): List<DiaryEntry>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: String): DiaryEntry?

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM diary_entries WHERE epochDay = :epochDay")
    fun observeDayTotalKcal(epochDay: Long): Flow<Double>

    /** Every goal-able nutrient for one day in a single query, rather than one flow per nutrient. */
    @Query(
        "SELECT COALESCE(SUM(kcal), 0) AS kcal, COALESCE(SUM(protein), 0) AS protein, " +
            "COALESCE(SUM(carbs), 0) AS carbs, COALESCE(SUM(fat), 0) AS fat, " +
            "COALESCE(SUM(saturatedFat), 0) AS saturatedFat, COALESCE(SUM(sugar), 0) AS sugar, " +
            "COALESCE(SUM(fiber), 0) AS fiber, COALESCE(SUM(salt), 0) AS salt " +
            "FROM diary_entries WHERE epochDay = :epochDay",
    )
    fun observeDayNutritionTotals(epochDay: Long): Flow<NutritionTotals>

    @Query(
        "SELECT epochDay, SUM(kcal) AS value FROM diary_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyKcalTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyKcalTotal>>

    @Query(
        "SELECT epochDay, SUM(protein) AS value FROM diary_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyProteinTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyProteinTotal>>

    @Query(
        "SELECT epochDay, SUM(carbs) AS value FROM diary_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyCarbsTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyCarbsTotal>>

    @Query(
        "SELECT epochDay, SUM(fat) AS value FROM diary_entries " +
            "WHERE epochDay BETWEEN :startInclusive AND :endInclusive " +
            "GROUP BY epochDay ORDER BY epochDay",
    )
    fun observeDailyFatTotals(startInclusive: Long, endInclusive: Long): Flow<List<DailyFatTotal>>

    /**
     * The most recent diary_entries row per (sourceType, sourceId), Schnelleinträge excluded (they have
     * no sourceId to group by). May return more than one row for a source if it was logged more than
     * once on its single most recent day (e.g. breakfast and dinner the same day) — the caller collapses
     * that by createdAt, the same tiebreaker [observeForDay] already sorts by.
     */
    @Query(
        "SELECT de.sourceType, de.sourceId, de.mealType, de.epochDay, de.createdAt FROM diary_entries de " +
            "JOIN (SELECT sourceType, sourceId, MAX(epochDay) AS lastDay FROM diary_entries " +
            "WHERE sourceType != :excludedType GROUP BY sourceType, sourceId) latest " +
            "ON latest.sourceType = de.sourceType AND latest.sourceId = de.sourceId AND latest.lastDay = de.epochDay " +
            "WHERE de.sourceType != :excludedType",
    )
    fun observeLastLoggedPerSource(excludedType: DiarySourceType): Flow<List<LastLoggedSource>>

    /** The most recent quantity and unit for a specific source, for pre-filling when adding. */
    @Query(
        "SELECT quantity, quantityUnit, unitName, unitCount FROM diary_entries " +
            "WHERE sourceType = :sourceType AND sourceId = :sourceId " +
            "ORDER BY epochDay DESC, createdAt DESC LIMIT 1",
    )
    suspend fun getLastLoggedAmount(sourceType: DiarySourceType, sourceId: String): LastLoggedAmount?

    @Upsert
    suspend fun upsert(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)

    @Transaction
    @Query("SELECT * FROM diary_recipe_ingredients WHERE diaryEntryId = :diaryEntryId ORDER BY sortOrder")
    fun observeRecipeIngredients(diaryEntryId: String): Flow<List<DiaryRecipeIngredientWithFood>>

    @Transaction
    @Query("SELECT * FROM diary_recipe_ingredients WHERE diaryEntryId = :diaryEntryId ORDER BY sortOrder")
    suspend fun getRecipeIngredients(diaryEntryId: String): List<DiaryRecipeIngredientWithFood>

    @Insert
    suspend fun insertRecipeIngredients(ingredients: List<DiaryRecipeIngredient>)

    @Query("DELETE FROM diary_recipe_ingredients WHERE diaryEntryId = :diaryEntryId")
    suspend fun deleteRecipeIngredients(diaryEntryId: String)

    /**
     * Upserts the entry and wholesale-replaces its per-day recipe ingredients (delete-then-insert),
     * so the entry's nutrition snapshot and the list it was derived from can never disagree.
     */
    @Transaction
    suspend fun upsertWithRecipeIngredients(entry: DiaryEntry, ingredients: List<DiaryRecipeIngredient>) {
        upsert(entry)
        deleteRecipeIngredients(entry.id)
        if (ingredients.isNotEmpty()) {
            insertRecipeIngredients(ingredients)
        }
    }
}
