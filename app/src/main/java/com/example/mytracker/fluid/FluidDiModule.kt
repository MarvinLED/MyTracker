package com.example.prokject2_tracker.fluid

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface FluidDiModule {
    @Binds
    @IntoSet
    fun bindMlFluidMetricSeriesProvider(impl: MlFluidMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindFluidTypeLibraryExportProvider(impl: FluidTypeLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindFluidUnitLibraryExportProvider(impl: FluidUnitLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindFluidQuickAddLibraryExportProvider(impl: FluidQuickAddLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindFluidEntriesExportProvider(impl: FluidEntriesExportProvider): BackupExportProvider
}
