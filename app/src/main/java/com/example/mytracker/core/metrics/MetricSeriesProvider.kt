package com.example.mytracker.core.metrics

import kotlinx.coroutines.flow.Flow

/**
 * Inclusive range of epoch-days, used to scope a series query (e.g. for the Analyse date filter).
 */
data class EpochDayRange(val startInclusive: Long, val endInclusive: Long)

/**
 * Implemented by each feature module to expose one comparable data series to the Analyse screen,
 * without Analyse importing any feature-specific types (Hilt `@IntoSet` multibinding).
 */
interface MetricSeriesProvider {
    fun descriptor(): MetricSeriesDescriptor
    fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>>
}
