package com.example.prokject2_tracker.sleep

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Only the tags are library data; the nights themselves are tracked and get no exporter — the same
 * split as [com.example.prokject2_tracker.measurement.MeasurementDiModule] describes.
 */
@Module
@InstallIn(SingletonComponent::class)
interface SleepDiModule {
    @Binds
    @IntoSet
    fun bindSleepTagLibraryExportProvider(impl: SleepTagLibraryExportProvider): LibraryExportProvider

    @Binds
    @IntoSet
    fun bindSleepDurationMetricSeriesProvider(impl: SleepDurationMetricSeriesProvider): MetricSeriesProvider
}
