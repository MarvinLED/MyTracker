package com.example.mytracker.fitness.strength

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.metrics.MetricSeriesProvider
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
    fun bindStrengthExerciseLibraryExportProvider(impl: StrengthExerciseLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindMuscleGroupLibraryExportProvider(impl: MuscleGroupLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindStrengthLogExportProvider(impl: StrengthLogExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindStrengthSetsMetricSeriesProvider(impl: StrengthSetsMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindStrengthVolumeMetricSeriesProvider(impl: StrengthVolumeMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindStrengthSessionCountMetricSeriesProvider(impl: StrengthSessionCountMetricSeriesProvider): MetricSeriesProvider
}
