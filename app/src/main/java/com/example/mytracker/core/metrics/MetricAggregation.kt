package com.example.mytracker.core.metrics

/** How daily [MetricPoint]s of a series are combined when bucketed into a coarser [Granularity]. */
enum class MetricAggregation { SUM, AVERAGE, LAST, MAX }
