package com.example.prokject2_tracker.sleep

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Sleep duration for the Analyse screen, in minutes — the unit it is stored and compared in, so a
 * bucketed week averages minutes rather than a rounded hour count. One night per day at most, so
 * [MetricAggregation.AVERAGE] only ever kicks in when several days are bucketed together.
 */
class SleepDurationMetricSeriesProvider @Inject constructor(
    private val sleepRepository: SleepRepository,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "sleep_duration_minutes",
        displayName = "Schlafdauer",
        unit = "min",
        category = "Schlaf",
        aggregation = MetricAggregation.AVERAGE,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        sleepRepository.observeDailyDurationMinutes(range.startInclusive, range.endInclusive)
}
