package com.example.mytracker.core.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AxisTicksTest {
    @Test
    fun stepsAreRoundNumbers() {
        val ticks = niceAxisTicks(min = 0.0, max = 2340.0)

        assertEquals(listOf(0.0, 500.0, 1000.0, 1500.0, 2000.0, 2500.0), ticks.values)
    }

    @Test
    fun theAxisNeverCutsOffAPoint() {
        val ticks = niceAxisTicks(min = 63.4, max = 81.2)

        assertTrue(ticks.min <= 63.4)
        assertTrue(ticks.max >= 81.2)
    }

    @Test
    fun smallValuesGetSmallSteps() {
        // Salz moves in single grams — an axis stepping by 1 g would have two useful lines on it.
        val ticks = niceAxisTicks(min = 0.0, max = 6.0)

        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0), ticks.values)
    }

    @Test
    fun fractionalStepsDoNotDrift() {
        val ticks = niceAxisTicks(min = 0.0, max = 0.5)

        assertEquals(listOf(0.0, 0.1, 0.2, 0.3, 0.4, 0.5), ticks.values.map { (it * 10).toLong() / 10.0 })
        ticks.values.forEachIndexed { index, value -> assertEquals(index * 0.1, value, 1e-9) }
    }

    @Test
    fun aFlatSeriesStillGetsAnAxis() {
        val ticks = niceAxisTicks(min = 2000.0, max = 2000.0)

        assertTrue(ticks.values.size >= 2)
        assertTrue(ticks.min <= 2000.0)
        assertTrue(ticks.max >= 2000.0)
    }

    @Test
    fun theStepCountStaysNearTheTarget() {
        listOf(1.0, 7.5, 23.0, 340.0, 2999.0, 18_000.0).forEach { max ->
            val count = niceAxisTicks(min = 0.0, max = max).values.size
            assertTrue("$max produced $count steps", count in 3..9)
        }
    }

    @Test
    fun extendingAddsHeadroomAtTheTopAndKeepsTheStep() {
        // Two axes beside one plot share its height, so their labels only line up with each other —
        // and with the grid — when both carry the same number of steps.
        val ticks = niceAxisTicks(min = 60.0, max = 80.0, targetSteps = 2)

        val extended = ticks.extendedTo(ticks.values.size + 2)

        assertEquals(ticks.values.size + 2, extended.values.size)
        assertEquals(ticks.min, extended.min, 0.0001)
        // Only the top moves, and it moves by whole steps: the axis never narrows and never picks up
        // an unround number.
        assertTrue(extended.max > ticks.max)
        val step = ticks.values[1] - ticks.values[0]
        assertEquals(ticks.max + 2 * step, extended.max, 0.0001)
        extended.values.zipWithNext().forEach { (low, high) -> assertEquals(step, high - low, 0.0001) }
    }

    @Test
    fun extendingToFewerOrEqualStepsLeavesTheAxisAlone() {
        val ticks = niceAxisTicks(min = 0.0, max = 100.0)

        // Never narrowed: the axis that already has the most steps is the one the others match.
        assertEquals(ticks, ticks.extendedTo(ticks.values.size))
        assertEquals(ticks, ticks.extendedTo(1))
    }
}
