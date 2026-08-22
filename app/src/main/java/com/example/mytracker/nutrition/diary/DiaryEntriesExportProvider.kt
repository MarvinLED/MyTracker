package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupScope
import com.example.mytracker.nutrition.food.FoodDao
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class DiaryRecipeIngredientDto(
    val id: String,
    val foodId: String,
    val amountBaseUnits: Double,
    val unitName: String? = null,
    val unitCount: Double? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class DiaryEntryDto(
    val id: String,
    val epochDay: Long,
    val createdAtEpochMillis: Long,
    val mealType: MealType,
    val sourceType: DiarySourceType,
    val sourceId: String,
    val sourceName: String,
    val quantity: Double,
    val quantityUnit: String,
    val unitName: String? = null,
    val unitCount: Double? = null,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val saturatedFat: Double = 0.0,
    val sugar: Double = 0.0,
    val fiber: Double = 0.0,
    val salt: Double = 0.0,
    val recipeServings: Double? = null,
    /** The recipe's ingredients as they were on that day; empty for anything but a recipe entry. */
    val recipeIngredients: List<DiaryRecipeIngredientDto> = emptyList(),
)

private fun DiaryRecipeIngredient.toDto() = DiaryRecipeIngredientDto(
    id = id,
    foodId = foodId,
    amountBaseUnits = amountBaseUnits,
    unitName = unitName,
    unitCount = unitCount,
    sortOrder = sortOrder,
)

private fun DiaryRecipeIngredientDto.toEntity(diaryEntryId: String) = DiaryRecipeIngredient(
    id = id,
    diaryEntryId = diaryEntryId,
    foodId = foodId,
    amountBaseUnits = amountBaseUnits,
    unitName = unitName,
    unitCount = unitCount,
    sortOrder = sortOrder,
)

private fun DiaryEntry.toDto(ingredients: List<DiaryRecipeIngredient>) = DiaryEntryDto(
    id = id,
    epochDay = epochDay,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    mealType = mealType,
    sourceType = sourceType,
    sourceId = sourceId,
    sourceName = sourceName,
    quantity = quantity,
    quantityUnit = quantityUnit,
    unitName = unitName,
    unitCount = unitCount,
    kcal = kcal,
    protein = protein,
    carbs = carbs,
    fat = fat,
    saturatedFat = saturatedFat,
    sugar = sugar,
    fiber = fiber,
    salt = salt,
    recipeServings = recipeServings,
    recipeIngredients = ingredients.map { it.toDto() },
)

private fun DiaryEntryDto.toEntity() = DiaryEntry(
    id = id,
    epochDay = epochDay,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    mealType = mealType,
    sourceType = sourceType,
    sourceId = sourceId,
    sourceName = sourceName,
    quantity = quantity,
    quantityUnit = quantityUnit,
    unitName = unitName,
    unitCount = unitCount,
    kcal = kcal,
    protein = protein,
    carbs = carbs,
    fat = fat,
    saturatedFat = saturatedFat,
    sugar = sugar,
    fiber = fiber,
    salt = salt,
    recipeServings = recipeServings,
)

/**
 * Every logged Tagebuch entry, with its per-day recipe breakdown nested inside the entry that owns
 * it rather than as a second top-level list — the two can then never arrive apart.
 *
 * The entries carry their own nutrition figures and their `sourceName`, so a backup restores a
 * readable diary even onto a device whose Bibliothek came out of a different file. Only the recipe
 * breakdown really needs its foods present, and it is dropped rather than refused when they aren't:
 * losing the ingredient list of one meal beats losing the meal.
 */
class DiaryEntriesExportProvider @Inject constructor(
    private val diaryDao: DiaryDao,
    private val foodDao: FoodDao,
) : BackupExportProvider {
    override val key = "diaryEntries"
    override val scope = BackupScope.DAILY_ENTRIES
    override val importPriority = BackupExportProvider.DAILY_ENTRIES_PRIORITY

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val ingredientsByEntry = diaryDao.getAllRecipeIngredientsOnce().groupBy { it.diaryEntryId }
        val dtos = diaryDao.getAllOnce().map { entry ->
            entry.toDto(ingredientsByEntry[entry.id].orEmpty())
        }
        return json.encodeToJsonElement(dtos)
    }

    /**
     * Kept if already there: a logged meal has no `updatedAt` to compare, and a correction made
     * after the backup was written is the more recent truth.
     */
    override suspend fun import(json: JsonElement) {
        val dtos = this.json.decodeFromJsonElement<List<DiaryEntryDto>>(json)
        dtos.forEach { dto ->
            if (diaryDao.getById(dto.id) != null) return@forEach
            // `foodId` is a real foreign key: an ingredient naming a food this device doesn't have
            // would fail the whole insert, so it is left behind and the meal itself still lands.
            val ingredients = dto.recipeIngredients
                .filter { foodDao.getById(it.foodId) != null }
                .map { it.toEntity(dto.id) }
            diaryDao.upsertWithRecipeIngredients(dto.toEntity(), ingredients)
        }
    }

    override suspend fun clear() {
        diaryDao.deleteAll()
    }
}
