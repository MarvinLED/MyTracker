package com.example.mytracker.nutrition.food

/**
 * How much of a food amount is fluid, in ml. The single place this conversion lives: the diary
 * mirrors it into the Flüssigkeiten log, and Rezepte show it per ingredient and as a total.
 *
 * A food that isn't linked to a Getränkeart contributes nothing; a linked one without an explicit
 * [FoodItem.fluidMlPer100] counts as "consists entirely of that fluid" (100 ml per 100 g/ml).
 *
 * Takes the two link fields loose rather than a whole [FoodItem] so UI rows that only carry the
 * link (and not the food) can use it too.
 */
fun fluidMlOf(fluidTypeId: String?, fluidMlPer100: Double?, amountBaseUnits: Double): Double =
    if (fluidTypeId == null) 0.0 else amountBaseUnits * (fluidMlPer100 ?: 100.0) / 100.0

fun FoodItem.fluidMl(amountBaseUnits: Double): Double = fluidMlOf(fluidTypeId, fluidMlPer100, amountBaseUnits)
