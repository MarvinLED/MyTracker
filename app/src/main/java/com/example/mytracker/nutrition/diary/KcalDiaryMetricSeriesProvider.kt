package com.example.mytracker.nutrition.diary

import com.example.mytracker.core.metrics.EpochDayRange
import com.example.mytracker.core.metrics.MetricAggregation
import com.example.mytracker.core.metrics.MetricPoint
import com.example.mytracker.core.metrics.MetricSeriesDescriptor
import com.example.mytracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KcalDiaryMetricSeriesProvider @Inject constructor(
    private val diaryDao: DiaryDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "diary.kcal_total",
        displayName = "Kalorien",
        unit = "kcal",
        category = "nutrition",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        diaryDao.observeDailyKcalTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
