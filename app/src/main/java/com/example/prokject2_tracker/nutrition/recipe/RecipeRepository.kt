package com.example.prokject2_tracker.nutrition.recipe

import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.nutrition.NutritionTotals
import com.example.prokject2_tracker.nutrition.NutritionMath
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RecipeWithNutrition(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredientWithFood>,
    val total: NutritionTotals,
    val perServing: NutritionTotals,
)

/** One ingredient row as edited in the UI, before ids are assigned. */
data class RecipeIngredientDraft(
    val foodId: String,
    val amountBaseUnits: Double,
)

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao,
) {
    fun observeAllWithNutrition(): Flow<List<RecipeWithNutrition>> =
        recipeDao.observeAllWithIngredients().map { list -> list.map { it.toRecipeWithNutrition() } }

    fun observeByIdWithNutrition(id: String): Flow<RecipeWithNutrition?> =
        recipeDao.observeWithIngredients(id).map { it?.toRecipeWithNutrition() }

    suspend fun getRecipeOnly(id: String): Recipe? = recipeDao.getById(id)

    suspend fun getWithNutrition(id: String): RecipeWithNutrition? =
        recipeDao.getWithIngredients(id)?.toRecipeWithNutrition()

    suspend fun saveRecipe(existing: Recipe?, name: String, servings: Double, instructions: String?, ingredientDrafts: List<RecipeIngredientDraft>) {
        val now = Instant.now()
        val recipe = Recipe(
            id = existing?.id ?: IdGenerator.newId(),
            name = name,
            servings = servings,
            instructions = instructions,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val ingredients = ingredientDrafts.mapIndexed { index, draft ->
            RecipeIngredient(
                id = IdGenerator.newId(),
                recipeId = recipe.id,
                foodId = draft.foodId,
                amountBaseUnits = draft.amountBaseUnits,
                sortOrder = index,
            )
        }
        recipeDao.replaceRecipeWithIngredients(recipe, ingredients)
    }

    suspend fun delete(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe)
    }

    private fun RecipeWithIngredients.toRecipeWithNutrition(): RecipeWithNutrition {
        val total = NutritionMath.total(ingredients)
        return RecipeWithNutrition(
            recipe = recipe,
            ingredients = ingredients,
            total = total,
            perServing = NutritionMath.perServing(total, recipe.servings),
        )
    }
}
