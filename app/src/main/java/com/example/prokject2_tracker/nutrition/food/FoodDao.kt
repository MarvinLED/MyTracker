package com.example.prokject2_tracker.nutrition.food

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun search(query: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: String): FoodItem?

    @Query("SELECT * FROM food_items ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<FoodItem>

    @Upsert
    suspend fun upsert(food: FoodItem)

    @Delete
    suspend fun delete(food: FoodItem)

    @Query("SELECT EXISTS(SELECT 1 FROM recipe_ingredients WHERE foodId = :foodId)")
    suspend fun isUsedInAnyRecipe(foodId: String): Boolean
}
