package com.example.mytracker.nutrition.recipe

import androidx.room.Embedded
import androidx.room.Relation
import com.example.mytracker.nutrition.FoodAmount
import com.example.mytracker.nutrition.food.FoodItem

data class RecipeIngredientWithFood(
    @Embedded val ingredient: RecipeIngredient,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: FoodItem,
)

fun List<RecipeIngredientWithFood>.foodAmounts(): List<FoodAmount> =
    map {
        FoodAmount(
            food = it.food,
            amountBaseUnits = it.ingredient.amountBaseUnits,
            unitName = it.ingredient.unitName,
            unitCount = it.ingredient.unitCount,
        )
    }

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(entity = RecipeIngredient::class, parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<RecipeIngredientWithFood>,
)
