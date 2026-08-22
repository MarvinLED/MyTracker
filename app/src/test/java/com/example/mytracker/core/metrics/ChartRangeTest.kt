package com.example.prokject2_tracker.core.metrics

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartRangeTest {
    @Test
    fun shortWindows_resolveToOnePointPerSession() {
        // Weekly points across a one-week axis would be a single dot.
        assertEquals(Granularity.DAILY, ChartRange.WEEK.granularityFor(spanDays = 7))
        assertEquals(Granularity.DAILY, ChartRange.MONTH.granularityFor(spanDays = 30))
    }

    @Test
    fun yearWindow_resolvesToWeeks() {
        assertEquals(Granularity.WEEKLY, ChartRange.YEAR.granularityFor(spanDays = 365))
    }

    @Test
    fun all_staysWeeklyUpToTwoYears_thenCoarsens() {
        assertEquals(Granularity.WEEKLY, ChartRange.ALL.granularityFor(spanDays = 0))
        assertEquals(Granularity.WEEKLY, ChartRange.ALL.granularityFor(spanDays = 730))
        assertEquals(Granularity.MONTHLY, ChartRange.ALL.granularityFor(spanDays = 731))
    }

    @Test
    fun all_hasNoFixedWindow() {
        assertEquals(null, ChartRange.ALL.days)
        assertEquals(7, ChartRange.WEEK.days)
        assertEquals(30, ChartRange.MONTH.days)
        assertEquals(365, ChartRange.YEAR.days)
    }

    @Test
    fun labels_areGerman() {
        assertEquals(listOf("Woche", "Monat", "Jahr", "Insgesamt"), ChartRange.entries.map { it.label() })
    }

    @Test
    fun pointLabels_sayWhatOnePointCovers() {
        assertEquals("pro Training", Granularity.DAILY.pointLabel())
        assertEquals("pro Woche", Granularity.WEEKLY.pointLabel())
        assertEquals("pro Monat", Granularity.MONTHLY.pointLabel())
    }
}
