package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class FoodItemDto(
    val id: String,
    val name: String,
    val baseUnit: BaseUnit,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val saturatedFatPer100: Double = 0.0,
    val sugarPer100: Double = 0.0,
    val fiberPer100: Double = 0.0,
    val saltPer100: Double = 0.0,
    val servingName: String? = null,
    val servingAmount: Double? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

private fun FoodItem.toDto() = FoodItemDto(
    id = id,
    name = name,
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
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

private fun FoodItemDto.toEntity() = FoodItem(
    id = id,
    name = name,
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
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

class FoodLibraryExportProvider @Inject constructor(
    private val foodDao: FoodDao,
) : LibraryExportProvider {
    override val key = "foods"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement =
        json.encodeToJsonElement(foodDao.getAllOnce().map { it.toDto() })

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<FoodItemDto>>(json)
        dtos.forEach { dto ->
            val existing = foodDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                foodDao.upsert(dto.toEntity())
            }
        }
    }
}
