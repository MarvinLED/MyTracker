package com.example.mytracker.core.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinearFitTest {
    @Test
    fun aPerfectLineIsFoundExactly() {
        // y = 0,002x − 4: two units up per thousand, crossing zero at 2000.
        val points = listOf(1500.0, 2000.0, 2500.0, 3000.0).map { it to 0.002 * it - 4.0 }

        val fit = linearFit(points)!!

        assertEquals(0.002, fit.slope, 1e-9)
        assertEquals(-4.0, fit.intercept, 1e-9)
        assertEquals(1.0, fit.rSquared, 1e-9)
        assertEquals(2000.0, fit.xAtZero()!!, 1e-6)
    }

    @Test
    fun aCloudWithNoTrendExplainsNothing() {
        // A symmetric cloud: the same swing on both halves, so the best line is flat and says
        // nothing the mean does not.
        val points = listOf(1000.0 to 1.0, 2000.0 to -1.0, 3000.0 to -1.0, 4000.0 to 1.0)

        val fit = linearFit(points)!!

        assertEquals(0.0, fit.slope, 1e-9)
        assertEquals(0.0, fit.rSquared, 1e-9)
        // A flat line never crosses zero anywhere in particular, so no maintenance figure exists.
        assertNull(fit.xAtZero())
    }

    @Test
    fun noiseAroundATrendIsPartlyExplained() {
        val points = listOf(1800.0 to -0.4, 2000.0 to -0.1, 2200.0 to 0.0, 2400.0 to 0.5, 2600.0 to 0.3)

        val fit = linearFit(points)!!

        assertTrue("slope ${fit.slope}", fit.slope > 0.0)
        assertTrue("r² ${fit.rSquared}", fit.rSquared > 0.5 && fit.rSquared < 1.0)
    }

    @Test
    fun tooFewPointsAreNotATrend() {
        // A straight line runs through any two points; calling that a finding is the whole trap.
        assertNull(linearFit(listOf(2000.0 to 0.1, 2500.0 to 0.4)))
        assertNull(linearFit(emptyList()))
    }

    @Test
    fun withoutSpreadInXThereIsNoSlope() {
        // Every week the same intake: the cloud is vertical and has every slope at once.
        val points = listOf(2000.0 to -0.2, 2000.0 to 0.1, 2000.0 to 0.3)

        assertNull(linearFit(points))
    }

    @Test
    fun aFlatYIsExplainedPerfectly() {
        // Nothing to explain, so nothing is left unexplained — and no division by a variance of zero.
        val fit = linearFit(listOf(1000.0 to 2.0, 2000.0 to 2.0, 3000.0 to 2.0))!!

        assertEquals(0.0, fit.slope, 1e-9)
        assertEquals(1.0, fit.rSquared, 1e-9)
    }
}
