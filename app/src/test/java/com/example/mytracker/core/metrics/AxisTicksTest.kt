package com.example.mytracker.core.metrics

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AxisTicksTest {
    /** What a step on log paper is a multiple of. */
    private val RoundMantissas = listOf(1.0, 2.0, 5.0)

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
    fun equalRatiosAreEqualDistancesOnALogAxis() {
        val axis = logAxis(listOf(1.0, 1000.0))!!

        // The whole point of the scale: a tenth more is the same rise wherever it happens, which is
        // what lets Kalorien and Salz be compared at all.
        val lowDecade = axis.fractionOf(10.0) - axis.fractionOf(1.0)
        val highDecade = axis.fractionOf(1000.0) - axis.fractionOf(100.0)
        assertEquals(lowDecade, highDecade, 1e-6f)
    }

    @Test
    fun aLogAxisStepsInRoundNumbersInsideItsBounds() {
        // Salz in grams beside Kalorien: two and a half orders of magnitude in one chart.
        val axis = logAxis(listOf(6.0, 2500.0))!!

        assertTrue("bounds must hold the data", axis.min < 6.0 && axis.max > 2500.0)
        axis.values.forEach { value ->
            assertTrue("$value outside ${axis.min}..${axis.max}", value >= axis.min && value <= axis.max)
            val mantissa = value / 10.0.pow(floor(log10(value)))
            assertTrue("$value is not a round step", RoundMantissas.any { abs(it - mantissa) < 1e-9 })
        }
    }

    @Test
    fun aRangeTooNarrowForDecadesIsStillLabelled() {
        // Kalorien Soll and Ist alone move inside a single decade, where no power of ten times 1, 2
        // or 5 falls between them at all. An unlabelled axis would be the worse answer.
        val axis = logAxis(listOf(2000.0, 2500.0))!!

        assertTrue(axis.values.size >= 2)
        axis.values.forEach { assertTrue(it >= axis.min && it <= axis.max) }
    }

    @Test
    fun manyDecadesKeepAReadableNumberOfSteps() {
        val axis = logAxis(listOf(0.05, 3000.0))!!

        assertTrue("${axis.values.size} steps", axis.values.size in 2..9)
    }

    @Test
    fun nothingAboveZeroHasNoLogAxis() {
        // log(0) is not a number and no axis can pretend otherwise — the chart falls back to linear.
        assertNull(logAxis(listOf(0.0, -3.0)))
        assertNull(logAxis(emptyList()))
    }

    @Test
    fun zeroSitsOnTheFloorRatherThanNowhere() {
        val axis = logAxis(listOf(50.0, 500.0))!!

        // Pinned to the bottom edge: a hole in the line would read as "nothing logged", and the
        // crosshair still names the real value.
        assertEquals(0f, axis.fractionOf(0.0), 1e-6f)
        assertEquals(0f, axis.fractionOf(-10.0), 1e-6f)
        assertTrue(axis.fractionOf(500.0) > axis.fractionOf(50.0))
    }

    @Test
    fun extendingToFewerOrEqualStepsLeavesTheAxisAlone() {
        val ticks = niceAxisTicks(min = 0.0, max = 100.0)

        // Never narrowed: the axis that already has the most steps is the one the others match.
        assertEquals(ticks, ticks.extendedTo(ticks.values.size))
        assertEquals(ticks, ticks.extendedTo(1))
    }
}
