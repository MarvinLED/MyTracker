package com.example.prokject2_tracker.fluid

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MlFluidMetricSeriesProvider @Inject constructor(
    private val fluidDao: FluidDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "fluid.ml_total",
        displayName = "Flüssigkeit",
        unit = "ml",
        category = "fluid",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        fluidDao.observeDailyMlTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
