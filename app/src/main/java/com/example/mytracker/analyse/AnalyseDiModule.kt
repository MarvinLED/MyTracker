package com.example.mytracker.analyse

import com.example.mytracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface AnalyseDiModule {
    @Binds
    @IntoSet
    fun bindActiveDaysMetricSeriesProvider(impl: ActiveDaysMetricSeriesProvider): MetricSeriesProvider
}
