package com.example.mytracker.nutrition

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.nutrition.food.BaseUnit
import com.example.mytracker.nutrition.food.FoodItem
import com.example.mytracker.nutrition.food.fluidMl
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionMathTest {
    private fun food(
        kcal: Double = 100.0,
        protein: Double = 10.0,
        carbs: Double = 20.0,
        fat: Double = 5.0,
        sugar: Double = 8.0,
        salt: Double = 1.0,
        fluidTypeId: String? = null,
        fluidMlPer100: Double? = null,
    ) = FoodItem(
        id = "f", name = "f", baseUnit = BaseUnit.G,
        kcalPer100 = kcal, proteinPer100 = protein, carbsPer100 = carbs, fatPer100 = fat,
        sugarPer100 = sugar, saltPer100 = salt,
        fluidTypeId = fluidTypeId, fluidMlPer100 = fluidMlPer100,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
    )

    @Test
    fun forFoodAmount_scalesFromPer100Values() {
        val totals = NutritionMath.forFoodAmount(food(), amountBaseUnits = 250.0)
        assertEquals(250.0, totals.kcal, 0.0001)
        assertEquals(25.0, totals.protein, 0.0001)
        assertEquals(50.0, totals.carbs, 0.0001)
        assertEquals(12.5, totals.fat, 0.0001)
        // The nutrients beyond the macros scale too — the diary snapshots these now.
        assertEquals(20.0, totals.sugar, 0.0001)
        assertEquals(2.5, totals.salt, 0.0001)
    }

    @Test
    fun forFoodAmount_ofNothingIsNothing() {
        val totals = NutritionMath.forFoodAmount(food(), amountBaseUnits = 0.0)
        assertEquals(0.0, totals.kcal, 0.0001)
        assertEquals(0.0, totals.sugar, 0.0001)
    }

    @Test
    fun total_sumsEveryNutrientAcrossIngredients() {
        val totals = NutritionMath.total(
            listOf(
                FoodAmount(food(kcal = 100.0), 100.0),
                FoodAmount(food(kcal = 300.0), 200.0),
            ),
        )
        assertEquals(100.0 + 600.0, totals.kcal, 0.0001)
        assertEquals(10.0 + 20.0, totals.protein, 0.0001)
    }

    @Test
    fun perServing_divides_andSurvivesAZeroServingCount() {
        val total = NutritionTotals(kcal = 900.0, protein = 30.0, carbs = 60.0, fat = 15.0)
        assertEquals(300.0, NutritionMath.perServing(total, 3.0).kcal, 0.0001)
        // Not a crash and not infinity: a recipe with no servings has no per-serving value.
        assertEquals(NutritionTotals.ZERO, NutritionMath.perServing(total, 0.0))
    }

    @Test
    fun macroEnergyShare_weightsFatAtNineKcalPerGram() {
        // 60 g carbs + 30 g protein + 20 g fat -> 240 + 120 + 180 kcal
        val shares = NutritionMath.macroEnergyShare(
            NutritionTotals(kcal = 540.0, protein = 30.0, carbs = 60.0, fat = 20.0),
        )
        assertEquals(120.0, shares.getValue(Nutrient.PROTEIN), 0.0001)
        assertEquals(240.0, shares.getValue(Nutrient.CARBS), 0.0001)
        assertEquals(180.0, shares.getValue(Nutrient.FAT), 0.0001)

        // Fat is 20 of 110 grams (18 %) but 180 of 540 kcal (33 %) — the whole point of the ring
        // being energy-based rather than gram-based.
        assertTrue(shares.getValue(Nutrient.FAT) / shares.values.sum() > 0.3)
    }

    @Test
    fun macroEnergyShare_isEmptyBeforeAnythingIsLogged() {
        assertTrue(NutritionMath.macroEnergyShare(NutritionTotals.ZERO).isEmpty())
    }

    @Test
    fun byNutrient_exposesEveryGoalableNutrient() {
        val map = NutritionTotals(kcal = 1.0, protein = 2.0, carbs = 3.0, fat = 4.0).byNutrient()
        assertEquals(Nutrient.entries.toSet(), map.keys)
    }

    @Test
    fun fluidMl_countsOnlyDrinkLinkedFoods() {
        assertEquals(0.0, food().fluidMl(500.0), 0.0001)
        // A linked food without an explicit ml-per-100 consists entirely of that fluid.
        assertEquals(500.0, food(fluidTypeId = "t").fluidMl(500.0), 0.0001)
        // A soup: 40 ml of fluid per 100 g.
        assertEquals(200.0, food(fluidTypeId = "t", fluidMlPer100 = 40.0).fluidMl(500.0), 0.0001)
    }
}
