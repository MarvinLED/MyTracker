package com.example.prokject2_tracker.sleep

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Two providers, one per [com.example.prokject2_tracker.core.backup.BackupScope]: the tags are
 * library data, the nights they are stuck on are tracked data.
 */
@Module
@InstallIn(SingletonComponent::class)
interface SleepDiModule {
    @Binds
    @IntoSet
    fun bindSleepTagLibraryExportProvider(impl: SleepTagLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindSleepEntriesExportProvider(impl: SleepEntriesExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindSleepDurationMetricSeriesProvider(impl: SleepDurationMetricSeriesProvider): MetricSeriesProvider
}
