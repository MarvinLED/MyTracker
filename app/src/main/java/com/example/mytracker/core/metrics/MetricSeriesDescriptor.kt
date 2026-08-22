package com.example.mytracker.core.metrics

data class MetricSeriesDescriptor(
    val id: String,
    val displayName: String,
    val unit: String,
    val category: String,
    val aggregation: MetricAggregation,
)
