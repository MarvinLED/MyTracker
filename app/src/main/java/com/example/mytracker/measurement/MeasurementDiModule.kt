package com.example.prokject2_tracker.measurement

import com.example.prokject2_tracker.core.backup.BackupExportProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Two providers, one per [com.example.prokject2_tracker.core.backup.BackupScope]: the site
 * definitions are library data, the measurements taken against them are tracked data.
 */
@Module
@InstallIn(SingletonComponent::class)
interface MeasurementDiModule {
    @Binds
    @IntoSet
    fun bindBodySiteLibraryExportProvider(impl: BodySiteLibraryExportProvider): BackupExportProvider

    @Binds
    @IntoSet
    fun bindBodyMeasurementExportProvider(impl: BodyMeasurementExportProvider): BackupExportProvider
}
