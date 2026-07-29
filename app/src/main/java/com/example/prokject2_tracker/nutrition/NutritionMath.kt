package com.example.prokject2_tracker.nutrition

import com.example.prokject2_tracker.core.datastore.Nutrient
import com.example.prokject2_tracker.nutrition.food.FoodItem

/**
 * A food and how much of it, in the food's own base unit — what both a Rezept's ingredient list and
 * a Tagebuch entry's per-day copy of one reduce to before any nutrition or fluid is computed.
 *
 * [unitName]/[unitCount] only record how the amount was *entered* ("2 × Scheibe"); nothing computes
 * with them, so they carry along untouched.
 */
data class FoodAmount(
    val food: FoodItem,
    val amountBaseUnits: Double,
    val unitName: String? = null,
    val unitCount: Double? = null,
)

/**
 * Nutrition for some amount of food. Covers every [Nutrient] a goal can be set for, so a diary
 * entry's snapshot can answer "how much sugar today" without going back to the source food.
 */
data class NutritionTotals(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val saturatedFat: Double = 0.0,
    val sugar: Double = 0.0,
    val fiber: Double = 0.0,
    val salt: Double = 0.0,
) {
    operator fun plus(other: NutritionTotals) = NutritionTotals(
        kcal = kcal + other.kcal,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
        saturatedFat = saturatedFat + other.saturatedFat,
        sugar = sugar + other.sugar,
        fiber = fiber + other.fiber,
        salt = salt + other.salt,
    )

    operator fun times(factor: Double) = NutritionTotals(
        kcal = kcal * factor,
        protein = protein * factor,
        carbs = carbs * factor,
        fat = fat * factor,
        saturatedFat = saturatedFat * factor,
        sugar = sugar * factor,
        fiber = fiber * factor,
        salt = salt * factor,
    )

    /** Keyed by [Nutrient], for code that iterates goals rather than naming a nutrient directly. */
    fun byNutrient(): Map<Nutrient, Double> = mapOf(
        Nutrient.KCAL to kcal,
        Nutrient.PROTEIN to protein,
        Nutrient.CARBS to carbs,
        Nutrient.FAT to fat,
        Nutrient.SATURATED_FAT to saturatedFat,
        Nutrient.SUGAR to sugar,
        Nutrient.FIBER to fiber,
        Nutrient.SALT to salt,
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
            saturatedFat = food.saturatedFatPer100 * factor,
            sugar = food.sugarPer100 * factor,
            fiber = food.fiberPer100 * factor,
            salt = food.saltPer100 * factor,
        )
    }

    fun total(ingredients: List<FoodAmount>): NutritionTotals =
        ingredients.fold(NutritionTotals.ZERO) { acc, item ->
            acc + forFoodAmount(item.food, item.amountBaseUnits)
        }

    fun perServing(total: NutritionTotals, servings: Double): NutritionTotals =
        if (servings > 0.0) total * (1.0 / servings) else NutritionTotals.ZERO

    /**
     * Each macro's share of the energy it contributes, using the standard 4/4/9 kcal per gram —
     * *not* its share of the total grams, which would understate fat by more than half.
     * Empty when nothing has been logged yet.
     */
    fun macroEnergyShare(totals: NutritionTotals): Map<Nutrient, Double> {
        val energies = mapOf(
            Nutrient.PROTEIN to totals.protein * 4.0,
            Nutrient.CARBS to totals.carbs * 4.0,
            Nutrient.FAT to totals.fat * 9.0,
        )
        return if (energies.values.sum() <= 0.0) emptyMap() else energies
    }
}
