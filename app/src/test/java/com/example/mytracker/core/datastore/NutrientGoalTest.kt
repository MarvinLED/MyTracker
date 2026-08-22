package com.example.mytracker.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which bounds are set decides what "met" means, and getting it backwards would tell the user
 * they're on track while they're over their sugar limit — so each combination is pinned separately.
 */
class NutrientGoalTest {
    @Test
    fun lowerBoundOnly_isMetOnceReachedAndStaysMetAbove() {
        val goal = NutrientGoal(min = 100.0)
        assertFalse(goal.isMetBy(99.9))
        assertTrue(goal.isMetBy(100.0))
        assertTrue(goal.isMetBy(250.0))
        // Nothing to exceed without an upper bound.
        assertFalse(goal.isExceededBy(250.0))
    }

    @Test
    fun upperBoundOnly_isMetWhileUnderAndExceededOnlyAbove() {
        val goal = NutrientGoal(max = 50.0)
        assertTrue(goal.isMetBy(0.0))
        assertTrue(goal.isMetBy(50.0))
        assertFalse(goal.isMetBy(50.1))
        assertFalse(goal.isExceededBy(50.0))
        assertTrue(goal.isExceededBy(50.1))
    }

    @Test
    fun bothBounds_areMetOnlyInsideTheRange() {
        val goal = NutrientGoal(min = 100.0, max = 150.0)
        assertFalse(goal.isMetBy(99.9))
        assertTrue(goal.isMetBy(100.0))
        assertTrue(goal.isMetBy(150.0))
        assertFalse(goal.isMetBy(150.1))
        assertTrue(goal.isExceededBy(150.1))
        // Under the range is unmet, but it is not the "over the limit" state.
        assertFalse(goal.isExceededBy(20.0))
    }

    @Test
    fun aGoalWithNeitherBound_isNeverMet() {
        val goal = NutrientGoal()
        assertTrue(goal.isEmpty)
        assertFalse(goal.isMetBy(0.0))
        assertFalse(goal.isMetBy(100.0))
    }

    @Test
    fun barTarget_isTheUpperBoundWhenThereIsOne() {
        assertEquals(150.0, NutrientGoal(min = 100.0, max = 150.0).barTarget)
        assertEquals(100.0, NutrientGoal(min = 100.0).barTarget)
        assertEquals(50.0, NutrientGoal(max = 50.0).barTarget)
        assertNull(NutrientGoal().barTarget)
    }

    @Test
    fun lineTarget_isTheLowerBoundWhenThereIsOne() {
        // The deliberate mirror image of barTarget: a bar ends at the upper bound, the Verlauf's
        // Soll line follows what is being worked towards.
        assertEquals(100.0, NutrientGoal(min = 100.0, max = 150.0).lineTarget)
        assertEquals(100.0, NutrientGoal(min = 100.0).lineTarget)
        assertEquals(50.0, NutrientGoal(max = 50.0).lineTarget)
        assertNull(NutrientGoal().lineTarget)
    }

    @Test
    fun minMarker_sitsWhereTheLowerBoundFallsInsideTheUpperOne() {
        assertEquals(2f / 3f, NutrientGoal(min = 100.0, max = 150.0).minMarkerFraction!!, 0.0001f)
        // Nothing to mark: the bar already ends at the only bound there is.
        assertNull(NutrientGoal(min = 100.0).minMarkerFraction)
        assertNull(NutrientGoal(max = 150.0).minMarkerFraction)
        // An unreachable goal still has to produce a drawable fraction rather than overflow the bar.
        assertEquals(1f, NutrientGoal(min = 200.0, max = 150.0).minMarkerFraction!!, 0.0001f)
    }

    @Test
    fun fraction_isClampedToABarsRange() {
        val goal = NutrientGoal(min = 100.0)
        assertEquals(0f, goal.fractionOf(0.0), 0.0001f)
        assertEquals(0.5f, goal.fractionOf(50.0), 0.0001f)
        assertEquals(1f, goal.fractionOf(100.0), 0.0001f)
        assertEquals(1f, goal.fractionOf(400.0), 0.0001f)
        assertEquals(0f, NutrientGoal().fractionOf(10.0), 0.0001f)
    }

    @Test
    fun fraction_measuresAgainstTheUpperBoundWhenBothAreSet() {
        val goal = NutrientGoal(min = 100.0, max = 200.0)
        assertEquals(0.5f, goal.fractionOf(100.0), 0.0001f)
        assertEquals(1f, goal.fractionOf(200.0), 0.0001f)
    }

    @Test
    fun unmetGoals_listsOnlyWhatIsStillOpenInNutrientOrder() {
        val prefs = UserPreferences(
            dailyWaterGoalMl = 2000.0,
            weightUnit = WeightUnit.KG,
            nutrientGoals = mapOf(
                Nutrient.PROTEIN to NutrientGoal(min = 100.0),
                Nutrient.SUGAR to NutrientGoal(max = 50.0),
                Nutrient.FIBER to NutrientGoal(min = 30.0),
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

        val withGoal = prefs.copy(nutrientGoals = mapOf(Nutrient.KCAL to NutrientGoal(max = 1800.0)))
        assertEquals(1800.0, withGoal.dailyCalorieGoalKcal, 0.0001)
    }
}
