package com.example.mytracker.sleep

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Two providers, one per [com.example.mytracker.core.backup.BackupScope]: the tags are
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
