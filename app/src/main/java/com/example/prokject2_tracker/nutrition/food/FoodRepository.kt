package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** A unit as edited in the UI, before it has an id — see [FoodRepository.setUnits]. */
data class FoodUnitDraft(val name: String, val amountBaseUnits: Double)

@Singleton
class FoodRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val foodUnitDao: FoodUnitDao,
) {
    fun observeAll(): Flow<List<FoodItem>> = foodDao.observeAll()

    fun observeUnits(foodId: String): Flow<List<FoodUnit>> = foodUnitDao.observeForFood(foodId)

    suspend fun getUnits(foodId: String): List<FoodUnit> = foodUnitDao.getForFood(foodId)

    /** Wholesale-replaces a food's units; blank names and non-positive amounts are dropped. */
    suspend fun setUnits(foodId: String, drafts: List<FoodUnitDraft>) {
        val units = drafts
            .filter { it.name.isNotBlank() && it.amountBaseUnits > 0.0 }
            .mapIndexed { index, draft ->
                FoodUnit(
                    id = IdGenerator.newId(),
                    foodItemId = foodId,
                    name = draft.name.trim(),
                    amountBaseUnits = draft.amountBaseUnits,
                    sortOrder = index,
                )
            }
        foodUnitDao.replaceForFood(foodId, units)
    }

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
        fluidTypeId: String?,
        fluidMlPer100: Double?,
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
            fluidTypeId = fluidTypeId,
            fluidMlPer100 = fluidMlPer100,
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
