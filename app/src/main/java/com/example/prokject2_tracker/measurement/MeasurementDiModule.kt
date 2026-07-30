package com.example.prokject2_tracker.measurement

import com.example.prokject2_tracker.core.backup.LibraryExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Only the site definitions are library data; the measurements are tracked/logged and get no
 * exporter, the same split as [com.example.prokject2_tracker.weight.WeightDiModule] describes.
 */
@Module
@InstallIn(SingletonComponent::class)
interface MeasurementDiModule {
    @Binds
    @IntoSet
    fun bindBodySiteLibraryExportProvider(impl: BodySiteLibraryExportProvider): LibraryExportProvider
}
