package com.example.prokject2_tracker.fitness.strength

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface StrengthDiModule {
    @Binds
    @IntoSet
    fun bindStrengthExerciseLibraryExportProvider(impl: StrengthExerciseLibraryExportProvider): LibraryExportProvider

    @Binds
    @IntoSet
    fun bindStrengthSetsMetricSeriesProvider(impl: StrengthSetsMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindStrengthVolumeMetricSeriesProvider(impl: StrengthVolumeMetricSeriesProvider): MetricSeriesProvider
}
