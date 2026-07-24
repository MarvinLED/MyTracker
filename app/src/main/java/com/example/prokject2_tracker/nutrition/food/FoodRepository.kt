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

    fun observeAllBrands(): Flow<List<String>> = foodDao.observeAllBrands()

    suspend fun getById(id: String): FoodItem? = foodDao.getById(id)

    suspend fun create(
        name: String,
        brand: String?,
        baseUnit: BaseUnit,
        kcalPer100: Double,
        proteinPer100: Double,
        carbsPer100: Double,
        fatPer100: Double,
        saturatedFatPer100: Double,
        sugarPer100: Double,
        fiberPer100: Double,
        saltPer100: Double,
        servingName: String?,
        servingAmount: Double?,
    ): FoodItem {
        val now = Instant.now()
        val food = FoodItem(
            id = IdGenerator.newId(),
            name = name,
            brand = brand,
            baseUnit = baseUnit,
            kcalPer100 = kcalPer100,
            proteinPer100 = proteinPer100,
            carbsPer100 = carbsPer100,
            fatPer100 = fatPer100,
            saturatedFatPer100 = saturatedFatPer100,
            sugarPer100 = sugarPer100,
            fiberPer100 = fiberPer100,
            saltPer100 = saltPer100,
            servingName = servingName,
            servingAmount = servingAmount,
            createdAt = now,
            updatedAt = now,
        )
        foodDao.upsert(food)
        return food
    }

    suspend fun update(existing: FoodItem, updated: FoodItem) {
        foodDao.upsert(updated.copy(createdAt = existing.createdAt, updatedAt = Instant.now()))
    }

    suspend fun canDelete(foodId: String): Boolean = !foodDao.isUsedInAnyRecipe(foodId)

    suspend fun delete(food: FoodItem) {
        foodDao.delete(food)
    }
}
