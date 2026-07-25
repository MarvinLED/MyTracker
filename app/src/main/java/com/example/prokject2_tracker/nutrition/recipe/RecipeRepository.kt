package com.example.prokject2_tracker.nutrition.recipe

import com.example.prokject2_tracker.core.util.IdGenerator
import com.example.prokject2_tracker.fluid.FluidTypeDao
import com.example.prokject2_tracker.nutrition.NutritionTotals
import com.example.prokject2_tracker.nutrition.NutritionMath
import com.example.prokject2_tracker.nutrition.food.Tag
import com.example.prokject2_tracker.nutrition.food.TagRepository
import com.example.prokject2_tracker.nutrition.food.fluidMl
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * How much of one Getränkeart a whole recipe contains, summed over its drink-linked ingredients —
 * derived, never stored. [totalMl] is for the full recipe; divide by the servings for one portion.
 */
data class RecipeFluidAmount(val fluidTypeId: String, val name: String, val totalMl: Double)

data class RecipeWithNutrition(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredientWithFood>,
    val total: NutritionTotals,
    val perServing: NutritionTotals,
    /** Derived, not stored: the union of tags across this recipe's ingredient Lebensmittel. */
    val tags: List<Tag> = emptyList(),
    /** Derived, not stored: in Getränkearten-library order, so a recipe lists its fluids stably. */
    val fluids: List<RecipeFluidAmount> = emptyList(),
)

/** One ingredient row as edited in the UI, before ids are assigned. */
data class RecipeIngredientDraft(
    val foodId: String,
    val amountBaseUnits: Double,
)

@Singleton
class RecipeRepository @Inject constructor(
    private val recipeDao: RecipeDao,
    private val tagRepository: TagRepository,
    private val fluidTypeDao: FluidTypeDao,
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

    private suspend fun RecipeWithIngredients.toRecipeWithNutrition(): RecipeWithNutrition {
        val total = NutritionMath.total(ingredients.foodAmounts())
        return RecipeWithNutrition(
            recipe = recipe,
            ingredients = ingredients,
            total = total,
            perServing = NutritionMath.perServing(total, recipe.servings),
            tags = tagRepository.getTagsForFoodIds(ingredients.map { it.food.id }),
            fluids = fluidAmountsOf(ingredients),
        )
    }

    /**
     * The fluid each drink-linked ingredient brings along, summed per Getränkeart. Types that have
     * since been deleted from the library are dropped rather than shown nameless — the same rule the
     * diary's fluid mirroring applies.
     */
    private suspend fun fluidAmountsOf(ingredients: List<RecipeIngredientWithFood>): List<RecipeFluidAmount> {
        val mlByTypeId = ingredients
            .filter { it.food.fluidTypeId != null }
            .groupBy { it.food.fluidTypeId!! }
            .mapValues { (_, items) -> items.sumOf { it.food.fluidMl(it.ingredient.amountBaseUnits) } }
        if (mlByTypeId.isEmpty()) return emptyList()
        return fluidTypeDao.getAllOnce().mapNotNull { type ->
            mlByTypeId[type.id]
                ?.takeIf { it > 0.0 }
                ?.let { RecipeFluidAmount(fluidTypeId = type.id, name = type.name, totalMl = it) }
        }
    }
}
