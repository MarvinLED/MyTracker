package com.example.mytracker.fluid

import com.example.mytracker.core.metrics.EpochDayRange
import com.example.mytracker.core.metrics.MetricAggregation
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.MetricSeriesDescriptor
import com.example.mytracker.core.metrics.MetricSeriesProvider
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
