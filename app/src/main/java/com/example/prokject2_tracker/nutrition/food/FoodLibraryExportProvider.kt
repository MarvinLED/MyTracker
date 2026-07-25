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
    val brand: String? = null,
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
    val fluidTypeId: String? = null,
    val fluidMlPer100: Double? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val tagIds: List<String> = emptyList(),
)

private fun FoodItem.toDto(tagIds: List<String>) = FoodItemDto(
    id = id,
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
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    tagIds = tagIds,
)

private fun FoodItemDto.toEntity() = FoodItem(
    id = id,
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
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)

/**
 * Imported after `"tags"` (see [importPriority]) since [FoodItemDto.tagIds] are foreign keys into
 * that data — and after `"fluidTypes"` (default priority 0) for the same reason, since
 * [FoodItemDto.fluidTypeId] points into the Getränkearten library.
 */
class FoodLibraryExportProvider @Inject constructor(
    private val foodDao: FoodDao,
    private val tagDao: TagDao,
) : LibraryExportProvider {
    override val key = "foods"
    override val importPriority = 5

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val dtos = foodDao.getAllOnce().map { food ->
            val tagIds = tagDao.getCrossRefsForFood(food.id).map { it.tagId }
            food.toDto(tagIds)
        }
        return json.encodeToJsonElement(dtos)
    }

    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<FoodItemDto>>(json)
        dtos.forEach { dto ->
            val existing = foodDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                foodDao.upsert(dto.toEntity())
                tagDao.replaceFoodTags(dto.id, dto.tagIds)
            }
        }
    }
}
