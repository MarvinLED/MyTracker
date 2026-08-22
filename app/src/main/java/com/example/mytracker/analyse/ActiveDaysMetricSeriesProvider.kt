package com.example.mytracker.analyse

import com.example.mytracker.core.metrics.EpochDayRange
import com.example.mytracker.core.metrics.MetricAggregation
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.MetricSeriesDescriptor
import com.example.mytracker.core.metrics.MetricSeriesProvider
import com.example.mytracker.fitness.cardio.CardioDao
import com.example.mytracker.fitness.strength.StrengthSetDao
import com.example.mytracker.habit.HabitCheckInDao
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ActiveDaysMetricSeriesProvider @Inject constructor(
    private val cardioDao: CardioDao,
    private val strengthSetDao: StrengthSetDao,
    private val habitCheckInDao: HabitCheckInDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "overall.active_day_ratio",
        displayName = "Aktive Tage",
        unit = "Anteil",
        category = "overall",
        aggregation = MetricAggregation.AVERAGE,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> = combine(
        cardioDao.observeDailyMinutesTotals(range.startInclusive, range.endInclusive),
        strengthSetDao.observeDailySetsTotals(range.startInclusive, range.endInclusive),
        habitCheckInDao.observeDailyCompletedCounts(range.startInclusive, range.endInclusive),
    ) { cardioRows, strengthRows, habitRows ->
        val activeDays = (cardioRows.map { it.epochDay } + strengthRows.map { it.epochDay } + habitRows.map { it.epochDay }).toSet()
        (range.startInclusive..range.endInclusive).map { day -> MetricPoint(day, if (day in activeDays) 1.0 else 0.0) }
    }
}
