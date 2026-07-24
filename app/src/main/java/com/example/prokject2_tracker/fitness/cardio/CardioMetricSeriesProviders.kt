package com.example.prokject2_tracker.fitness.cardio

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardioDurationMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "cardio.duration_minutes",
        displayName = "Cardio-Dauer",
        unit = "min",
        category = "cardio",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailyMinutesTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class CardioCaloriesMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "cardio.calories_burned",
        displayName = "Cardio-Kalorien",
        unit = "kcal",
        category = "cardio",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailyCaloriesBurnedTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class CardioHeartRateMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "cardio.avg_heart_rate",
        displayName = "Cardio-Herzfrequenz",
        unit = "bpm",
        category = "cardio",
        aggregation = MetricAggregation.AVERAGE,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailyAvgHeartRateTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class CardioSessionCountMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "cardio.sessions_count",
        displayName = "Anzahl Cardio-Einheiten",
        unit = "Einheiten",
        category = "cardio",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailySessionCountTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class CardioDistanceMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "cardio.distance_km",
        displayName = "Cardio-Distanz",
        unit = "km",
        category = "cardio",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailyDistanceKmTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class CardioPaceMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "cardio.avg_pace_min_per_km",
        displayName = "Ø Pace",
        unit = "min/km",
        category = "cardio",
        aggregation = MetricAggregation.AVERAGE,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailyAvgPace(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
