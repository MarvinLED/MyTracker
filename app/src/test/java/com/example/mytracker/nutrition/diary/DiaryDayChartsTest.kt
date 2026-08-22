package com.example.prokject2_tracker.nutrition.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiaryDayChartsTest {

    @Test
    fun macroBarFraction_withGoal_isTheShareOfThatGoal() {
        assertEquals(0.5f, macroBarFraction(consumed = 50.0, goal = 100.0, peerMax = 400.0), 0.0001f)
    }

    @Test
    fun macroBarFraction_overTheGoal_stopsAtFull() {
        assertEquals(1f, macroBarFraction(consumed = 150.0, goal = 100.0, peerMax = 150.0), 0.0001f)
    }

    @Test
    fun macroBarFraction_withoutGoal_comparesAgainstTheBiggestMacro() {
        // 60 g of fat next to 240 g of carbs: a quarter as high, with no goal involved at all.
        assertEquals(0.25f, macroBarFraction(consumed = 60.0, goal = null, peerMax = 240.0), 0.0001f)
    }

    @Test
    fun macroBarFraction_withNothingLogged_isEmpty() {
        assertEquals(0f, macroBarFraction(consumed = 0.0, goal = null, peerMax = 0.0), 0.0001f)
    }

    @Test
    fun macroBarFraction_withAZeroGoal_fallsBackToThePeerComparison() {
        // A goal of 0 is no target to measure against, so it must not divide by zero either.
        assertEquals(0.5f, macroBarFraction(consumed = 50.0, goal = 0.0, peerMax = 100.0), 0.0001f)
    }

    @Test
    fun goalTargetLabel_saysWhichKindOfBoundItIs() {
        // A range and a ceiling must not read the same — "100–150" vs a bare "150".
        assertEquals("100–150", goalTargetLabel(min = 100.0, max = 150.0))
        assertEquals("150", goalTargetLabel(min = null, max = 150.0))
        assertEquals("≥100", goalTargetLabel(min = 100.0, max = null))
        assertNull(goalTargetLabel(min = null, max = null))
    }

    @Test
    fun fluidBarSegments_underTheGoal_leavesTheRestOpen() {
        val widths = fluidBarSegments(amountsMl = listOf(600.0, 400.0), goalMl = 2000.0)

        assertEquals(listOf(0.3f, 0.2f), widths.segments)
        assertEquals(0.5f, widths.open, 0.0001f)
    }

    @Test
    fun fluidBarSegments_atTheGoal_leavesNothingOpen() {
        val widths = fluidBarSegments(amountsMl = listOf(1000.0, 1000.0), goalMl = 2000.0)

        assertEquals(listOf(0.5f, 0.5f), widths.segments)
        assertEquals(0f, widths.open, 0.0001f)
    }

    @Test
    fun fluidBarSegments_overTheGoal_fillsTheBarAndKeepsTheRatio() {
        // 3000 ml against a 2000 ml goal: the bar is simply full, and the 2:1 split stays 2:1.
        val widths = fluidBarSegments(amountsMl = listOf(2000.0, 1000.0), goalMl = 2000.0)

        assertEquals(listOf(2f / 3f, 1f / 3f), widths.segments)
        assertEquals(0f, widths.open, 0.0001f)
    }

    @Test
    fun fluidBarSegments_withNothingDrunk_isAllOpen() {
        val widths = fluidBarSegments(amountsMl = emptyList(), goalMl = 2000.0)

        assertEquals(emptyList<Float>(), widths.segments)
        assertEquals(1f, widths.open, 0.0001f)
    }

    @Test
    fun fluidBarSegments_withoutAGoal_measuresAgainstWhatWasDrunk() {
        val widths = fluidBarSegments(amountsMl = listOf(500.0, 500.0), goalMl = 0.0)

        assertEquals(listOf(0.5f, 0.5f), widths.segments)
        assertEquals(0f, widths.open, 0.0001f)
    }

    @Test
    fun fluidBarSegments_withNeitherGoalNorDrinks_isAllOpen() {
        val widths = fluidBarSegments(amountsMl = listOf(0.0), goalMl = 0.0)

        assertEquals(listOf(0f), widths.segments)
        assertEquals(1f, widths.open, 0.0001f)
    }
}
