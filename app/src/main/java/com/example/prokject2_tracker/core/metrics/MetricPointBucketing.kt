package com.example.prokject2_tracker.core.metrics

import com.example.prokject2_tracker.core.util.DateUtils

/** Combines daily [MetricPoint]s into weekly/monthly buckets per [aggregation]; a no-op for [Granularity.DAILY]. */
fun List<MetricPoint>.bucketBy(granularity: Granularity, aggregation: MetricAggregation): List<MetricPoint> {
    if (granularity == Granularity.DAILY) return this
    return groupBy { point ->
        when (granularity) {
            Granularity.WEEKLY -> DateUtils.startOfWeekEpochDay(point.epochDay)
            Granularity.MONTHLY -> DateUtils.startOfMonthEpochDay(point.epochDay)
            Granularity.DAILY -> point.epochDay
        }
    }
        .map { (bucketStart, points) ->
            val value = when (aggregation) {
                MetricAggregation.SUM -> points.sumOf { it.value }
                MetricAggregation.AVERAGE -> points.sumOf { it.value } / points.size
                MetricAggregation.LAST -> points.maxBy { it.epochDay }.value
            }
            MetricPoint(bucketStart, value)
        }
        .sortedBy { it.epochDay }
}
