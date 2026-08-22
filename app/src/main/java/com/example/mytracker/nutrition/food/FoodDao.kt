package com.example.mytracker.nutrition.food

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FoodItem>>

    /** Matches Name und Marke, damit "Alnatura" die Produkte der Marke findet. */
    @Query(
        "SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' " +
            "OR brand LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE",
    )
    fun search(query: String): Flow<List<FoodItem>>

    @Query("SELECT DISTINCT brand FROM food_items WHERE brand IS NOT NULL AND brand != '' ORDER BY brand COLLATE NOCASE")
    fun observeAllBrands(): Flow<List<String>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<FoodItem>

    @Query("SELECT * FROM food_items ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<FoodItem>

    @Upsert
    suspend fun upsert(food: FoodItem)

    @Delete
    suspend fun delete(food: FoodItem)

    /**
     * Both tables reference food_items with a NO_ACTION foreign key, so deleting a food either is
     * used in a Rezept or in a Tagebuch entry's own per-day copy of one would be rejected by SQLite.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM recipe_ingredients WHERE foodId = :foodId) " +
            "OR EXISTS(SELECT 1 FROM diary_recipe_ingredients WHERE foodId = :foodId)",
    )
    suspend fun isUsedInAnyRecipe(foodId: String): Boolean

    /** Wipes the Lebensmittel for a replacing import; `food_units` and `food_item_tags` cascade with them. */
    @Query("DELETE FROM food_items")
    suspend fun deleteAll()
}
