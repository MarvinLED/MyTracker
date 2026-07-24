package com.example.prokject2_tracker.fitness.cardio

import com.example.prokject2_tracker.core.metrics.EpochDayRange
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
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        cardioDao.observeDailyAvgHeartRateTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
