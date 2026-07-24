package com.example.prokject2_tracker.fitness.cardio

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface CardioDiModule {
    @Binds
    @IntoSet
    fun bindCardioDurationMetricSeriesProvider(impl: CardioDurationMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindCardioCaloriesMetricSeriesProvider(impl: CardioCaloriesMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindCardioHeartRateMetricSeriesProvider(impl: CardioHeartRateMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindCardioActivityTypeLibraryExportProvider(impl: CardioActivityTypeLibraryExportProvider): LibraryExportProvider
}
