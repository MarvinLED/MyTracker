package com.example.prokject2_tracker.nutrition.diary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Projection matching [com.example.prokject2_tracker.core.metrics.MetricPoint]'s field names. */
data class DailyKcalTotal(val epochDay: Long, val value: Double)
data class DailyProteinTotal(val epochDay: Long, val value: Double)
data class DailyCarbsTotal(val epochDay: Long, val value: Double)
data class DailyFatTotal(val epochDay: Long, val value: Double)

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries WHERE epochDay = :epochDay ORDER BY mealType, createdAt")
    fun observeForDay(epochDay: Long): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: String): DiaryEntry?

    @Query("SELECT COALESCE(SUM(kcal), 0) FROM diary_entries WHERE epochDay = :epochDay")
    fun observeDayTotalKcal(epochDay: Long): Flow<Double>

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
