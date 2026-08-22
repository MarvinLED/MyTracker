package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StrengthSetsMetricSeriesProvider @Inject constructor(
    private val strengthSetDao: StrengthSetDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "strength.sets_total",
        displayName = "Kraft-Sätze",
        unit = "Sätze",
        category = "strength",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        strengthSetDao.observeDailySetsTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class StrengthVolumeMetricSeriesProvider @Inject constructor(
    private val strengthSetDao: StrengthSetDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "strength.volume_kg",
        displayName = "Trainingsvolumen",
        unit = "kg",
        category = "strength",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        strengthSetDao.observeDailyVolumeTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class StrengthSessionCountMetricSeriesProvider @Inject constructor(
    private val strengthLogDao: StrengthLogDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "strength.sessions_count",
        displayName = "Anzahl Krafttrainings-Einheiten",
        unit = "Trainingstage",
        category = "strength",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        strengthLogDao.observeDailyActiveFlag(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
