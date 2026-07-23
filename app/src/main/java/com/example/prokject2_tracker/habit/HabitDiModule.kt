package com.example.prokject2_tracker.habit

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
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
    fun bindHabitLibraryExportProvider(impl: HabitLibraryExportProvider): LibraryExportProvider

    @Binds
    @IntoSet
    fun bindHabitCompletionMetricSeriesProvider(impl: HabitCompletionMetricSeriesProvider): MetricSeriesProvider
}
