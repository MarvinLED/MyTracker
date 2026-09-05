package com.example.mytracker.achievements

import com.example.mytracker.core.datastore.Nutrient
import com.example.mytracker.fitness.FitnessGoalMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What a reached goal is worth, and to which part of the figure it goes. The self-calibrating
 * weighting is the reason the score is still worth reading in month twelve, so it gets pinned down.
 */
class DayPointsTest {
    private fun goal(id: String, attribute: AvatarAttribute, isMet: Boolean) = ScoredGoal(id, attribute, isMet)

    @Test
    fun aGoalWithNoHistoryIsWorthThePlainBase() {
        // A brand-new goal has not yet earned the right to be called hard.
        assertEquals(1.0, difficultyFactor(null), 0.0001)
        assertEquals(1.0, difficultyFactor(GoalHistory(met = 0, judged = 0)), 0.0001)
    }

    @Test
    fun somethingManagedEveryDayIsWorthTheLeast() {
        assertEquals(1.0, difficultyFactor(GoalHistory(met = 56, judged = 56)), 0.0001)
    }

    @Test
    fun theRarerItIsManagedTheMoreItPays() {
        val often = difficultyFactor(GoalHistory(met = 40, judged = 56))
        val seldom = difficultyFactor(GoalHistory(met = 14, judged = 56))

        assertEquals(1.4, often, 0.0001)
        // Four times out of every fourteen days would be 4.0 uncapped — the ceiling holds it at 2.5.
        assertEquals(MAX_DIFFICULTY, seldom, 0.0001)
    }

    @Test
    fun aGoalNeverOnceReachedPaysTheMaximum() {
        // It is the one most worth breaking open, so it is the one worth the most.
        assertEquals(MAX_DIFFICULTY, difficultyFactor(GoalHistory(met = 0, judged = 30)), 0.0001)
    }

    @Test
    fun theCeilingHoldsHoweverRareTheGoalIs() {
        assertEquals(MAX_DIFFICULTY, difficultyFactor(GoalHistory(met = 1, judged = 100)), 0.0001)
    }

    @Test
    fun onlyMetGoalsPay() {
        val score = dayScore(
            epochDay = 20_000,
            goals = listOf(
                goal("nutrient-PROTEIN", AvatarAttribute.KRAFT, isMet = true),
                goal("nutrient-KCAL", AvatarAttribute.FORM, isMet = false),
            ),
            history = emptyMap(),
        )

        assertEquals(BASE_POINTS, score.points.getValue(AvatarAttribute.KRAFT), 0.0001)
        // A missed goal costs nothing: the day it was missed is already its own answer.
        assertNull(score.points[AvatarAttribute.FORM])
    }

    @Test
    fun goalsOfTheSameAttributeAddUp() {
        val score = dayScore(
            epochDay = 20_000,
            goals = listOf(
                goal("fluid-total", AvatarAttribute.VITALITAET, isMet = true),
                goal("task-a", AvatarAttribute.VITALITAET, isMet = true),
            ),
            history = emptyMap(),
        )

        assertEquals(2 * BASE_POINTS, score.points.getValue(AvatarAttribute.VITALITAET), 0.0001)
        assertEquals(2 * BASE_POINTS, score.total, 0.0001)
    }

    @Test
    fun theHardGoalOutweighsTheEasyOne() {
        val score = dayScore(
            epochDay = 20_000,
            goals = listOf(
                goal("nutrient-PROTEIN", AvatarAttribute.KRAFT, isMet = true),
                goal("fluid-total", AvatarAttribute.VITALITAET, isMet = true),
            ),
            history = mapOf(
                // Protein is managed one day in four; water practically always.
                "nutrient-PROTEIN" to GoalHistory(met = 14, judged = 56),
                "fluid-total" to GoalHistory(met = 56, judged = 56),
            ),
        )

        assertEquals(BASE_POINTS * MAX_DIFFICULTY, score.points.getValue(AvatarAttribute.KRAFT), 0.0001)
        assertEquals(BASE_POINTS, score.points.getValue(AvatarAttribute.VITALITAET), 0.0001)
    }

    @Test
    fun everyGoalRowFindsItsAttribute() {
        assertEquals(AvatarAttribute.KRAFT, attributeForGoalRow("nutrient-${Nutrient.PROTEIN.name}"))
        assertEquals(AvatarAttribute.FORM, attributeForGoalRow("nutrient-${Nutrient.KCAL.name}"))
        assertEquals(AvatarAttribute.FORM, attributeForGoalRow("nutrient-${Nutrient.SUGAR.name}"))
        assertEquals(AvatarAttribute.VITALITAET, attributeForGoalRow("fluid-total"))
        assertEquals(AvatarAttribute.VITALITAET, attributeForGoalRow("habit-abc-123"))
        assertEquals(AvatarAttribute.VITALITAET, attributeForGoalRow("task-abc-123"))
        assertEquals(AvatarAttribute.KLARHEIT, attributeForGoalRow("sleep-Schlafdauer"))
    }

    @Test
    fun anUnknownRowEarnsNothingRatherThanLandingSomewhereArbitrary() {
        assertNull(attributeForGoalRow("bloodpressure-morning"))
    }

    @Test
    fun cardioBuildsStaminaAndIronBuildsStrength() {
        assertEquals(AvatarAttribute.AUSDAUER, attributeForFitnessMetric(FitnessGoalMetric.CARDIO_SESSIONS))
        assertEquals(
            AvatarAttribute.AUSDAUER,
            attributeForFitnessMetric(FitnessGoalMetric.CARDIO_DURATION_MINUTES),
        )
        assertEquals(AvatarAttribute.KRAFT, attributeForFitnessMetric(FitnessGoalMetric.STRENGTH_SETS_TOTAL))
        assertEquals(
            AvatarAttribute.KRAFT,
            attributeForFitnessMetric(FitnessGoalMetric.STRENGTH_MAX_WEIGHT_INCREASE),
        )
    }
}
