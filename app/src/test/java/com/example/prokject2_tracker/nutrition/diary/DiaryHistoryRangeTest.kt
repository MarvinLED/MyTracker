package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.metrics.ChartRange
import org.junit.Assert.assertEquals
import org.junit.Test

/** The window is anchored on today, unlike [ChartRange] elsewhere — see [diaryHistoryRange]. */
class DiaryHistoryRangeTest {
    private val today = 20_000L

    @Test
    fun monatIsThirtyDaysEndingToday() {
        val range = diaryHistoryRange(ChartRange.MONTH, firstLoggedDay = 1L, today = today)

        assertEquals(today - 29, range.startInclusive)
        assertEquals(today, range.endInclusive)
    }

    @Test
    fun jahrIsThreeHundredSixtyFiveDaysEndingToday() {
        val range = diaryHistoryRange(ChartRange.YEAR, firstLoggedDay = 1L, today = today)

        assertEquals(today - 364, range.startInclusive)
        assertEquals(today, range.endInclusive)
    }

    @Test
    fun insgesamtStartsAtTheFirstLoggedDay() {
        val range = diaryHistoryRange(ChartRange.ALL, firstLoggedDay = 19_000L, today = today)

        assertEquals(19_000L, range.startInclusive)
        assertEquals(today, range.endInclusive)
    }

    @Test
    fun insgesamtWithNothingLoggedCollapsesOntoToday() {
        // The chart then shows its own "nicht genug Datenpunkte" state rather than a bogus span.
        val range = diaryHistoryRange(ChartRange.ALL, firstLoggedDay = null, today = today)

        assertEquals(today, range.startInclusive)
        assertEquals(today, range.endInclusive)
    }

    @Test
    fun aFirstLoggedDayInTheFutureNeverInvertsTheRange() {
        val range = diaryHistoryRange(ChartRange.ALL, firstLoggedDay = today + 5, today = today)

        assertEquals(today, range.startInclusive)
        assertEquals(today, range.endInclusive)
    }
}
