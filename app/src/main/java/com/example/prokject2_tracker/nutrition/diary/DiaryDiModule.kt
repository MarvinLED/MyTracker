package com.example.prokject2_tracker.nutrition.diary

import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface DiaryDiModule {
    @Binds
    @IntoSet
    fun bindKcalDiaryMetricSeriesProvider(impl: KcalDiaryMetricSeriesProvider): MetricSeriesProvider
}
