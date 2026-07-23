package com.example.prokject2_tracker.nutrition.recipe

import androidx.room.Embedded
import androidx.room.Relation
import com.example.prokject2_tracker.nutrition.food.FoodItem

data class RecipeIngredientWithFood(
    @Embedded val ingredient: RecipeIngredient,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: FoodItem,
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(entity = RecipeIngredient::class, parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<RecipeIngredientWithFood>,
)
