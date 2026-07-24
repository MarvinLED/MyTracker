package com.example.prokject2_tracker.habit

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HabitCompletionMetricSeriesProvider @Inject constructor(
    private val habitCheckInDao: HabitCheckInDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "habit.completed_count",
        displayName = "Habits erledigt",
        unit = "Stk.",
        category = "habit",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        habitCheckInDao.observeDailyCompletedCounts(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
