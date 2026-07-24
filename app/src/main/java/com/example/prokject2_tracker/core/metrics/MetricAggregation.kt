package com.example.prokject2_tracker.core.metrics

/** How daily [MetricPoint]s of a series are combined when bucketed into a coarser [Granularity]. */
enum class MetricAggregation { SUM, AVERAGE, LAST }
