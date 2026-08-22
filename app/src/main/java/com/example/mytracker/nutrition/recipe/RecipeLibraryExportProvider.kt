package com.example.mytracker.nutrition.recipe

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.backup.BackupScope
import com.example.mytracker.core.util.IdGenerator
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
data class RecipeIngredientDto(
    val foodId: String,
    val amountBaseUnits: Double,
    val sortOrder: Int,
    /** How the amount was entered ("2 × Scheibe"); absent in backups written before units existed. */
    val unitName: String? = null,
    val unitCount: Double? = null,
)

@Serializable
data class RecipeExportDto(
    val id: String,
    val name: String,
    val servings: Double,
    val instructions: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val ingredients: List<RecipeIngredientDto>,
)

/**
 * Key `"recipes"`, imported after `"foods"` (see [importPriority]) since recipe ingredients
 * reference food ids by foreign key.
 */
class RecipeLibraryExportProvider @Inject constructor(
    private val recipeDao: RecipeDao,
) : BackupExportProvider {
    override val key = "recipes"
    override val scope = BackupScope.LIBRARY
    override val importPriority = 10

    private val jsonCodec = Json { ignoreUnknownKeys = true }

    override suspend fun export(): JsonElement {
        val dtos = recipeDao.getAllOnce().map { recipe ->
            val ingredients = recipeDao.getIngredientsOnce(recipe.id).map {
                RecipeIngredientDto(
                    foodId = it.foodId,
                    amountBaseUnits = it.amountBaseUnits,
                    sortOrder = it.sortOrder,
                    unitName = it.unitName,
                    unitCount = it.unitCount,
                )
            }
            RecipeExportDto(
                id = recipe.id,
                name = recipe.name,
                servings = recipe.servings,
                instructions = recipe.instructions,
                createdAtEpochMillis = recipe.createdAt.toEpochMilli(),
                updatedAtEpochMillis = recipe.updatedAt.toEpochMilli(),
                ingredients = ingredients,
            )
        }
        return jsonCodec.encodeToJsonElement(dtos)
    }

    override suspend fun import(json: JsonElement) {
        val dtos = jsonCodec.decodeFromJsonElement<List<RecipeExportDto>>(json)
        dtos.forEach { dto ->
            val existing = recipeDao.getById(dto.id)
            if (existing == null || dto.updatedAtEpochMillis > existing.updatedAt.toEpochMilli()) {
                val recipe = Recipe(
                    id = dto.id,
                    name = dto.name,
                    servings = dto.servings,
                    instructions = dto.instructions,
                    createdAt = Instant.ofEpochMilli(dto.createdAtEpochMillis),
                    updatedAt = Instant.ofEpochMilli(dto.updatedAtEpochMillis),
                )
                val ingredients = dto.ingredients.map {
                    RecipeIngredient(
                        id = IdGenerator.newId(),
                        recipeId = dto.id,
                        foodId = it.foodId,
                        amountBaseUnits = it.amountBaseUnits,
                        unitName = it.unitName,
                        unitCount = it.unitCount,
                        sortOrder = it.sortOrder,
                    )
                }
                recipeDao.replaceRecipeWithIngredients(recipe, ingredients)
            }
        }
    }

    /** Ingredients included. */
    override suspend fun clear() {
        recipeDao.deleteAll()
    }
}
