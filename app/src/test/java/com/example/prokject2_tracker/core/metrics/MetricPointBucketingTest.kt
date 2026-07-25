package com.example.prokject2_tracker.core.metrics

import com.example.prokject2_tracker.core.util.DateUtils
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricPointBucketingTest {
    /** Mon 2026-07-20 .. Sun 2026-07-26, so the week boundary is unambiguous. */
    private fun day(date: String): Long = LocalDate.parse(date).toEpochDay()

    private val week = listOf(
        MetricPoint(day("2026-07-20"), 100.0), // Monday
        MetricPoint(day("2026-07-22"), 200.0),
        MetricPoint(day("2026-07-26"), 300.0), // Sunday
        MetricPoint(day("2026-07-27"), 50.0), // next Monday
    )

    @Test
    fun daily_passesThroughUntouched() {
        assertEquals(week, week.bucketBy(Granularity.DAILY, MetricAggregation.SUM))
    }

    @Test
    fun weekly_bucketsOnMondayAndSums() {
        val result = week.bucketBy(Granularity.WEEKLY, MetricAggregation.SUM)
        assertEquals(2, result.size)
        assertEquals(day("2026-07-20"), result[0].epochDay)
        assertEquals(600.0, result[0].value, 0.0001)
        assertEquals(day("2026-07-27"), result[1].epochDay)
        assertEquals(50.0, result[1].value, 0.0001)
    }

    @Test
    fun weekly_averageDividesByThePointsPresent_notBySevenDays() {
        // Three logged days in the week: 600 / 3, not 600 / 7. Days without an entry are unknown,
        // not zero.
        val result = week.bucketBy(Granularity.WEEKLY, MetricAggregation.AVERAGE)
        assertEquals(200.0, result[0].value, 0.0001)
    }

    @Test
    fun last_takesTheNewestPointInTheBucket() {
        val result = week.bucketBy(Granularity.WEEKLY, MetricAggregation.LAST)
        assertEquals(300.0, result[0].value, 0.0001)
    }

    @Test
    fun monthly_bucketsOnTheFirst() {
        val across = listOf(
            MetricPoint(day("2026-06-30"), 10.0),
            MetricPoint(day("2026-07-01"), 20.0),
            MetricPoint(day("2026-07-31"), 30.0),
        )
        val result = across.bucketBy(Granularity.MONTHLY, MetricAggregation.SUM)
        assertEquals(listOf(day("2026-06-01"), day("2026-07-01")), result.map { it.epochDay })
        assertEquals(10.0, result[0].value, 0.0001)
        assertEquals(50.0, result[1].value, 0.0001)
    }

    @Test
    fun result_isAlwaysInChronologicalOrder() {
        val shuffled = week.reversed()
        val result = shuffled.bucketBy(Granularity.WEEKLY, MetricAggregation.SUM)
        assertEquals(result.sortedBy { it.epochDay }, result)
    }

    @Test
    fun empty_staysEmpty() {
        assertEquals(emptyList<MetricPoint>(), emptyList<MetricPoint>().bucketBy(Granularity.WEEKLY, MetricAggregation.SUM))
    }

    @Test
    fun startOfWeek_isMondayForEveryDayOfThatWeek() {
        val monday = day("2026-07-20")
        (0..6).forEach { offset ->
            assertEquals(monday, DateUtils.startOfWeekEpochDay(monday + offset))
        }
    }
}
