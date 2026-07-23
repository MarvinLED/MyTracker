package com.example.prokject2_tracker.nutrition

import com.example.prokject2_tracker.nutrition.food.FoodItem
import com.example.prokject2_tracker.nutrition.recipe.RecipeIngredientWithFood

data class NutritionTotals(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    operator fun plus(other: NutritionTotals) = NutritionTotals(
        kcal = kcal + other.kcal,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
    )

    operator fun times(factor: Double) = NutritionTotals(
        kcal = kcal * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor,
    )

    companion object {
        val ZERO = NutritionTotals(0.0, 0.0, 0.0, 0.0)
    }
}

object NutritionMath {
    fun forFoodAmount(food: FoodItem, amountBaseUnits: Double): NutritionTotals {
        val factor = amountBaseUnits / 100.0
        return NutritionTotals(
            kcal = food.kcalPer100 * factor,
            protein = food.proteinPer100 * factor,
            carbs = food.carbsPer100 * factor,
            fat = food.fatPer100 * factor,
        )
    }

    fun total(ingredients: List<RecipeIngredientWithFood>): NutritionTotals =
        ingredients.fold(NutritionTotals.ZERO) { acc, item ->
            acc + forFoodAmount(item.food, item.ingredient.amountBaseUnits)
        }

    fun perServing(total: NutritionTotals, servings: Double): NutritionTotals =
        if (servings > 0.0) total * (1.0 / servings) else NutritionTotals.ZERO
}
