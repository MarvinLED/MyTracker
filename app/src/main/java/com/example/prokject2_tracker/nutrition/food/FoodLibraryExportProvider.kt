package com.example.prokject2_tracker.nutrition.food

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import com.example.prokject2_tracker.core.backup.BackupScope
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class FoodUnitDto(
    val id: String,
    val name: String,
    val amountBaseUnits: Double,
    val sortOrder: Int = 0,
)

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
    /**
     * Legacy: the single named serving foods had before [units] existed. Only ever read, never
     * written — [toEntity]'s caller folds it into a unit so older backups still import.
     */
    val servingName: String? = null,
    val servingAmount: Double? = null,
    val fluidTypeId: String? = null,
    val fluidMlPer100: Double? = null,
    val price: Double? = null,
    val priceUnitName: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val tagIds: List<String> = emptyList(),
    val units: List<FoodUnitDto> = emptyList(),
)

private fun FoodUnit.toDto() = FoodUnitDto(
    id = id,
    name = name,
    amountBaseUnits = amountBaseUnits,
    sortOrder = sortOrder,
)

private fun FoodUnitDto.toEntity(foodItemId: String) = FoodUnit(
    id = id,
    foodItemId = foodItemId,
    name = name,
    amountBaseUnits = amountBaseUnits,
    sortOrder = sortOrder,
)

/**
 * The units to import for [dto]: its own list, or — for a backup written before units existed — the
 * single legacy serving turned into one.
 */
private fun FoodItemDto.unitEntities(): List<FoodUnit> = when {
    units.isNotEmpty() -> units.map { it.toEntity(id) }
    servingName != null && (servingAmount ?: 0.0) > 0.0 -> listOf(
        FoodUnit(
            id = "$id-serving",
            foodItemId = id,
            name = servingName,
            amountBaseUnits = servingAmount!!,
        ),
    )
    else -> emptyList()
}

private fun FoodItem.toDto(tagIds: List<String>, units: List<FoodUnit>) = FoodItemDto(
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
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
    price = price,
    priceUnitName = priceUnitName,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    tagIds = tagIds,
    units = units.map { it.toDto() },
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
    fluidTypeId = fluidTypeId,
    fluidMlPer100 = fluidMlPer100,
    price = price,
    priceUnitName = priceUnitName,
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
    private val foodUnitDao: FoodUnitDao,
) : BackupExportProvider {
    override val key = "foods"
    override val scope = BackupScope.LIBRARY
    override val importPriority = 5

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val dtos = foodDao.getAllOnce().map { food ->
            val tagIds = tagDao.getCrossRefsForFood(food.id).map { it.tagId }
            food.toDto(tagIds, foodUnitDao.getForFood(food.id))
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
                foodUnitDao.replaceForFood(dto.id, dto.unitEntities())
            }
        }
    }

    /** The units and tag links cascade with the Lebensmittel. */
    override suspend fun clear() {
        foodDao.deleteAll()
    }
}
