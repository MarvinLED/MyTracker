package com.example.mytracker.core.di

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.metrics.MetricSeriesProvider
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
