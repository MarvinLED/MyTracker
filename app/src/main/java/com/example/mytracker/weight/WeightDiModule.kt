package com.example.mytracker.weight

import com.example.mytracker.core.backup.BackupExportProvider
import com.example.mytracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
interface WeightDiModule {
    @Binds
    @IntoSet
    fun bindWeightMetricSeriesProvider(impl: WeightMetricSeriesProvider): MetricSeriesProvider

    @Binds
    @IntoSet
    fun bindBodyWeightExportProvider(impl: BodyWeightExportProvider): BackupExportProvider
}
