package com.example.prokject2_tracker.nutrition.food

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodUnitDao {
    @Query("SELECT * FROM food_units WHERE foodItemId = :foodItemId ORDER BY sortOrder")
    fun observeForFood(foodItemId: String): Flow<List<FoodUnit>>

    @Query("SELECT * FROM food_units WHERE foodItemId = :foodItemId ORDER BY sortOrder")
    suspend fun getForFood(foodItemId: String): List<FoodUnit>

    @Query("SELECT * FROM food_units ORDER BY foodItemId, sortOrder")
    suspend fun getAllOnce(): List<FoodUnit>

    @Query("DELETE FROM food_units WHERE foodItemId = :foodItemId")
    suspend fun deleteAllForFood(foodItemId: String)

    @Insert
    suspend fun insertAll(units: List<FoodUnit>)

    /** Wholesale-replaces a food's units (delete-then-insert), like `TagDao.replaceFoodTags`. */
    @Transaction
    suspend fun replaceForFood(foodItemId: String, units: List<FoodUnit>) {
        deleteAllForFood(foodItemId)
        if (units.isNotEmpty()) {
            insertAll(units)
        }
    }
}
