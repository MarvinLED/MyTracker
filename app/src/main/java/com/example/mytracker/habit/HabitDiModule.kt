package com.example.mytracker.habit

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface HabitDiModule {
    @Binds
    @IntoSet
    fun bindHabitLibraryExportProvider(impl: HabitLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindHabitCheckInExportProvider(impl: HabitCheckInExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindHabitCompletionMetricSeriesProvider(impl: HabitCompletionMetricSeriesProvider): MetricSeriesProvider
}
