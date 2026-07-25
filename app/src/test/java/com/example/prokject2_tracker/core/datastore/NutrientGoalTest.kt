package com.example.prokject2_tracker.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The goal type decides what "met" means, and getting it backwards would tell the user they're on
 * track while they're over their sugar limit — so each type is pinned separately.
 */
class NutrientGoalTest {
    @Test
    fun minGoal_isMetOnceTheValueIsReachedAndStaysMetAbove() {
        val goal = NutrientGoal(100.0, NutrientGoalType.MIN)
        assertFalse(goal.isMetBy(99.9))
        assertTrue(goal.isMetBy(100.0))
        assertTrue(goal.isMetBy(250.0))
        assertFalse(goal.isExceededBy(250.0))
    }

    @Test
    fun maxGoal_isMetWhileUnderAndExceededOnlyAbove() {
        val goal = NutrientGoal(50.0, NutrientGoalType.MAX)
        assertTrue(goal.isMetBy(0.0))
        assertTrue(goal.isMetBy(50.0))
        assertFalse(goal.isMetBy(50.1))
        assertFalse(goal.isExceededBy(50.0))
        assertTrue(goal.isExceededBy(50.1))
    }

    @Test
    fun exactGoal_allowsFivePercentEitherSide() {
        val goal = NutrientGoal(100.0, NutrientGoalType.EXACT)
        assertFalse(goal.isMetBy(94.9))
        assertTrue(goal.isMetBy(95.0))
        assertTrue(goal.isMetBy(100.0))
        assertTrue(goal.isMetBy(105.0))
        assertFalse(goal.isMetBy(105.1))
        // Only a MAX goal is ever "exceeded" — overshooting an exact goal isn't a failure state.
        assertFalse(goal.isExceededBy(200.0))
    }

    @Test
    fun exactGoal_ofZeroIsNeverMet() {
        assertFalse(NutrientGoal(0.0, NutrientGoalType.EXACT).isMetBy(0.0))
    }

    @Test
    fun fraction_isClampedToABarsRange() {
        val goal = NutrientGoal(100.0, NutrientGoalType.MIN)
        assertEquals(0f, goal.fractionOf(0.0), 0.0001f)
        assertEquals(0.5f, goal.fractionOf(50.0), 0.0001f)
        assertEquals(1f, goal.fractionOf(100.0), 0.0001f)
        assertEquals(1f, goal.fractionOf(400.0), 0.0001f)
        assertEquals(0f, NutrientGoal(0.0).fractionOf(10.0), 0.0001f)
    }

    @Test
    fun unmetGoals_listsOnlyWhatIsStillOpenInNutrientOrder() {
        val prefs = UserPreferences(
            dailyWaterGoalMl = 2000.0,
            weightUnit = WeightUnit.KG,
            nutrientGoals = mapOf(
                Nutrient.PROTEIN to NutrientGoal(100.0, NutrientGoalType.MIN),
                Nutrient.SUGAR to NutrientGoal(50.0, NutrientGoalType.MAX),
                Nutrient.FIBER to NutrientGoal(30.0, NutrientGoalType.MIN),
            ),
        )

        val open = prefs.unmetGoals(
            mapOf(
                Nutrient.PROTEIN to 120.0, // met
                Nutrient.SUGAR to 70.0, // over the limit
                Nutrient.FIBER to 10.0, // still short
            ),
        )

        assertEquals(listOf(Nutrient.SUGAR, Nutrient.FIBER), open.map { it.first })
    }

    @Test
    fun calorieGoal_fallsBackWhenTheUserNeverSetOne() {
        val prefs = UserPreferences(dailyWaterGoalMl = 2000.0, weightUnit = WeightUnit.KG)
        assertEquals(2000.0, prefs.dailyCalorieGoalKcal, 0.0001)

        val withGoal = prefs.copy(nutrientGoals = mapOf(Nutrient.KCAL to NutrientGoal(1800.0)))
        assertEquals(1800.0, withGoal.dailyCalorieGoalKcal, 0.0001)
    }
}
