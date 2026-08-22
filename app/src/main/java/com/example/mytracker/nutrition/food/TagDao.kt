package com.example.mytracker.nutrition.food

import androidx.room.Dao
import androidx.room.Delete
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

    @Delete
    suspend fun delete(tag: Tag)

    /** How many Lebensmittel carry this tag — what the delete confirmation says out loud. */
    @Query("SELECT COUNT(*) FROM food_item_tags WHERE tagId = :tagId")
    suspend fun usageCount(tagId: String): Int

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

    @Query("SELECT * FROM tag_implications")
    fun observeAllImplications(): Flow<List<TagImplication>>

    @Query("SELECT * FROM tag_implications")
    suspend fun getAllImplicationsOnce(): List<TagImplication>

    @Upsert
    suspend fun upsertImplication(row: TagImplication)

    @Query("DELETE FROM tag_implications WHERE childTagId = :childTagId AND parentTagId = :parentTagId")
    suspend fun deleteImplication(childTagId: String, parentTagId: String)

    @Query("DELETE FROM tag_implications")
    suspend fun deleteAllImplications()

    /** Wipes the Tags for a replacing import; the `food_item_tags` rows cascade with them. */
    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
