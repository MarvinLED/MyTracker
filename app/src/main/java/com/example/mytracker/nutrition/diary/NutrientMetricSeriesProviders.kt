package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.metrics.EpochDayRange
import com.example.prokject2_tracker.core.metrics.MetricAggregation
import com.example.prokject2_tracker.core.metrics.MetricPoint
import com.example.prokject2_tracker.core.metrics.MetricSeriesDescriptor
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProteinDiaryMetricSeriesProvider @Inject constructor(
    private val diaryDao: DiaryDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "diary.protein_total",
        displayName = "Protein",
        unit = "g",
        category = "nutrition",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        diaryDao.observeDailyProteinTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class CarbsDiaryMetricSeriesProvider @Inject constructor(
    private val diaryDao: DiaryDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "diary.carbs_total",
        displayName = "Kohlenhydrate",
        unit = "g",
        category = "nutrition",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        diaryDao.observeDailyCarbsTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}

class FatDiaryMetricSeriesProvider @Inject constructor(
    private val diaryDao: DiaryDao,
) : MetricSeriesProvider {
    override fun descriptor() = MetricSeriesDescriptor(
        id = "diary.fat_total",
        displayName = "Fett",
        unit = "g",
        category = "nutrition",
        aggregation = MetricAggregation.SUM,
    )

    override fun getSeries(range: EpochDayRange): Flow<List<MetricPoint>> =
        diaryDao.observeDailyFatTotals(range.startInclusive, range.endInclusive)
            .map { rows -> rows.map { MetricPoint(it.epochDay, it.value) } }
}
