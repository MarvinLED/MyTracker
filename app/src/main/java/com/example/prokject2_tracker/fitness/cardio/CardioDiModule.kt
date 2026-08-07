package com.example.prokject2_tracker.fitness.cardio

import com.example.prokject2_tracker.core.backup.BackupExportProvider
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
    fun bindCardioSessionCountMetricSeriesProvider(impl: CardioSessionCountMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindCardioDistanceMetricSeriesProvider(impl: CardioDistanceMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindCardioPaceMetricSeriesProvider(impl: CardioPaceMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindCardioActivityTypeLibraryExportProvider(impl: CardioActivityTypeLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindCardioSessionExportProvider(impl: CardioSessionExportProvider): BackupExportProvider
}
