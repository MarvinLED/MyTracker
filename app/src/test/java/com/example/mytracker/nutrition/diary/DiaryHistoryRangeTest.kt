package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.metrics.ChartRange
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
    fun hidingTodayEndsTheWindowYesterdayWithoutShorteningIt() {
        val range = diaryHistoryRange(ChartRange.MONTH, firstLoggedDay = 1L, today = today, includeToday = false)

        // Still 30 days, slid back by one — dropping today must not cost the window a day.
        assertEquals(today - 30, range.startInclusive)
        assertEquals(today - 1, range.endInclusive)
    }

    @Test
    fun insgesamtWithoutTodayStillStartsAtTheFirstLoggedDay() {
        val range = diaryHistoryRange(ChartRange.ALL, firstLoggedDay = 19_000L, today = today, includeToday = false)

        assertEquals(19_000L, range.startInclusive)
        assertEquals(today - 1, range.endInclusive)
    }

    @Test
    fun aDiaryStartedTodayCollapsesOnceTodayIsHidden() {
        val range = diaryHistoryRange(ChartRange.ALL, firstLoggedDay = today, today = today, includeToday = false)

        assertEquals(today - 1, range.startInclusive)
        assertEquals(today - 1, range.endInclusive)
    }

    @Test
    fun aFirstLoggedDayInTheFutureNeverInvertsTheRange() {
        val range = diaryHistoryRange(ChartRange.ALL, firstLoggedDay = today + 5, today = today)

        assertEquals(today, range.startInclusive)
        assertEquals(today, range.endInclusive)
    }
}
