package com.example.prokject2_tracker.nutrition.food

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: String): Tag?

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): Tag?

    @Upsert
    suspend fun upsert(tag: Tag)

    @Query("SELECT * FROM food_item_tags")
    fun observeAllFoodItemTags(): Flow<List<FoodItemTag>>

    @Query("SELECT * FROM food_item_tags WHERE foodItemId = :foodItemId")
    suspend fun getCrossRefsForFood(foodItemId: String): List<FoodItemTag>

    @Query("SELECT * FROM food_item_tags WHERE foodItemId IN (:foodItemIds)")
    suspend fun getCrossRefsForFoods(foodItemIds: List<String>): List<FoodItemTag>

    @Query("DELETE FROM food_item_tags WHERE foodItemId = :foodItemId")
    suspend fun deleteAllForFood(foodItemId: String)

    @Insert
    suspend fun insertFoodItemTags(rows: List<FoodItemTag>)

    /** Wholesale-replaces a food's tag associations (delete-then-insert). */
    @Transaction
    suspend fun replaceFoodTags(foodItemId: String, tagIds: List<String>) {
        deleteAllForFood(foodItemId)
        if (tagIds.isNotEmpty()) {
            insertFoodItemTags(tagIds.map { FoodItemTag(foodItemId = foodItemId, tagId = it) })
        }
    }
}
