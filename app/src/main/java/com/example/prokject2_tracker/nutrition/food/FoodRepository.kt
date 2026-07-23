package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FoodRepository @Inject constructor(
    private val foodDao: FoodDao,
) {
    fun observeAll(): Flow<List<FoodItem>> = foodDao.observeAll()

    fun search(query: String): Flow<List<FoodItem>> = foodDao.search(query)

    suspend fun getById(id: String): FoodItem? = foodDao.getById(id)

    suspend fun create(
        name: String,
        baseUnit: BaseUnit,
        kcalPer100: Double,
        proteinPer100: Double,
        carbsPer100: Double,
        fatPer100: Double,
        servingName: String?,
        servingAmount: Double?,
    ) {
        val now = Instant.now()
        foodDao.upsert(
            FoodItem(
                id = IdGenerator.newId(),
                name = name,
                baseUnit = baseUnit,
                kcalPer100 = kcalPer100,
                proteinPer100 = proteinPer100,
                carbsPer100 = carbsPer100,
                fatPer100 = fatPer100,
                servingName = servingName,
                servingAmount = servingAmount,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(existing: FoodItem, updated: FoodItem) {
        foodDao.upsert(updated.copy(createdAt = existing.createdAt, updatedAt = Instant.now()))
    }

    suspend fun canDelete(foodId: String): Boolean = !foodDao.isUsedInAnyRecipe(foodId)

    suspend fun delete(food: FoodItem) {
        foodDao.delete(food)
    }
}
