package com.example.prokject2_tracker.fluid

import org.junit.Assert.assertEquals
import org.junit.Test

class FluidQuickAddTest {
    @Test
    fun rows_wrapAfterAFullRow() {
        assertEquals(emptyList<List<Int>>(), fluidQuickAddRows(emptyList<Int>()))
        assertEquals(listOf(listOf(1, 2, 3)), fluidQuickAddRows(listOf(1, 2, 3)))
        assertEquals(listOf(listOf(1, 2, 3, 4), listOf(5)), fluidQuickAddRows(listOf(1, 2, 3, 4, 5)))
    }

    @Test
    fun rows_neverGrowPastTwo() {
        val tooMany = (1..FluidQuickAddLimit + 5).toList()
        val rows = fluidQuickAddRows(tooMany)
        assertEquals(2, rows.size)
        assertEquals(FluidQuickAddLimit, rows.sumOf { it.size })
        assertEquals(FluidQuickAddsPerRow, rows.first().size)
    }

    @Test
    fun defaultAmounts_matchWhatTheSymbolsPromise() {
        assertEquals(250.0, FluidQuickAddSymbol.GLASS.defaultAmountMl(), 0.0001)
        assertEquals(500.0, FluidQuickAddSymbol.BOTTLE.defaultAmountMl(), 0.0001)
        assertEquals(100.0, FluidQuickAddSymbol.ML_100.defaultAmountMl(), 0.0001)
    }
}
