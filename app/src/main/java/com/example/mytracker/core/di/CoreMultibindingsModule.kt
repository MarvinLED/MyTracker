package com.example.prokject2_tracker.core.di

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Declares the [BackupExportProvider]/[MetricSeriesProvider] sets so Hilt can satisfy an
 * injection of an empty set before any feature module contributes a concrete `@IntoSet` binding.
 */
@Module
@InstallIn(SingletonComponent::class)
interface CoreMultibindingsModule {
    @Multibinds
    fun bindBackupExportProviders(): Set<BackupExportProvider>

    @Multibinds
    fun bindMetricSeriesProviders(): Set<MetricSeriesProvider>
}
