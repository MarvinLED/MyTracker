package com.example.prokject2_tracker.weight

import com.example.prokject2_tracker.core.metrics.MetricSeriesProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * No [com.example.prokject2_tracker.core.backup.LibraryExportProvider] binding here: body-weight
 * entries are tracked/logged data (like diary or fluid entries), never exported — only "library"
 * reference data (food items, fluid types/units, cardio activity types, habits) is.
 */
@Module
@InstallIn(SingletonComponent::class)
interface WeightDiModule {
    @Binds
    @IntoSet
    fun bindWeightMetricSeriesProvider(impl: WeightMetricSeriesProvider): MetricSeriesProvider
}
