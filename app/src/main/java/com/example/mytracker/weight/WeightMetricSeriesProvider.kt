package com.example.prokject2_tracker.weight

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Always reports kg — the Analyse screen never does per-series unit conversion. Only [WeightScreen]
 * honors the user's [com.example.prokject2_tracker.core.datastore.WeightUnit] preference.
 */
class WeightMetricSeriesProvider @Inject constructor(
    private val bodyWeightDao: BodyWeightDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "weight.kg",
        displayName = "Gewicht",
        unit = "kg",
        category = "weight",
        aggregation = MetricAggregation.AVERAGE,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        bodyWeightDao.observeRange(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.weightKg) } }
}
