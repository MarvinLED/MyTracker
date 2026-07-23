package com.example.prokject2_tracker.nutrition.recipe

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    fun observeAllWithIngredients(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeWithIngredients(id: String): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getWithIngredients(id: String): RecipeWithIngredients?

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: String): Recipe?

    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    suspend fun getAllOnce(): List<Recipe>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    suspend fun getIngredientsOnce(recipeId: String): List<RecipeIngredient>

    @Upsert
    suspend fun upsertRecipe(recipe: Recipe)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)

    /** Upserts the recipe and wholesale-replaces its ingredient rows (delete-then-insert). */
    @Transaction
    suspend fun replaceRecipeWithIngredients(recipe: Recipe, ingredients: List<RecipeIngredient>) {
        upsertRecipe(recipe)
        deleteIngredientsForRecipe(recipe.id)
        if (ingredients.isNotEmpty()) {
            insertIngredients(ingredients)
        }
    }

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)
}
